package jsidplay2.dirs;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class SidDirs {

	private static Map<String, byte[]> dirs2imageData = new HashMap<String, byte[]>();

	/**
	 * Contains a mapping: Directory to picture resource path.
	 */
	private static final Properties SID_DIRS = new Properties();

	static {
		try (InputStream is = SidDirs.class.getResourceAsStream("photodirs.properties")) {
			SID_DIRS.load(is);
			for (Iterator<Object> dirs = SID_DIRS.keySet().iterator(); dirs.hasNext();) {
				String author = (String) dirs.next();
				String photoResource = SID_DIRS.getProperty(author);
				if (photoResource != null) {
					URL us = SidDirs.class.getResource(photoResource);
					if (us == null) {
						System.err.println(photoResource);
					}
					byte[] photo = new byte[us.openConnection().getContentLength()];
					try (DataInputStream is2 = new DataInputStream(SidDirs.class.getResourceAsStream(photoResource))) {
						is2.readFully(photo);
						dirs2imageData.put(author, photo);
					}
				}
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static final byte[] getDirectoryImageData(String hvscName) {
		return dirs2imageData.get(hvscName);
	}

}
