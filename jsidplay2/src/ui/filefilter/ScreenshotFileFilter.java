/**
 * 
 */
package ui.filefilter;

import java.io.File;
import java.io.FileFilter;

public final class ScreenshotFileFilter extends
		javax.swing.filechooser.FileFilter implements FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".png",
			".gif", ".jpg" };

	@Override
	public String getDescription() {
		return "C64 screenshots (PNG, GIF or JPG)";
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