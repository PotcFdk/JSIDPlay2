package ui.musiccollection.search;

import java.io.File;
import java.io.FileFilter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

import ui.filefilter.TuneFileFilter;


public class SearchIndexerThread extends SearchThread {

	/**
	 * file filter for tunes
	 */
	protected FileFilter fFileFilter = new TuneFileFilter();
	protected Queue<File> fQueue = new LinkedList<File>();

	public SearchIndexerThread(final File root, Consumer<Void> searchStart,
			Consumer<File> searchHit, Consumer<Boolean> searchStop) {
		super(true, searchStart, searchHit, searchStop);
		fQueue.add(root);
	}

	@Override
	public void run() {
		searchStart.accept(null);
		while (!fAborted && !fQueue.isEmpty()) {
			final File tmp = fQueue.remove();
			searchHit.accept(tmp);
			File[] childs = tmp.listFiles(fFileFilter);
			if (childs != null) {
				for (final File child : childs) {
					fQueue.add(child);
				}
			}
		}
		searchStop.accept(fAborted);
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
