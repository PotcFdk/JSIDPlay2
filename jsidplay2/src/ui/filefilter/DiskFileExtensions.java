package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface DiskFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.d64", "*.g64", "*.nib",
			"*.zip", "*.d64.gz", "*.g64.gz", "*.nib.gz");

	final String DESCRIPTION = "Disk Image (D64, G64, NIB, GZ or ZIP)";

}
