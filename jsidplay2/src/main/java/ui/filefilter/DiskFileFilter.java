package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Locale;

public class DiskFileFilter implements FileFilter, FilenameFilter {

	public static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".d64",
			".g64", ".nib", ".zip", ".d64.gz", ".g64.gz", ".nib.gz" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return accept(null, file.getName().toLowerCase(Locale.US));
	}

	@Override
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
