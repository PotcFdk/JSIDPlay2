/**
 * 
 */
package applet.filefilter;

import java.io.File;
import java.io.FileFilter;

public final class CartFileFilter extends javax.swing.filechooser.FileFilter
		implements FileFilter {

	@Override
	public String getDescription() {
		return "C64 Cartridges (CRT, REU, MMC64-IMA, GEORAM-IMG or ZIP)";
	}

	@Override
	public boolean accept(File file) {
		return file.isDirectory()
				|| file.getAbsolutePath().toLowerCase().endsWith(".reu")
				|| file.getAbsolutePath().toLowerCase().endsWith(".ima")
				|| file.getAbsolutePath().toLowerCase().endsWith(".crt")
				|| file.getName().toLowerCase().endsWith(".img")
				|| file.getName().toLowerCase().endsWith(".zip");
	}
}