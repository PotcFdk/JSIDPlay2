package sidplay.ini.converter;

import static libsidutils.ZipFileUtils.newFile;

import java.io.File;

public interface IFileToStringConverter {

	default String toString(File file) {
		return file != null ? file.getAbsolutePath() : null;
	}

	default File fromString(String fileString) {
		return fileString != null && !"".equals(fileString) && newFile(null, fileString).exists()
				? newFile(null, fileString)
				: null;
	}
}
