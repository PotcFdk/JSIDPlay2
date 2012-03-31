package applet.events.favorites;

import applet.events.IEvent;
import applet.favorites.IFavorites;

/**
 * Get all favorites of a tab referenced by the tab name
 * 
 * @author Ken Händel
 * 
 */
public interface IGetFavorites extends IEvent {
	/**
	 * @return the name of the tab
	 */
	public int getIndex();

	/**
	 * Callback with the requested favorites referenced by the tab name
	 * @param favorites the favorites of the tab referenced by the title
	 */
	public void setFavorites(IFavorites favorites);
}
