package applet.entities.config;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class IntegerAdapter extends XmlAdapter<String, Integer> {

	@Override
	public Integer unmarshal(String s) {
		return Integer.parseInt(s);
	}

	@Override
	public String marshal(Integer number) {
		if (number == null)
			return "";

		return number.toString();
	}
}