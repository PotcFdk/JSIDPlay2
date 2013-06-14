package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public class ConfigFileFilter implements FileFilter {

	public static final String EXT_CONFIGURATION = ".xml";

	@Override
	public boolean accept(File f) {
		return f.isDirectory() || f.getName().endsWith(EXT_CONFIGURATION);
	}

}
