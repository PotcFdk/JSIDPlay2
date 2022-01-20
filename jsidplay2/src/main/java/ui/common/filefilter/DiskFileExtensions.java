package ui.common.filefilter;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface DiskFileExtensions {
	List<String> EXTENSIONS = addUpperCase(
			asList("*.d64", "*.g64", "*.nib", "*.zip", "*.d64.gz", "*.64.gz", "*.nib.gz"));

	String DESCRIPTION = "Disk Image";

}
