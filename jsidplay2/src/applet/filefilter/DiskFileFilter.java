package applet.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class DiskFileFilter extends FileFilter implements java.io.FileFilter {

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

	@Override
	public String getDescription() {
		return "Disk Image (D64, G64, NIB, GZ or ZIP)";
	}

}
