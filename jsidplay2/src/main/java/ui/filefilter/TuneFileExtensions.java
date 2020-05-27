package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface TuneFileExtensions {
	List<String> EXTENSIONS = Arrays.asList("*.sid", "*.dat", "*.c64", "*.prg", "*.p00", "*.t64", "*.mus", "*.str",
			"*.mp3", "*.zip", "*.[sS][iI][dD]", "*.[dD][aA][tT]", "*.[cC]64", "*.[pP][rR][gG]", "*.[pP]00", "*.[tT]64",
			"*.[mM][uU][sS]", "*.[sS][tT][rR]", "*.[mM][pP]3", "*.[zZ][iI][pP]");

	String DESCRIPTION = "C64 Tunes";

}
