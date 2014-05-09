package sidplay.consoleplayer;

import sidplay.audio.Audio;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class OutputConverter implements IStringConverter<Audio> {
	@Override
	public Audio convert(String value) throws ParameterException {
		try {
			return Audio.valueOf(value);
		} catch (Exception e) {
			throw new ParameterException("Parameter value " + "should be "
					+ Audio.NONE + ", " + Audio.SOUNDCARD + ", " + Audio.WAV
					+ ", " + Audio.MP3 + ", " + Audio.LIVE_WAV + " or "
					+ Audio.LIVE_MP3 + " (found " + value + ")");
		}
	}
}