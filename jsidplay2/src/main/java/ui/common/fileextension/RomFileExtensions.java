package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface RomFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.bin"));

	String DESCRIPTION = "C64 ROMs";
}
