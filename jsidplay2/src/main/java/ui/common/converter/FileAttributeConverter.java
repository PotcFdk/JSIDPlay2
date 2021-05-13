package ui.common.converter;

import java.io.File;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import sidplay.ini.converter.IFileToStringConverter;

@Converter
public final class FileAttributeConverter implements AttributeConverter<File, String>, IFileToStringConverter {

	@Override
	public File convertToEntityAttribute(String string) {
		return fromString(string);
	}

	@Override
	public String convertToDatabaseColumn(File file) {
		return toString(file);
	}

}