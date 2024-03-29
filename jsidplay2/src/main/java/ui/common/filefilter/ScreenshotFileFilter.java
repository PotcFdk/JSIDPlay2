/**
 *
 */
package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public final class ScreenshotFileFilter implements FileFilter {

	public static final String defaultFileNameExt[] = new String[] { ".png", ".gif", ".jpg" };

	@Override
	public boolean accept(File file) {
		if (file.isDirectory()) {
			return true;
		}
		String name = file.getName().toLowerCase(Locale.ENGLISH);
		String[] exts = defaultFileNameExt;
		for (String ext : exts) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}
}