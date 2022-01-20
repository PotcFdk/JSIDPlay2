package ui.common.filefilter;

import static java.util.Arrays.stream;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public final class DiskFileFilter implements FileFilter {

	private static final String DEFAULT_FILE_NAME_EXT[] = new String[] { ".d64", ".g64", ".nib" };

	@Override
	public boolean accept(File file) {
		return file.isDirectory() || stream(DEFAULT_FILE_NAME_EXT)
				.filter(file.getName().toLowerCase(Locale.ENGLISH)::endsWith).findFirst().isPresent();
	}
}
