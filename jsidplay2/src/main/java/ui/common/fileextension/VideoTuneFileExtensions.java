package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface VideoTuneFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.c64", "*.prg", "*.p00", "*.t64"));

	String DESCRIPTION = "C64 Tunes";
}
