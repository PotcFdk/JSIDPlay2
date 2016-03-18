package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface TapeFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.[tT][aA][pP]", "*.[tT]64", "*.[pP][rR][gG]",
			"*.[pP]00", "*.zip", "*.tap.gz", "*.t64.gz", "*.prg.gz", "*.p00.gz");

	final String DESCRIPTION = "Tape Image (TAP, T64, PRG, P00, GZ or ZIP)";

}
