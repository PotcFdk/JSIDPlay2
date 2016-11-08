package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface TapeFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.tap", "*.t64", "*.prg", "*.p00", "*.zip", "*.[tT][aA][pP]", "*.[tT]64", "*.[pP][rR][gG]", "*.[pP]00", "*.[zZ][iI][pP]",
			"*.[tT][aA][pP].[gG][zZ]", "*.[tT]64.[gG][zZ]", "*.[pP][rR][gG].[gG][zZ]", "*.[pP]00.[gG][zZ]");

	final String DESCRIPTION = "Tape Image (TAP, T64, PRG, P00, GZ or ZIP)";

}
