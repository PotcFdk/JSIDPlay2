package applet.events.favorites;

import applet.events.IEvent;
import applet.favorites.IFavorites;

/**
 * Add a new tab in the favorites view.
 * 
 * @author Ken H�ndel
 * 
 */
public interface IAddFavoritesTab extends IEvent {

	/**
	 * @return the title of the new tab
	 */
	String getTitle();

	/**
	 * @param favorites the newly added favorites
	 */
	public void setFavorites(IFavorites favorites);
	
}
