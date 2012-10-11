package sidplay.ini;

import sidplay.ini.intf.IFavoritesSection;



/**
 * Favorites section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniFavoritesSection extends IniSection implements IFavoritesSection {

	protected IniFavoritesSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the Favorites browser titles.
	 * 
	 * @return the favorites browser titles
	 */
	@Override
	public final String getFavoritesTitles() {
		return iniReader.getPropertyString("Favorites", "Titles", null);
	}

	/**
	 * Setter of the favorites browser titles.
	 * 
	 * @param titles
	 *            the favorites browser titles
	 */
	@Override
	public final void setFavoritesTitles(final String titles) {
		iniReader.setProperty("Favorites", "Titles", titles);
	}

	/**
	 * Getter of the favorites browser filenames.
	 * 
	 * @return the favorites browser filenames
	 */
	@Override
	public final String getFavoritesFilenames() {
		return iniReader.getPropertyString("Favorites", "Filenames", null);
	}

	/**
	 * Setter of the favorites browser filenames.
	 * 
	 * @param filenames
	 *            the favorites browser filenames
	 */
	@Override
	public final void setFavoritesFilenames(final String filenames) {
		iniReader.setProperty("Favorites", "Filenames", filenames);
	}

	/**
	 * Getter of the currently active favorites title.
	 * 
	 * @return the currently active favorites title
	 */
	@Override
	public final String getFavoritesCurrent() {
		return iniReader.getPropertyString("Favorites", "Current", null);
	}

	/**
	 * Setter of the currently active favorites title.
	 * 
	 * @param current
	 *            currently active favorites title
	 */
	@Override
	public final void setFavoritesCurrent(final String current) {
		iniReader.setProperty("Favorites", "Current", current);
	}
}