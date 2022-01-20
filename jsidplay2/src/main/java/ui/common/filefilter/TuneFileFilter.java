/**
*
*/
package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;

public final class TuneFileFilter implements FileFilter {

	private AudioTuneFileFilter audioTuneFileFilter = new AudioTuneFileFilter();
	private VideoTuneFileFilter videoTuneFileFilter = new VideoTuneFileFilter();
	private MP3TuneFileFilter mp3TuneFileFilter = new MP3TuneFileFilter();

	@Override
	public boolean accept(File file) {
		return audioTuneFileFilter.accept(file) || videoTuneFileFilter.accept(file) || mp3TuneFileFilter.accept(file);
	}
}