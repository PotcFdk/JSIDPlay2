package applet.download;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;

import sidplay.ini.intf.IConfig;
import applet.PathUtils;

/**
 * DownloadManager downloads a large file from a server. If the file is splitted
 * into several chunks, it downloads them separately and merges them altogether
 * again. Each chunk is checked against its content length provided by HTTP. The
 * whole downloaded file is checked against the CRC checksum which is stored
 * stored in the file with file extension crc.
 * 
 * <PRE>
 * &lt;chunk&gt;.001
 * &lt;chunk&gt;.002
 * ...
 * &lt;chunk&gt;.&lt;N&gt;
 * &lt;chunk&gt;.crc
 * </PRE>
 * 
 * or
 * 
 * <PRE>
 * &lt;file&gt;.&lt;ext&gt;
 * &lt;file&gt;.crc
 * </PRE>
 * 
 * <PRE>
 * filename=&lt;file&gt;.&lt;ext&gt;
 * size=130944022
 * crc32=BAE9A635
 * </PRE>
 * 
 * @author Ken
 * 
 */
public class DownloadThread extends Thread implements RBCWrapperDelegate {
	public static final int MAX_BUFFER_SIZE = 1 << 20;
	private static final int MAX_TRY_COUNT = 3;

	private final IConfig config;
	private final URL url;
	private final IDownloadListener listener;

	private Proxy proxy;

	public DownloadThread(IConfig cfg, IDownloadListener listener, URL url) {
		this.config = cfg;
		this.url = url;
		this.listener = listener;
		this.proxy = getProxy();
	}

	@Override
	public void run() {
		File availableFile = createLocalFile(url);
		try {
			if (alreadyAvailable(availableFile)) {
				listener.downloadStop(availableFile);
				return;
			}
		} catch (IOException e1) {
			// on error: download again!
		}
		try {
			File downloadedFile;

			boolean isSplittedInChunks = isSplittedInChunks();
			if (isSplittedInChunks) {
				downloadedFile = downloadAndMergeChunks();
			} else {
				downloadedFile = download(url, true);
			}
			try {
				File crcFile = download(getCrcUrl(), false);
				boolean checkCrc = checkCrc(crcFile, downloadedFile);
				if (!checkCrc) {
					System.err.println("CRC32 check failed!");
					listener.downloadStop(null);
				} else {
					System.out.println("CRC32 check OK for download: " + url);
					listener.downloadStop(downloadedFile);
				}
			} catch (IOException e) {
				listener.downloadStop(downloadedFile);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			listener.downloadStop(null);
		} catch (IOException e) {
			e.printStackTrace();
			listener.downloadStop(null);
		}
	}

	private boolean alreadyAvailable(File file) throws IOException {
		File crcFile = download(getCrcUrl(), false);
		boolean checkCrc = checkCrc(crcFile, file);
		if (!checkCrc) {
			System.err.println("CRC32 check failed, re-download!");
			return false;
		}
		return true;
	}

	private Proxy getProxy() {
		if (config.getSidplay2().isEnableProxy()) {
			final SocketAddress addr = new InetSocketAddress(config
					.getSidplay2().getProxyHostname(), config.getSidplay2()
					.getProxyPort());
			return new Proxy(Proxy.Type.HTTP, addr);
		} else {
			return Proxy.NO_PROXY;
		}
	}

	private boolean isSplittedInChunks() throws IOException {
		return checkExistingURL(getURL(1));
	}

	private File downloadAndMergeChunks() throws IOException,
			MalformedURLException {
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
		return new URL(getURLUsingExt(url.toExternalForm(),
				"." + String.format("%03d", part)));
	}

	private boolean checkExistingURL(URL currentURL) throws IOException {
		HttpURLConnection connection = getConnection(currentURL);
		return connection.getResponseCode() == HttpURLConnection.HTTP_OK
				&& connection.getContentLength() >= 0;
	}

	private HttpURLConnection getConnection(URL currentURL) throws IOException,
			ProtocolException {
		HttpURLConnection.setFollowRedirects(false);
		HttpURLConnection connection;
		connection = (HttpURLConnection) currentURL.openConnection(proxy);
		connection.setRequestMethod("HEAD");
		return connection;
	}

	private File download(URL currentURL, boolean retry) throws IOException {
		int tries = 0;
		do {
			tries++;
			File file = createLocalFile(currentURL);

			FileOutputStream fos = null;
			try {
				long contentLength = getConnection(currentURL)
						.getContentLength();

				final URLConnection connection = currentURL
						.openConnection(proxy);
				ReadableByteChannel rbc = new RBCWrapper(
						Channels.newChannel(connection.getInputStream()),
						contentLength, this);
				fos = new FileOutputStream(file);
				fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

				if (file.length() == contentLength) {
					return file;
				}
			} catch (IOException e) {
				if (retry) {
					System.err.println(String.format(
							"Download failed for %s, next try!",
							currentURL.getPath()));
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
		throw new IOException(String.format(
				"Download error, i have tried %d times! ", MAX_TRY_COUNT));
	}

	private boolean hasNextPart(int part) throws IOException {
		return checkExistingURL(getURL(part));
	}

	private File createLocalFile(URL currentURL) {
		return new File(System.getProperty("jsidplay2.tmpdir"), new File(
				currentURL.getPath()).getName());
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
		PathUtils.copyFile(mergedFile, resultFile);
		return resultFile;
	}

	@SuppressWarnings("resource")
	private File merge(File resultFile, File chunk) throws IOException {
		BufferedInputStream is = null;
		BufferedOutputStream os = null;
		File tmp = null;
		try {
			tmp = File.createTempFile("jsidplay2", "tmp");
			tmp.deleteOnExit();
			is = new BufferedInputStream(
					new SequenceInputStream(new FileInputStream(resultFile),
							new FileInputStream(chunk)));
			os = new BufferedOutputStream(new FileOutputStream(tmp));
			int bytesRead;
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while ((bytesRead = is.read(buffer)) != -1) {
				os.write(buffer, 0, bytesRead);
			}
			return tmp;
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private URL getCrcUrl() throws MalformedURLException {
		return new URL(getURLUsingExt(url.toExternalForm(), ".crc"));
	}

	private boolean checkCrc(File crcFile, File download) throws IOException {
		Properties properties = new Properties();
		BufferedInputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(crcFile));
			properties.load(stream);
		} finally {
			if (stream != null) {
				stream.close();
			}
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

	private long calculateCRC32(File file) throws IOException {
		CheckedInputStream cis = null;
		try {
			FileInputStream fis = new FileInputStream(file);
			cis = new CheckedInputStream(fis, new CRC32());
			byte[] buffer = new byte[MAX_BUFFER_SIZE];
			while (cis.read(buffer) >= 0) {
			}
			return cis.getChecksum().getValue();
		} finally {
			if (cis != null) {
				try {
					cis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String getURLUsingExt(String path, String ext) {
		int extIdx = path.lastIndexOf('.');
		if (extIdx != -1) {
			return path.substring(0, extIdx) + ext;
		} else {
			throw new RuntimeException("filename must have a file extension");
		}
	}

	@Override
	public void rbcProgressCallback(RBCWrapper rbc, double progress) {
		listener.downloadStep((int) (progress));
	}

}
