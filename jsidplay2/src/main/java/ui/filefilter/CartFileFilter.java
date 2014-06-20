package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Locale;

public class CartFileFilter implements FileFilter, FilenameFilter {

	public static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".crt" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return accept(null, file.getName().toLowerCase(Locale.ENGLISH));
	}

	@Override
	public boolean accept(File dir, String name) {
		String[] exts = DEFAULT_FILE_NAME_EXT;
		for (String ext : exts) {
			if (name.toLowerCase(Locale.ENGLISH).endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

}
