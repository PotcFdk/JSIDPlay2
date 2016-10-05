package ui.common;

import java.io.File;

import de.schlichtherle.truezip.file.TFile;
import javafx.util.StringConverter;

public class FileToStringConverter extends StringConverter<File> {
	@Override
	public String toString(File file) {
		return file != null && file.exists() ? file.getAbsolutePath() : "";
	}

	@Override
	public File fromString(String string) {
		if (string != null && string.length() > 0) {
			TFile file = new TFile(string);
			if (file.exists()) {
				return file;
			}
		}
		return null;
	}

}
