package ui.common.filefilter;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface FavoritesExtension {
	List<String> EXTENSIONS = addUpperCase(asList("*.js2"));

	String DESCRIPTION = "Favorite Tunes";

}
