package ui.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class DiskFileFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return file.getName().toLowerCase(Locale.ENGLISH).endsWith(".d64")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".g64")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".nib")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".d64.gz")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".g64.gz")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".nib.gz");
	}

}
