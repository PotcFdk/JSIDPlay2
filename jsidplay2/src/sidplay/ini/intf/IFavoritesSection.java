package sidplay.ini.intf;

import java.util.List;

public interface IFavoritesSection {

	/**
	 * Getter of the Favorites browser titles.
	 * 
	 * @return the favorites browser titles
	 */
	String getName();

	/**
	 * Setter of the Favorites browser titles.
	 * 
	 * @param name
	 *            the favorites browser titles
	 */
	void setName(String name);

	/**
	 * Getter of the favorites browser filenames.
	 * 
	 * @return the favorites browser filenames
	 */
	String getFilename();

	/**
	 * Setter of the favorites browser filenames.
	 * 
	 * @param filenames
	 *            the favorites browser filenames
	 */
	void setFilename(String filename);

	List<String> getFavorites();

}