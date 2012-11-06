package sidplay.ini.intf;

import java.util.List;

import applet.entities.collection.HVSCEntry;

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

	List<HVSCEntry> getFavorites();

}