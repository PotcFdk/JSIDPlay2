package ui.common.filefilter;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface TuneFileExtensions {
	List<String> EXTENSIONS = addUpperCase(
			asList("*.sid", "*.dat", "*.c64", "*.prg", "*.p00", "*.t64", "*.mus", "*.str", "*.mp3", "*.zip"));

	String DESCRIPTION = "C64 Tunes";

}
