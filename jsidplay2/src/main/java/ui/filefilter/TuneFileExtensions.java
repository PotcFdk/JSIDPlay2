package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface TuneFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.[sS][iI][dD]", "*.[dD][aA][tT]", "*.[cC]64",
			"*.[pP][rR][gG]", "*.[pP]00", "*.[tT]64", "*.[mM][uU][sS]", "*.[sS][tT][rR]", "*.[mM][pP]3", "*.zip");

	final String DESCRIPTION = "C64 Tunes";

}
