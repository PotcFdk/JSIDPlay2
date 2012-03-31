package applet.collection.search;

import java.util.ArrayList;

/**
 * Common Search thread
 * 
 * @author Ken Händel
 * 
 */
public abstract class SearchThread extends Thread {

	/**
	 * Search direction (forward/backward)
	 */
	protected boolean fForward;

	/**
	 * User abort flag
	 */
	protected boolean fAborted;

	/**
	 * List of added search listeners
	 */
	protected ArrayList<ISearchListener> fListeners = new ArrayList<ISearchListener>();

	/**
	 * Create a new search thread
	 * 
	 * @param forward
	 *            search direction (forward/backward)
	 */
	public SearchThread(boolean forward) {
		fForward = forward;
		setPriority(MIN_PRIORITY);
	}

	/**
	 * Add a new search listener to be notified of the current search state
	 * 
	 * @param listener
	 *            the listener to add
	 */
	public void addSearchListener(ISearchListener listener) {
		fListeners.add(listener);
	}

	/**
	 * Set user abort flag
	 * 
	 * @param aborted
	 *            user abort flag
	 */
	public void setAborted(boolean aborted) {
		fAborted = aborted;
	}

	/**
	 * Get search direction
	 * @return true - forward, false backward
	 */
	public boolean getDirection() {
		return fForward;
	}
	
	/**
	 * Get current search state
	 * 
	 * @return the current search state
	 */
	public abstract Object getSearchState();

	/**
	 * Restore search state to continue search
	 * 
	 * @param state
	 *            the search state
	 */
	public abstract void restoreSearchState(Object state);
}
