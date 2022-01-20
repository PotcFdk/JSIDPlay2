package ui.common.filefilter;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface MDBFileExtensions {
	List<String> EXTENSIONS = addUpperCase(asList("*.mdb"));

	String DESCRIPTION = "GameBase64 Database";

}
