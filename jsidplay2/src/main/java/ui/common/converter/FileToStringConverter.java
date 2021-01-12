package ui.common.converter;

import java.io.File;

import javafx.util.StringConverter;

public class FileToStringConverter extends StringConverter<File> implements IFileToStringConverter {

	@Override
	public String toString(File file) {
		return toString(file);
	}

	@Override
	public File fromString(String fileString) {
		return fromString(fileString);
	}

}
