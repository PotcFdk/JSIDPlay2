package ui.common.converter;

import java.io.File;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class FileXmlAdapter extends XmlAdapter<String, File> implements IFileToStringConverter {
	@Override
	public String marshal(File file) throws Exception {
		return toString(file);
	}

	@Override
	public File unmarshal(String fileString) throws Exception {
		return fromString(fileString);
	}
}
