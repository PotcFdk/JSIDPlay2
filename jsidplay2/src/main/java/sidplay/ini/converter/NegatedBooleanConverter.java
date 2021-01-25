package sidplay.ini.converter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

/**
 * Converts a string to a negated boolean.
 */
public class NegatedBooleanConverter extends BaseConverter<Boolean> {

	public NegatedBooleanConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Boolean convert(String value) {
		if ("false".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value)) {
			return !Boolean.parseBoolean(value);
		} else {
			throw new ParameterException(getErrorString(value, "a boolean"));
		}
	}

}
