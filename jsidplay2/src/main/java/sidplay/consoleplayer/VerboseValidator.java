package sidplay.consoleplayer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class VerboseValidator implements IParameterValidator {
	public void validate(String name, String value)
			throws ParameterException {
		int n = Integer.parseInt(value);
		if (n < 0 || n > 2) {
			throw new ParameterException("Parameter " + name
					+ " should be 0, 1 or 2 (found " + value + ")");
		}
	}
}