package libsidplay.config.converter;

import java.io.File;

import com.beust.jcommander.IStringConverter;

public class FileToStringConverter implements IStringConverter<File>, IFileToStringConverter {

	@Override
	public File convert(String fileString) {
		return fromString(fileString);
	}

}
