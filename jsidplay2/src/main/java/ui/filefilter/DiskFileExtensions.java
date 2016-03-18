package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface DiskFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.[dD]64", "*.[gG]64", "*.[nN][iI][bB]",
			"*.zip", "*.d64.gz", "*.g64.gz", "*.nib.gz");

	final String DESCRIPTION = "Disk Image (D64, G64, NIB, GZ or ZIP)";

}
