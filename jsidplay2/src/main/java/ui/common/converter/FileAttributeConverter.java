package ui.common.converter;

import java.io.File;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import sidplay.ini.converter.IFileToStringConverter;

@Converter
public class FileAttributeConverter implements AttributeConverter<File, String>, IFileToStringConverter {

	@Override
	public String convertToDatabaseColumn(File file) {
		return toString(file);
	}

	@Override
	public File convertToEntityAttribute(String fileString) {
		return fromString(fileString);
	}

}