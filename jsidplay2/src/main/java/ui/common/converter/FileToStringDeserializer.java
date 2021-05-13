package ui.common.converter;

import java.io.File;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import sidplay.ini.converter.IFileToStringConverter;

public class FileToStringDeserializer extends JsonDeserializer<File> implements IFileToStringConverter {

	@Override
	public File deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		return fromString(p.readValueAs(String.class));
	}
}
