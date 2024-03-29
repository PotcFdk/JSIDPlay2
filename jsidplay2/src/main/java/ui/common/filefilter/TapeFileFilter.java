package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class TapeFileFilter implements FileFilter {

	public static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".tap", ".t64", ".prg", ".p00", ".zip",
			".tap.gz", ".t64.gz", ".prg.gz", ".p00.gz" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return accept(null, file.getName().toLowerCase(Locale.US));
	}

	public boolean accept(File dir, String name) {
		String[] exts = DEFAULT_FILE_NAME_EXT;
		for (String ext : exts) {
			if (name.toLowerCase(Locale.US).endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

}
