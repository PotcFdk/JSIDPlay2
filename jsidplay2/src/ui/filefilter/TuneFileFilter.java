/**
 * 
 */
package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public final class TuneFileFilter implements FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".sid",
			".dat", ".c64", ".prg", ".p00", ".mus", ".str", ".mp3", ".zip" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		String name = file.getName().toLowerCase();
		String[] exts = defaultFileNameExt;
		for (String ext : exts) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}
}