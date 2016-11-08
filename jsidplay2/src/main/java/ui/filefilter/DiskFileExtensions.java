package ui.filefilter;

import java.util.Arrays;
import java.util.List;

public interface DiskFileExtensions {
	final List<String> EXTENSIONS = Arrays.asList("*.d64", "*.g64", "*.nib", "*.zip", "*.d64.gz",
			"*.g64.gz", "*.nib.gz", "*.[dD]64", "*.[gG]64", "*.[nN][iI][bB]", "*.[zZ][iI][pP]", "*.[dD]64.[gG][zZ]",
			"*.[gG]64.[gG][zZ]", "*.[nN][iI][bB].[gG][zZ]");

	final String DESCRIPTION = "Disk Image (D64, G64, NIB, GZ or ZIP)";

}
