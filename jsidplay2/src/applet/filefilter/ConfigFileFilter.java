package applet.filefilter;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class ConfigFileFilter extends FileFilter {

	public static final String EXT_CONFIGURATION = ".xml";

	@Override
	public boolean accept(File f) {
		return f.isDirectory() || f.getName().endsWith(EXT_CONFIGURATION);
	}

	@Override
	public String getDescription() {
		return "Configuration Files";
	}

}
