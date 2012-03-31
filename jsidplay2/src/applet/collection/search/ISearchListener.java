package applet.collection.search;

import java.io.File;

/**
 * Interface to get noticed about the search state.
 * 
 * @author Ken Händel
 * 
 */
public interface ISearchListener {
	/**
	 * Search has been started
	 */
	void searchStart();

	/**
	 * Search has been stopped
	 * 
	 * @param canceled
	 *            operation was canceled by the user?
	 */
	void searchStop(boolean canceled);

	/**
	 * A search hit was detected
	 * 
	 * @param match
	 *            the matching file
	 */
	void searchHit(File match);
}
