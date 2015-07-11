package ui.musiccollection;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javafx.scene.image.Image;
import libsidplay.sidtune.SidTune;

public class SidAuthors {

	private static Map<String, Image> author2image = new HashMap<String, Image>();

	/**
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();
	static {
		try (InputStream is = SidTune.class
				.getResourceAsStream("pictures.properties")) {
			SID_AUTHORS.load(is);
			for (Iterator<Object> authors = SID_AUTHORS.keySet().iterator(); authors
					.hasNext();) {
				String author = (String) authors.next();
				String photo = SID_AUTHORS.getProperty(author);
				photo = "Photos/" + photo;
				Image image = new Image(SidTune.class.getResource(photo).toString());
				author2image.put(author, image);
			}
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static final Image getImage(String author) {
		return author2image.get(author);
	}
}
