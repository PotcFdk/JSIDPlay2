package ui.common.converter;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public final class LocalDateTimeXmlAdapter extends XmlAdapter<String, LocalDateTime> {

	@Override
	public LocalDateTime unmarshal(String string) throws Exception {
		return LocalDateTime.parse(string, ISO_DATE_TIME);
	}

	@Override
	public String marshal(LocalDateTime localDateTime) throws Exception {
		return localDateTime.format(ISO_DATE_TIME);
	}

}
