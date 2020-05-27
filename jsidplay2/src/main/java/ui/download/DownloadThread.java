package ui.download;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import de.schlichtherle.truezip.file.TFile;
import libsidutils.InternetUtils;
import ui.entities.config.Configuration;

/**
 * DownloadManager downloads a large file from a server. If the file is splitted
 * into several chunks, it downloads them separately and merges them altogether
 * again. Each chunk is checked against its content length provided by HTTP. The
 * whole downloaded file is checked against the CRC checksum which is stored in
 * the file with file extension crc. <BR>
 * Download file consists of:
 *
 * <PRE>
 * &lt;chunk&gt;.001
 * &lt;chunk&gt;.002
 * ...
 * &lt;chunk&gt;.&lt;N&gt;
 * &lt;file&gt;.crc
 * </PRE>
 *
 * (where the chunks will be merged to &lt;file&gt;.&lt;ext&gt;) or
 *
 * <PRE>
 * &lt;file&gt;.&lt;ext&gt;
 * &lt;file&gt;.crc
 * </PRE>
 *
 * CRC file contents:
 *
 * <PRE>
 * filename=&lt;file&gt;.&lt;ext&gt;
 * size=&lt;fileSizeInBytes&gt;
 * crc32=&lt;8DigitsHexCRC32&gt;
 * </PRE>
 *
 * @author Ken Händel
 *
 */
public class DownloadThread extends Thread implements RBCWrapperDelegate {
	private static final String ILLEGAL_FILENAME_CHARS = "[?:]";
	private static final String REPLACEMENT_ILLEGAL_CHAR = "_";
	public static final int MAX_BUFFER_SIZE = 1 << 20;
	private static final int MAX_TRY_COUNT = 3;

	private final Configuration config;
	private final URL url;
	private final IDownloadListener listener;

	private boolean handleCrcAndSplits;

	public DownloadThread(Configuration cfg, IDownloadListener listener, URL url, boolean handleCrcAndSplits) {
		this.config = cfg;
		this.url = url;
		this.listener = listener;
		this.handleCrcAndSplits = handleCrcAndSplits;
	}

	@Override
	public void run() {
		// Part 1: Use already existing download
		if (handleCrcAndSplits) {
			try {
				File availableFile = createLocalFile(url);
				if (checkCrcOfAvailableFile(availableFile)) {
					listener.downloadStop(availableFile);
					return;
				} else {
					System.err.println("Online file contents has changed, re-download!");
				}
			} catch (IOException e) {
				// fall through!
			}
		}
		// Part 2: Download file(s)
		File downloadedFile = null;
		try {
			boolean isSplittedInChunks = handleCrcAndSplits && isSplittedInChunks();
			if (isSplittedInChunks) {
				downloadedFile = downloadAndMergeChunks();
			} else {
				downloadedFile = download(url, true, true);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			listener.downloadStop(null);
			return;
		}
		// Part 3: Optional CRC check
		try {
			if (handleCrcAndSplits) {
				if (checkCrcOfAvailableFile(downloadedFile)) {
					System.out.println("CRC32 check OK for download: " + url);
				} else {
					System.err.println("CRC32 check failed!");
					downloadedFile = null;
				}
			}
		} catch (IOException ioE) {
			// ignore missing CRC file
		} finally {
			listener.downloadStop(downloadedFile);
		}
	}

	private boolean checkCrcOfAvailableFile(File file) throws IOException {
		File crcFile = download(getCrcUrl(), false, false);
		return checkCrc(crcFile, file);
	}

	private boolean isSplittedInChunks() throws MalformedURLException {
		return checkExistingURL(getURL(1));
	}

	private boolean hasNextPart(int part) throws MalformedURLException {
		return checkExistingURL(getURL(part));
	}

	private URL getCrcUrl() throws MalformedURLException {
		return new URL(getURLUsingExt(".crc"));
	}

	private File downloadAndMergeChunks() throws IOException, MalformedURLException {
		File downloadedFile;
		List<File> chunks = new ArrayList<>();
		int part = 1;
		do {
			File chunk = download(getURL(part), true, true);
			chunk.deleteOnExit();
			chunks.add(chunk);
		} while (hasNextPart(++part));
		downloadedFile = mergeChunks(chunks);
		return downloadedFile;
	}

	private URL getURL(int part) throws MalformedURLException {
		return new URL(getURLUsingExt("." + String.format("%03d", part)));
	}

	private boolean checkExistingURL(URL currentURL) {
		try {
			Proxy proxy = config.getSidplay2Section().getProxy();
			URLConnection connection = InternetUtils.openConnection(currentURL, proxy);
			return connection.getContentLength() >= 0;
		} catch (IOException e) {
			return false;
		}
	}

	private File download(URL currentURL, boolean retry, boolean useAlreadyAvailableFile) throws IOException {
		String decoded = URLDecoder.decode(currentURL.toString(), UTF_8.name());
		int tries = 0;
		do {
			tries++;

			FileOutputStream fos = null;
			try {
				Proxy proxy = config.getSidplay2Section().getProxy();
				URLConnection connection = InternetUtils.openConnection(currentURL, proxy);
				currentURL = connection.getURL();
				long contentLength = connection.getContentLengthLong();
				File file = createLocalFile(currentURL);
				if (useAlreadyAvailableFile && isAlreadyAvailableFile(contentLength, file)) {
					// only un-zipped entries will be remembered here (because we clean-up zips)
					return file;
				}
				ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(connection.getInputStream()),
						contentLength, this);
				file = createLocalFile(currentURL);
				fos = new FileOutputStream(file);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

				if (isAlreadyAvailableFile(contentLength, file)) {
					return file;
				}
				file.delete();
			} catch (IOException e) {
				if (retry) {
					System.err.println(String.format("Download failed for %s, next try!", decoded));
				} else {
					throw e;
				}
			} finally {
				if (fos != null) {
					fos.close();
				}
			}
		} while (tries != MAX_TRY_COUNT);
		throw new IOException(String.format("Download error for %s, i have tried %d times! ", decoded, MAX_TRY_COUNT));
	}

