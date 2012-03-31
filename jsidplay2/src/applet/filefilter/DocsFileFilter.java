package applet.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class DocsFileFilter extends FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".pdf" };

	@Override
	public String getDescription() {
		return "Documents (PDF)";
	}

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
