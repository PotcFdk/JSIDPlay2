package applet.events.favorites;

import applet.events.IEvent;

/**
 * Get all favorite tabs currently existing and the title of the currently
 * selected tab.
 * 
 * @author Ken Händel
 * 
 */
public interface IFavoriteTabNames extends IEvent {
	/**
	 * @param names the titles of all known favorite tabs
	 * @param selected the title of the currently selected tab
	 */
	public void setFavoriteTabNames(String[] names, String selected);
}
