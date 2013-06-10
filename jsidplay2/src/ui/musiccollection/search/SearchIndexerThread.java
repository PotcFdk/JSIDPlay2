package ui.musiccollection.search;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.Queue;

import ui.filefilter.TuneFileFilter;


public class SearchIndexerThread extends SearchThread {

	/**
	 * file filter for tunes
	 */
	protected FileFilter fFileFilter = new TuneFileFilter();
	protected Queue<File> fQueue = new LinkedList<File>();

	public SearchIndexerThread(final File root) {
		super(true);
		fQueue.add(root);
	}

	@Override
	public void run() {
		for (final ISearchListener listener : fListeners) {
			listener.searchStart();
		}
		while (!fAborted && !fQueue.isEmpty()) {
			final File tmp = fQueue.remove();
			for (final ISearchListener listener : fListeners) {
				listener.searchHit(tmp);
			}
			File[] childs = tmp.listFiles(fFileFilter);
			if (childs != null) {
				for (final File child : childs) {
					fQueue.add(child);
				}
			}
		}
		for (final ISearchListener listener : fListeners) {
			listener.searchStop(fAborted);
		}
	}

	@Override
	public Object getSearchState() {
		return fQueue;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void setSearchState(final Object state) {
		if (state != null && (state instanceof Queue<?>)) {
			fQueue = (Queue<File>) state;
		}

	}
}
