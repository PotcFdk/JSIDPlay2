package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface TuneFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.sid", "*.dat", "*.c64",
			"*.prg", "*.p00", "*.mus", "*.str", "*.mp3", "*.zip");

	final String DESCRIPTION = "C64 Tunes";

}
