package applet.favorites;

import java.io.File;

public interface IFavorites {
	String getFileName();
	void loadFavorites(String filename);
	void saveFavorites(String filename);
	void addToFavorites(File[] files);
	void selectFavorites();
	void deselectFavorites();
	void removeSelectedRows();
	File getNextFile(File filename);
	File getNextRandomFile(File filename);
	String[] getSelection();
	boolean isEmpty();
}
