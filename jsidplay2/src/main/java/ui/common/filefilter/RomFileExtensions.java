package ui.common.filefilter;

import static java.util.Arrays.asList;
import static ui.common.filefilter.TuneFileFilter.addUpperCase;

import java.util.List;

public interface RomFileExtensions {
	List<String> EXTENSIONS = addUpperCase(asList("*.bin", "*.zip"));

	String DESCRIPTION = "ROM images";

}
