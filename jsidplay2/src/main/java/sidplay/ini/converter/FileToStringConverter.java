package sidplay.ini.converter;

import java.io.File;

import com.beust.jcommander.IStringConverter;

public final class FileToStringConverter implements IStringConverter<File>, IFileToStringConverter {

	@Override
	public File convert(String fileString) {
		return fromString(fileString);
	}

}
