package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface AudioTuneFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.sid", "*.dat", "*.mus", "*.str"));

	String DESCRIPTION = "C64 Tunes";
}
