package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TuneFileFilter implements FileFilter {

	private AudioTuneFileFilter audioTuneFileFilter = new AudioTuneFileFilter();
	private VideoTuneFileFilter videoTuneFileFilter = new VideoTuneFileFilter();
	private MP3TuneFileFilter mp3TuneFileFilter = new MP3TuneFileFilter();

	@Override
	public boolean accept(File file) {
		return audioTuneFileFilter.accept(file) || videoTuneFileFilter.accept(file) || mp3TuneFileFilter.accept(file);
	}

	static final List<String> addUpperCase(List<String> fileExtensions) {
		List<String> result = new ArrayList<>(fileExtensions);
		fileExtensions.stream().map(fileName -> fileName.toUpperCase(Locale.US)).forEach(result::add);
		return result;
	}
}
