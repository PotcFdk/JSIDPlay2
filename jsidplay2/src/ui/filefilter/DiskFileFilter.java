package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public class DiskFileFilter implements FileFilter {

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		return file.getName().toLowerCase().endsWith(".d64")
				|| file.getName().toLowerCase().endsWith(".g64")
				|| file.getName().toLowerCase().endsWith(".nib")
				|| file.getName().toLowerCase().endsWith(".zip")
				|| file.getName().toLowerCase().endsWith(".d64.gz")
				|| file.getName().toLowerCase().endsWith(".g64.gz")
				|| file.getName().toLowerCase().endsWith(".nib.gz");
	}

}
