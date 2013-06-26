package ui.directory;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidplay.components.DirEntry;

public class DirectoryItem {
	private ObjectProperty<DirEntry> dirEntry = new SimpleObjectProperty<DirEntry>();
	private String text;

	public DirEntry getDirEntry() {
		return dirEntry.get();
	}

	public void setDirEntry(DirEntry value) {
		this.dirEntry.set(value);
	}

	public void setText(String text) {
		this.text = text;
	}
	
	public String getText() {
		return text;
	}
}
