package ui.common.converter;

import java.io.File;

import de.schlichtherle.truezip.file.TFile;
import javafx.util.StringConverter;

public class FileToStringConverter extends StringConverter<File> {
	@Override
	public String toString(File file) {
		return file != null ? file.getAbsolutePath() : "";
	}

	@Override
	public File fromString(String string) {
		return new TFile(string);
	}

}
