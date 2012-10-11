package sidplay.ini.intf;

public interface IFavoritesSection {

	/**
	 * Getter of the Favorites browser titles.
	 * 
	 * @return the favorites browser titles
	 */
	public String getName();

	/**
	 * Setter of the Favorites browser titles.
	 * 
	 * @param name
	 *            the favorites browser titles
	 */
	public void setName(String name);

	/**
	 * Getter of the favorites browser filenames.
	 * 
	 * @return the favorites browser filenames
	 */
	public String getFilename();

	/**
	 * Setter of the favorites browser filenames.
	 * 
	 * @param filenames
	 *            the favorites browser filenames
	 */
	public void setFilename(String filename);

}