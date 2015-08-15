package ui.musiccollection.search;

import java.io.File;
import java.util.function.Consumer;

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

	protected Consumer<Void> searchStart;
	protected Consumer<File> searchHit;
	protected Consumer<Boolean> searchStop;

	/**
	 * Create a new search thread
	 * 
	 * @param forward
	 *            search direction (forward/backward)
	 */
	public SearchThread(boolean forward, Consumer<Void> searchStart,
			Consumer<File> searchHit, Consumer<Boolean> searchStop) {
		fForward = forward;
		this.searchStart = searchStart;
		this.searchHit = searchHit;
		this.searchStop = searchStop;
		setPriority(MIN_PRIORITY);
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
	 * 
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
	public abstract void setSearchState(Object state);
}