	private boolean isAlreadyAvailableFile(long contentLength, File availableFile) {
		return availableFile.exists() && availableFile.isFile() && availableFile.canRead()
				&& availableFile.length() == contentLength;
	}

	private File createLocalFile(URL currentURL) throws IOException {
		String decoded = URLDecoder.decode(currentURL.getFile(), UTF_8.name());
		String name = new File(decoded).getName();
		return new File(config.getSidplay2Section().getTmpDir(),
				name.replaceAll(ILLEGAL_FILENAME_CHARS, REPLACEMENT_ILLEGAL_CHAR));
	}

	private File mergeChunks(List<File> chunks) throws IOException {
		File mergedFile = null;
		for (File chunk : chunks) {
			if (mergedFile == null) {
				mergedFile = chunk;
			} else {
				mergedFile = merge(mergedFile, chunk);
			}
		}
		File resultFile = createLocalFile(url);
		TFile.cp(mergedFile, resultFile);
		return resultFile;
	}

	@SuppressWarnings("resource")
	private File merge(File resultFile, File chunk) throws IOException {
		File tmp = File.createTempFile("jsidplay2", "tmp");
		tmp.deleteOnExit();
		try (InputStream is = new BufferedInputStream(
				new SequenceInputStream(new FileInputStream(resultFile), new FileInputStream(chunk)));
				OutputStream os = new BufferedOutputStream(new FileOutputStream(tmp))) {
			int bytesRead;
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
		}
		return tmp;
	}

	private boolean checkCrc(File crcFile, File download) throws IOException {
		Properties properties = new Properties();
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(crcFile))) {
			properties.load(stream);
		}
		try {
			String crc = properties.getProperty("crc32");
			long fileLength = Integer.valueOf(properties.getProperty("size"));
			String filename = properties.getProperty("filename");
			System.out.println("Check name: " + download.getName() + " with " + filename);
			System.out.println("Check size: " + download.length() + " with " + fileLength);
			System.out.println("Check  crc: " + calculateCRC32(download) + " with " + crc);
			return download.getName().equals(filename) && download.length() == fileLength
					&& calculateCRC32(download).equals(crc);
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static String calculateCRC32(File file) throws IOException {
		try (CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32())) {
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while (cis.read(buffer) >= 0) {
			}
			return String.format("%8X", cis.getChecksum().getValue()).replace(' ', '0');
		}
	}

	private String getURLUsingExt(String ext) {
		String path = url.toExternalForm();
		int extIdx = path.lastIndexOf('.');
		if (extIdx != -1) {
			return path.substring(0, extIdx) + ext;
		} else {
			throw new RuntimeException("filename must have a file extension");
		}
	}

	@Override
	public void rbcProgressCallback(double progress) {
		listener.downloadStep((int) progress);
	}

}
