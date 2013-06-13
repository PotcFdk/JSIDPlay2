package ui.events.favorites;

import java.util.List;

import ui.events.IEvent;
import ui.favorites.FavoritesTab;


/**
 * Get all favorite tabs currently existing and the title of the currently
 * selected tab.
 * 
 * @author Ken Händel
 * 
 */
public interface IGetFavoritesTabs extends IEvent {
	/**
	 * @param tabs
	 *            all known favorite tabs
	 * @param selected
	 *            the title of the currently selected tab
	 */
	public void setFavoriteTabs(List<FavoritesTab> tabs, String selected);
}
