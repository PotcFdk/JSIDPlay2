package sidplay.ini.converter;

import static libsidutils.ZipFileUtils.newFile;

import java.io.File;

public interface IFileToStringConverter {

	default String toString(File file) {
		return file != null ? file.getAbsolutePath() : null;
	}

	default File fromString(String fileString) {
		if (fileString == null || "".equals(fileString)) {
			return null;
		}
		File file = newFile(null, fileString);
		return file.exists() ? file : null;
	}

}
