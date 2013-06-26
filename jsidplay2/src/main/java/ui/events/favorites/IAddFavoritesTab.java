package ui.events.favorites;

import ui.events.IEvent;
import ui.favorites.FavoritesTab;

/**
 * Add a new tab in the favorites view.
 * 
 * @author Ken Händel
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
	public void setFavorites(FavoritesTab favorites);
	
}
