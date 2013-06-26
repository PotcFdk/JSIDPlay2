package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public class DocsFileFilter implements FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".pdf" };

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
