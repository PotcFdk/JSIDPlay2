package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class TapeFileFilter implements FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".tap",
			".t64", ".prg", ".p00", ".zip", ".tap.gz", ".t64.gz", ".prg.gz",
			".p00.gz" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		String[] exts = defaultFileNameExt;
		for (String ext : exts) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}

}
