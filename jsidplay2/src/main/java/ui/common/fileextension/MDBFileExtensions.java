package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface MDBFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.mdb"));

	String DESCRIPTION = "GameBase64 Database";
}
