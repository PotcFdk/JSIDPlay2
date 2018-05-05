package ui.musiccollection;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import libsidplay.sidtune.SidTune;

public class SidAuthors {

	private static Map<String, byte[]> author2imageData = new HashMap<String, byte[]>();

	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();

	static {
		try (InputStream is = SidTune.class.getResourceAsStream("pictures.properties")) {
			SID_AUTHORS.load(is);
			for (Iterator<Object> authors = SID_AUTHORS.keySet().iterator(); authors.hasNext();) {
				String author = (String) authors.next();
				String photoResource = SID_AUTHORS.getProperty(author);
				if (photoResource != null) {
					URL us = SidTune.class.getResource("Photos/" + photoResource);
					byte[] photo = new byte[us.openConnection().getContentLength()];
					try (DataInputStream is2 = new DataInputStream(
							SidTune.class.getResourceAsStream("Photos/" + photoResource))) {
						is2.readFully(photo);
						author2imageData.put(author, photo);
					}
				}
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static final byte[] getImageData(String author) {
		return author2imageData.get(author);
	}
}
