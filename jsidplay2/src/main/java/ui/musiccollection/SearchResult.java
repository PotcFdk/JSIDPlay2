package ui.musiccollection;

import java.io.File;
import java.util.function.Consumer;

public final class SearchResult {
	private String name;
	private Consumer<Boolean> searchStart;
	private Consumer<File> searchHit;

	public SearchResult(String name, Consumer<Boolean> searchStart,
			Consumer<File> searchHit) {
		this.name = name;
		this.searchStart = searchStart;
		this.searchHit = searchHit;
	}

	public Consumer<Boolean> getSearchStart() {
		return searchStart;
	}

	public Consumer<File> getSearchHit() {
		return searchHit;
	}

	@Override
	public String toString() {
		return name;
	}
}
