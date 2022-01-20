package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface TapeFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.tap", "*.t64", "*.prg", "*.p00"));

	String DESCRIPTION = "Tape Image";
}
