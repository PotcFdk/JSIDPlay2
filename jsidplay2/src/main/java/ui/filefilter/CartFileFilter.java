package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class CartFileFilter implements FileFilter {

	public static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".reu", ".crt" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return accept(null, file.getName().toLowerCase(Locale.ENGLISH));
	}

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
