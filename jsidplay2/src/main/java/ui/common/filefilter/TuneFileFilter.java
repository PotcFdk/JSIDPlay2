/**
*
*/
package ui.common.filefilter;

import static java.util.Arrays.stream;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public final class TuneFileFilter implements FileFilter {

	private static final String ZIP_FILE_NAME_EXT[] = new String[] { ".zip" };

	private AudioTuneFileFilter audioTuneFileFilter = new AudioTuneFileFilter();
	private VideoTuneFileFilter videoTuneFileFilter = new VideoTuneFileFilter();
	private MP3TuneFileFilter mp3TuneFileFilter = new MP3TuneFileFilter();

	@Override
	public boolean accept(File file) {
		return audioTuneFileFilter.accept(file) || videoTuneFileFilter.accept(file) || mp3TuneFileFilter.accept(file)
				|| stream(ZIP_FILE_NAME_EXT).filter(file.getName().toLowerCase(Locale.ENGLISH)::endsWith).findFirst()
						.isPresent();
	}
}