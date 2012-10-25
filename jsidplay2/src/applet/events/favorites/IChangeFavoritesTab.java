package applet.events.favorites;

import applet.events.IEvent;

/**
 * Change the favorites properties
 * 
 * @author Ken H�ndel
 * 
 */
public interface IChangeFavoritesTab extends IEvent {

	/**
	 * @return the index of the favorites to change
	 */
	int getIndex();

	/**
	 * @return the new title of the favorites
	 */
	String getTitle();

	boolean isSelected();
}
