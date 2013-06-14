package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public class CollectionFileFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return file.getName().toLowerCase().endsWith(".zip");
	}

}
