package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface DiskFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.d64", "*.g64", "*.nib"));

	String DESCRIPTION = "C64 Disks";
}
