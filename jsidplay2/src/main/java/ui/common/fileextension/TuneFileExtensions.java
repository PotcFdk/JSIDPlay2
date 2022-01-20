package ui.common.fileextension;

import static java.util.stream.Stream.concat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public interface TuneFileExtensions {

	List<String> EXTENSIONS = concat(
			concat(AudioTuneFileExtensions.EXTENSIONS.stream(), VideoTuneFileExtensions.EXTENSIONS.stream()),
			MP3TuneFileExtensions.EXTENSIONS.stream()).collect(Collectors.toList());

	String DESCRIPTION = "C64 Tunes";

	static List<String> addUpperCase(List<String> fileExtensions) {
		List<String> result = new ArrayList<>(fileExtensions);
		fileExtensions.stream().map(fileName -> fileName.toUpperCase(Locale.US)).forEach(result::add);
		return result;
	}
}
