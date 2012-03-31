package applet.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class TapeFileFilter extends FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".tap",
			".t64", ".prg", ".p00", ".zip", ".tap.gz", ".t64.gz", ".prg.gz",
			".p00.gz" };

	@Override
	public String getDescription() {
		return "Tape Image (TAP, T64, PRG, P00, GZ or ZIP)";
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
