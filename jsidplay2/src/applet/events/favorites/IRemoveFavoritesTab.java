package applet.events.favorites;

import applet.events.IEvent;

/**
 * Remove the favorites
 * @author Ken H�ndel
 *
 */
public interface IRemoveFavoritesTab extends IEvent {

	/**
	 * @return the index of the favorites to remove
	 */
	int getIndex();

	/**
	 * @return the title of the favorites to remove
	 */
	String getTitle();

}
