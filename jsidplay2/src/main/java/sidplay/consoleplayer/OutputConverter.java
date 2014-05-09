package sidplay.consoleplayer;

import resid_builder.resid.ChipModel;
import sidplay.audio.Output;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class OutputConverter implements IStringConverter<Output> {
	@Override
	public Output convert(String value) throws ParameterException {
		try {
			return Output.valueOf(value);
		} catch (Exception e) {
			throw new ParameterException("Parameter value " + "should be "
					+ Output.OUT_NULL + ", " + Output.OUT_SOUNDCARD + ", "
					+ Output.OUT_WAV + ", " + Output.OUT_MP3 + ", "
					+ Output.OUT_LIVE_WAV + ", " + Output.OUT_LIVE_MP3
					+ " or " + ChipModel.MOS8580 + " (found " + value + ")");
		}
	}
}