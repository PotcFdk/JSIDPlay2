package sidplay.ini;

import java.util.ArrayList;
import java.util.StringTokenizer;


/**
 * Favorites section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniFavoritesSection extends IniSection {

	protected IniFavoritesSection(IniReader iniReader) {
		super(iniReader);
	}

	private static String[] stringToList(final String str) {
		if (str == null) {
			return new String[0];
		}
		final ArrayList<String> result = new ArrayList<String>();
		final StringTokenizer tok = new StringTokenizer(str, ",", false);
		while (tok.hasMoreElements()) {
			final String name = (String) tok.nextElement();
			if ("null".equals(name)) {
				result.add(null);
			} else {
				result.add(name);
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private static String listToString(final String[] list) {
		if (list == null) {
			return "";
		}
		final StringBuffer result = new StringBuffer();
		for (int i = 0; i < list.length; i++) {
			if (i != 0) {
				result.append(",");
			}
			result.append(list[i]);
		}
		return result.toString();
	}

	/**
	 * Getter of the Favorites browser titles.
	 * 
	 * @return the favorites browser titles
	 */
	public final String[] getFavoritesTitles() {
		return stringToList(iniReader.getPropertyString("Favorites", "Titles", null));
	}

	/**
	 * Setter of the favorites browser titles.
	 * 
	 * @param titles
	 *            the favorites browser titles
	 */
	public final void setFavoritesTitles(final String[] titles) {
		iniReader.setProperty("Favorites", "Titles", listToString(titles));
	}

	/**
	 * Getter of the favorites browser filenames.
	 * 
	 * @return the favorites browser filenames
	 */
	public final String[] getFavoritesFilenames() {
		return stringToList(iniReader.getPropertyString("Favorites", "Filenames", null));
	}

	/**
	 * Setter of the favorites browser filenames.
	 * 
	 * @param filenames
	 *            the favorites browser filenames
	 */
	public final void setFavoritesFilenames(final String[] filenames) {
		iniReader.setProperty("Favorites", "Filenames", listToString(filenames));
	}

	/**
	 * Getter of the currently active favorites title.
	 * 
	 * @return the currently active favorites title
	 */
	public final String getFavoritesCurrent() {
		return iniReader.getPropertyString("Favorites", "Current", null);
	}

	/**
	 * Setter of the currently active favorites title.
	 * 
	 * @param current
	 *            currently active favorites title
	 */
	public final void setFavoritesCurrent(final String current) {
		iniReader.setProperty("Favorites", "Current", current);
	}
}