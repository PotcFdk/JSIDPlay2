/**
 * 
 */
package applet.filefilter;

import java.io.File;
import java.io.FileFilter;

public final class RomFileFilter extends javax.swing.filechooser.FileFilter
		implements FileFilter {

	@Override
	public String getDescription() {
		return "ROM images (BIN or ZIP)";
	}

	@Override
	public boolean accept(File file) {
		return file.isDirectory()
				|| file.getName().toLowerCase().endsWith(".bin")
				|| file.getName().toLowerCase().endsWith(".zip");
	}
}