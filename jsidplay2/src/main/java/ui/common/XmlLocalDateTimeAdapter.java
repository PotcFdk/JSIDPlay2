package ui.common;

import static java.time.format.DateTimeFormatter.ISO_DATE_TIME;

import java.time.LocalDateTime;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class XmlLocalDateTimeAdapter extends XmlAdapter<String, LocalDateTime> {
	@Override
	public String marshal(LocalDateTime localDateTime) throws Exception {
		return localDateTime.format(ISO_DATE_TIME);
	}

	@Override
	public LocalDateTime unmarshal(String localDateTime) throws Exception {
		return LocalDateTime.parse(localDateTime, ISO_DATE_TIME);
	}
}
