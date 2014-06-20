/**
 * 
 */
package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public final class TuneFileFilter implements FileFilter {

	public static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".sid",
			".dat", ".c64", ".prg", ".p00", ".mus", ".str", ".mp3", ".zip" };

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