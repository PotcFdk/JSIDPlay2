package ui.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.SequenceInputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketAddress;
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
	private static final String UTF_8 = "UTF-8";
	private static final String ILLEGAL_FILENAME_CHARS = "[?:]";
	private static final String REPLACEMENT_ILLEGAL_CHAR = "_";
	public static final int MAX_BUFFER_SIZE = 1 << 20;
	private static final int MAX_TRY_COUNT = 3;

	private final Configuration config;
	private final URL url;
	private final IDownloadListener listener;

	private Proxy proxy;

	private boolean handleCrcAndSplits;

	public DownloadThread(Configuration cfg, IDownloadListener listener, URL url) {
		this(cfg, listener, url, true);
	}

	public DownloadThread(Configuration cfg, IDownloadListener listener, URL url, boolean handleCrcAndSplits) {
		this.config = cfg;
		this.url = url;
		this.listener = listener;
		this.proxy = getProxy();
		this.handleCrcAndSplits = handleCrcAndSplits;
	}

	@Override
	public void run() {
		// Part 1: Use already existing download
		try {
			File availableFile = createLocalFile(url);
			if (alreadyAvailable(availableFile)) {
				listener.downloadStop(availableFile);
				return;
			}
		} catch (IOException e) {
			// fall through!
		}
		// Part 2: Download file(s)
		File downloadedFile = null;
		try {
			boolean isSplittedInChunks = handleCrcAndSplits && isSplittedInChunks();
			if (isSplittedInChunks) {
				downloadedFile = downloadAndMergeChunks();
			} else {
				downloadedFile = download(url, true);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			listener.downloadStop(null);
			return;
		}
		// Part 3: Optional CRC check
		try {
			if (handleCrcAndSplits) {
				File crcFile = download(getCrcUrl(), false);
				boolean checkCrc = checkCrc(crcFile, downloadedFile);
				if (!checkCrc) {
					System.err.println("CRC32 check failed!");
					downloadedFile = null;
				} else {
					System.out.println("CRC32 check OK for download: " + url);
				}
			}
		} catch (IOException ioE) {
			// ignore missing CRC file
		} finally {
			listener.downloadStop(downloadedFile);
		}
	}

	private boolean alreadyAvailable(File file) throws IOException {
		if (!handleCrcAndSplits)
			return file.exists() && file.canRead() && file.length() > 0;
		File crcFile = download(getCrcUrl(), false);
		boolean checkCrc = checkCrc(crcFile, file);
		if (!checkCrc) {
			System.err.println("Online file contents has changed, re-download!");
			return false;
		}
		return true;
	}

	private Proxy getProxy() {
		if (config.getSidplay2Section().isEnableProxy()) {
			final SocketAddress addr = new InetSocketAddress(config.getSidplay2Section().getProxyHostname(),
					config.getSidplay2Section().getProxyPort());
			return new Proxy(Proxy.Type.HTTP, addr);
		} else {
			return Proxy.NO_PROXY;
		}
	}

	private boolean isSplittedInChunks() throws IOException {
		return checkExistingURL(getURL(1));
	}

	private File downloadAndMergeChunks() throws IOException, MalformedURLException {
		File downloadedFile;
		List<File> chunks = new ArrayList<File>();
		int part = 1;
		do {
			File chunk = download(getURL(part), true);
			chunk.deleteOnExit();
			chunks.add(chunk);
		} while (hasNextPart(++part));
		downloadedFile = mergeChunks(chunks);
		return downloadedFile;
	}

	private URL getURL(int part) throws MalformedURLException {
		return new URL(getURLUsingExt("." + String.format("%03d", part)));
	}

	private boolean checkExistingURL(URL currentURL) throws IOException {
		HttpURLConnection connection = getConnection(currentURL);
		return connection.getResponseCode() == HttpURLConnection.HTTP_OK && connection.getContentLength() >= 0;
	}

	private HttpURLConnection getConnection(URL currentURL) throws IOException, ProtocolException {
		HttpURLConnection.setFollowRedirects(true);
		URLConnection openConnection = currentURL.openConnection(proxy);
		if (!(openConnection instanceof HttpURLConnection)) {
			throw new IOException("Unsupported url: " + currentURL);
		}
		HttpURLConnection connection = (HttpURLConnection) openConnection;
		connection.setRequestMethod("HEAD");
		return connection;
	}

	private File download(URL currentURL, boolean retry) throws IOException {
		String decoded;
		try {
			decoded = URLDecoder.decode(currentURL.toString(), UTF_8);
		} catch (UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
		int tries = 0;
		do {
			tries++;

			FileOutputStream fos = null;
			try {
				while (true) {
					URLConnection openConnection = currentURL.openConnection(proxy);
					if (!(openConnection instanceof HttpURLConnection)) {
						System.err.println("Unsupported protocol: " + currentURL);
						throw new IOException();
					}
					HttpURLConnection connection = (HttpURLConnection) openConnection;
					connection.setInstanceFollowRedirects(false);
					int responseCode = connection.getResponseCode();
					switch (responseCode) {
					case HttpURLConnection.HTTP_MOVED_PERM:
					case HttpURLConnection.HTTP_MOVED_TEMP:
					case HttpURLConnection.HTTP_SEE_OTHER:
						String location = connection.getHeaderField("Location");
						if (location != null) {
							location = URLDecoder.decode(location, "UTF-8");
							// Deal with relative URLs
							URL next = new URL(currentURL, location);
							currentURL = new URL(next.toExternalForm().replace(" ", "%20"));
							continue;
						}
					}
					long contentLength = connection.getContentLengthLong();
					ReadableByteChannel rbc = new RBCWrapper(Channels.newChannel(connection.getInputStream()),
							contentLength, this);
					File file = createLocalFile(currentURL);
					fos = new FileOutputStream(file);
					fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

					if (file.length() == contentLength) {
						return file;
					}
					file.delete();
					break;
				}
			} catch (IOException e) {
				if (retry) {
					System.err.println(String.format("Download failed for %s, next try!", decoded));
				} else {
					throw e;
				}
			} finally {
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} while (tries != MAX_TRY_COUNT);
		throw new IOException(String.format("Download error for %s, i have tried %d times! ", decoded, MAX_TRY_COUNT));
	}

	private boolean hasNextPart(int part) throws IOException {
		return checkExistingURL(getURL(part));
	}

	private File createLocalFile(URL currentURL) {
		try {
			String decoded = URLDecoder.decode(currentURL.getFile(), UTF_8);
			String name = new File(decoded).getName();
			return new File(config.getSidplay2Section().getTmpDir(),
					name.replaceAll(ILLEGAL_FILENAME_CHARS, REPLACEMENT_ILLEGAL_CHAR));
		} catch (UnsupportedEncodingException e1) {
			throw new RuntimeException(e1);
		}
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

	private URL getCrcUrl() throws MalformedURLException {
		return new URL(getURLUsingExt(".crc"));
	}

	private boolean checkCrc(File crcFile, File download) throws IOException {
		Properties properties = new Properties();
		try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(crcFile))) {
			properties.load(stream);
		}
		try {
			long crc = Long.valueOf(properties.getProperty("crc32"), 16);
			long fileLength = Integer.valueOf(properties.getProperty("size"));
			String filename = properties.getProperty("filename");
			if (!download.getName().equals(filename)) {
				return false;
			}
			if (download.length() != fileLength) {
				return false;
			}
			return calculateCRC32(download) == crc;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static long calculateCRC32(File file) throws IOException {
		try (CheckedInputStream cis = new CheckedInputStream(new FileInputStream(file), new CRC32())) {
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while (cis.read(buffer) >= 0) {
			}
			return cis.getChecksum().getValue();
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
	public void rbcProgressCallback(RBCWrapper rbc, double progress) {
		listener.downloadStep((int) progress);
	}

	/**
	 * Calculate CRC32 checksum for ant build script. Output goes to stdout.
	 * 
	 * @param args
	 *            filename to create a checksum for
	 */
	public static void main(String[] args) {
		try {
			long checksum = calculateCRC32(new File(args[0]));
			System.out.println(String.format("%8X", checksum).replace(' ', '0'));
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
}
