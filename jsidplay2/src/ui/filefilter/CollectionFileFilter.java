package ui.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class CollectionFileFilter extends FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return file.getName().toLowerCase().endsWith(".zip");
	}

	@Override
	public String getDescription() {
		return "MusicCollection (Directory or ZIP)";
	}

}
