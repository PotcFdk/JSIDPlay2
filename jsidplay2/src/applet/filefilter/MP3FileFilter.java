package applet.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class MP3FileFilter extends FileFilter {

	@Override
	public String getDescription() {
		return "MP3 file";
	}

	@Override
	public boolean accept(final File f) {
		if (f.isDirectory()) {
			return true;
		}
		return f.isFile() && f.getName().endsWith(".mp3");
	}

}
