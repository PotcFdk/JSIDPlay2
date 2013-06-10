package ui.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class FavoritesFileFilter extends FileFilter {

	public static final String EXT_FAVORITES = ".js2";

	@Override
	public boolean accept(File f) {
		return f.isDirectory() || f.getName().endsWith(EXT_FAVORITES);
	}

	@Override
	public String getDescription() {
		return "Favorite Tunes";
	}

}
