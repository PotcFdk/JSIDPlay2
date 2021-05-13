package ui.common.converter;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import sidplay.ini.converter.IFileToStringConverter;

public class FileToStringSerializer extends JsonSerializer<File> implements IFileToStringConverter {

	@Override
	public void serialize(File tmpInt, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
			throws IOException, JsonProcessingException {
		jsonGenerator.writeObject(toString(tmpInt));
	}
}
