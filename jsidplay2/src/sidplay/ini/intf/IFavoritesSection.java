package sidplay.ini.intf;

public interface IFavoritesSection {

	/**
	 * Getter of the Favorites browser titles.
	 * 
	 * @return the favorites browser titles
	 */
	public String getFavoritesTitles();

	/**
	 * Setter of the favorites browser titles.
	 * 
	 * @param titles
	 *            the favorites browser titles
	 */
	public void setFavoritesTitles(String titles);

	/**
	 * Getter of the favorites browser filenames.
	 * 
	 * @return the favorites browser filenames
	 */
	public String getFavoritesFilenames();

	/**
	 * Setter of the favorites browser filenames.
	 * 
	 * @param filenames
	 *            the favorites browser filenames
	 */
	public void setFavoritesFilenames(String filenames);

	/**
	 * Getter of the currently active favorites title.
	 * 
	 * @return the currently active favorites title
	 */
	public String getFavoritesCurrent();

	/**
	 * Setter of the currently active favorites title.
	 * 
	 * @param current
	 *            currently active favorites title
	 */
	public void setFavoritesCurrent(String current);

}