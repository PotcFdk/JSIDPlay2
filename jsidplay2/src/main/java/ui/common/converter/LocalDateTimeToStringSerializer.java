package ui.common.converter;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.io.IOException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LocalDateTimeToStringSerializer extends JsonSerializer<LocalDateTime> {

	@Override
	public void serialize(LocalDateTime localDateTime, JsonGenerator gen, SerializerProvider serializers)
			throws IOException {
		gen.writeObject(localDateTime.format(ISO_DATE_TIME));
	}

}
