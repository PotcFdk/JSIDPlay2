package ui.common.converter;

import java.io.File;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import sidplay.ini.converter.IFileToStringConverter;

public final class FileXmlAdapter extends XmlAdapter<String, File> implements IFileToStringConverter {

	@Override
	public File unmarshal(String string) throws Exception {
		return fromString(string);
	}

	@Override
	public String marshal(File file) throws Exception {
		return toString(file);
	}

}
