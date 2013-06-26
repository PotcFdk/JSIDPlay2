package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public class MP3FileFilter implements FileFilter {

	@Override
	public boolean accept(final File f) {
		if (f.isDirectory()) {
			return true;
		}
		return f.isFile() && f.getName().endsWith(".mp3");
	}

}
