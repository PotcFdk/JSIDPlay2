package applet.favorites;

import java.io.File;
import java.io.IOException;

public interface IFavorites {
	void loadFavorites(String filename) throws IOException;

	void saveFavorites(String filename) throws IOException;

	void addToFavorites(File[] files);

	void selectFavorites();

	void deselectFavorites();

	void removeSelectedRows();

	File getNextFile(File filename);

	File getNextRandomFile(File filename);

	String[] getSelection();

	boolean isEmpty();

	FavoritesModel getFavoritesModel();
}
