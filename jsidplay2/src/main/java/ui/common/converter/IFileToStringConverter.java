package ui.common.converter;

import java.io.File;

import de.schlichtherle.truezip.file.TFile;

public interface IFileToStringConverter {

	default String toString(File file) {
		return file != null ? file.getAbsolutePath() : null;
	}

	default File fromString(String fileString) {
		return fileString != null && !"".equals(fileString) ? new TFile(fileString) : null;
	}
}
