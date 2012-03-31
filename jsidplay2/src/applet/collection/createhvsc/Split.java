package applet.collection.createhvsc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class is used by Ant to split the HVSC in several parts
 * to avoid the maximum file size limit of sourceforge.
 * 
 * @author Ken
 *
 */
public class Split {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new Split().doSplit(args);
	}

	private void doSplit(String[] args) {
		String filename = null;
		int bytes = -1;
		for (String paramValue : args) {
			int idx = paramValue.indexOf("=");
			if (idx != -1) {
				String name = paramValue.substring(0, idx);
				String value = paramValue.substring(idx + 1);
				if ("file".equals(name)) {
					filename = value;
				} else if ("bytes".equals(name)) {
					bytes = Integer.valueOf(value);
				}
			}
		}
		if (filename == null || !filename.endsWith(".zip") || bytes == -1) {
			System.err.println("Usage: file=<filename> bytes=<byteCount>");
			System.exit(1);
		}
		int partNum = 1;
		String output = filename.substring(0, filename.lastIndexOf(".zip"))
				+ String.format(".%03d", partNum);

		byte[] buffer = new byte[1 << 20];
		BufferedInputStream is = null;
		BufferedOutputStream os = null;
		try {
			int bytesRead = 0, totalBytesRead = 0;
			is = new BufferedInputStream(
					new FileInputStream(new File(filename)), 1 << 20);
			os = new BufferedOutputStream(
					new FileOutputStream(new File(output)), 1 << 20);
			int len = Math.min(buffer.length, bytes - totalBytesRead);
			while ((bytesRead = is.read(buffer, 0, len)) >= 0) {
				os.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				len = Math.min(buffer.length, bytes - totalBytesRead);
				if (totalBytesRead == bytes) {
					os.close();
					output = filename
							.substring(0, filename.lastIndexOf(".zip"))
							+ String.format(".%03d", ++partNum);
					os = new BufferedOutputStream(new FileOutputStream(
							new File(output)), 1 << 20);
					totalBytesRead = 0;
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
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

}
