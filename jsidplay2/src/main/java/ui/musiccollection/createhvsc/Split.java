package ui.musiccollection.createhvsc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is used by Ant to split the HVSC in several parts to avoid the
 * maximum file size limit of the web-site provider.
 * 
 * @author Ken Händel
 */
public class Split {

	/**
	 * @param args Arguments
	 */
	public static void main(String[] args) {
		new Split().doSplit(args);
	}

	private void doSplit(String[] args) {
		String filename = null;
		int totalBytes = -1;
		for (String paramValue : args) {
			int idx = paramValue.indexOf("=");
			if (idx != -1) {
				String name = paramValue.substring(0, idx);
				String value = paramValue.substring(idx + 1);
				if ("file".equals(name)) {
					filename = value;
				} else if ("bytes".equals(name)) {
					totalBytes = Integer.valueOf(value);
				}
			}
		}
		if (filename == null || !filename.endsWith(".zip") || totalBytes == -1) {
			System.err.println("Usage: file=<filename> bytes=<byteCount>");
			System.exit(1);
		}
		int partNum = 1;
		String output;
		output = createOutputFilename(filename, partNum);

		byte[] buffer = new byte[1 << 20];
		BufferedInputStream is = null;
		BufferedOutputStream os = null;
		try {
			int bytesRead = 0, totalBytesRead = 0;
			is = new BufferedInputStream(
					new FileInputStream(new File(filename)), 1 << 20);
			os = createOutputStream(output);
			int len = Math.min(buffer.length, totalBytes - totalBytesRead);
			while ((bytesRead = is.read(buffer, 0, len)) >= 0) {
				os.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				len = Math.min(buffer.length, totalBytes - totalBytesRead);
				if (totalBytesRead == totalBytes) {
					os.close();
					++partNum;
					output = createOutputFilename(filename, partNum);
					os = createOutputStream(output);
					totalBytesRead = 0;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
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

	private String createOutputFilename(String filename, int partNum) {
		return filename.substring(0, filename.lastIndexOf(".zip"))
				+ String.format(".%03d", partNum);
	}

	private BufferedOutputStream createOutputStream(String filename)
			throws FileNotFoundException {
		return new BufferedOutputStream(
				new FileOutputStream(new File(filename)), 1 << 20);
	}

}
