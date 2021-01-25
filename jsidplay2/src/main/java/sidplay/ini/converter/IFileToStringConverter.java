package sidplay.ini.converter;

import java.io.File;

import libsidutils.ZipFileUtils;

public interface IFileToStringConverter {

	default String toString(File file) {
		return file != null ? file.getAbsolutePath() : null;
	}

	default File fromString(String fileString) {
		return fileString != null && !"".equals(fileString) ? ZipFileUtils.newFile(null, fileString) : null;
	}
}
