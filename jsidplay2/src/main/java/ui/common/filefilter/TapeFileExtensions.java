package ui.common.filefilter;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface TapeFileExtensions {
	List<String> EXTENSIONS = addUpperCase(
			asList("*.tap", "*.t64", "*.prg", "*.p00", "*.zip", "*.tap.gz", "*.t64.gz", "*.prg.gz", "*.p00.gz"));

	String DESCRIPTION = "Tape Image";

}
