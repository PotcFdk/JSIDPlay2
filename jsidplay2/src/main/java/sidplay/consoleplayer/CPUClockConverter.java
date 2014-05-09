package sidplay.consoleplayer;

import libsidplay.common.CPUClock;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class CPUClockConverter implements IStringConverter<CPUClock> {
	@Override
	public CPUClock convert(String value) throws ParameterException {
		try {
			return CPUClock.valueOf(value);
		} catch (Exception e) {
			throw new ParameterException("Parameter value " + "should be "
					+ CPUClock.PAL + " or " + CPUClock.NTSC + " (found "
					+ value + ")");
		}
	}
}