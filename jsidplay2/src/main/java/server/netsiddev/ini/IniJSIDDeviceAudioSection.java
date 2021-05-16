package server.netsiddev.ini;

import libsidplay.common.SamplingRate;
import sidplay.audio.AudioConfig;
import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

/**
 * Audio section of the INI file.
 *
 * @author Ken Händel
 *
 */
public class IniJSIDDeviceAudioSection extends IniSection {

	private static final String SECTION_ID = "Audio";

	public IniJSIDDeviceAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 *
	 * @return Playback/Recording frequency
	 */
	public final SamplingRate getSamplingRate() {
		return iniReader.getPropertyEnum(SECTION_ID, "Sampling Rate", SamplingRate.MEDIUM, SamplingRate.class);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 *
	 * @param samplingRate Playback/Recording frequency
	 */
	public final void setSamplingRate(final SamplingRate samplingRate) {
		iniReader.setProperty(SECTION_ID, "Sampling Rate", samplingRate);
	}

	public int getAudioBufferSize() {
		return iniReader.getPropertyInt(SECTION_ID, "Audio Buffer Size", AudioConfig.getDefaultBufferSize());
	}

}