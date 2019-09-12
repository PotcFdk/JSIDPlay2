package server.netsiddev.ini;

import libsidplay.common.SamplingRate;
import sidplay.ini.IniReader;
import sidplay.ini.IniSection;

/**
 * Audio section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniJSIDDeviceAudioSection extends IniSection {
	public IniJSIDDeviceAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	public int getDevice() {
		return iniReader.getPropertyInt("Audio", "Device", 0);
	}

	public void setDevice(int device) {
		iniReader.setProperty("Audio", "Device", device);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 * 
	 * @return Playback/Recording frequency
	 */
	public final SamplingRate getSamplingRate() {
		return iniReader.getPropertyEnum("Audio", "Sampling Rate", SamplingRate.MEDIUM, SamplingRate.class);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 * 
	 * @param samplingRate
	 *            Playback/Recording frequency
	 */
	public final void setSamplingRate(final SamplingRate samplingRate) {
		iniReader.setProperty("Audio", "Sampling Rate", samplingRate);
	}

	public int getAudioBufferSize() {
		return iniReader.getPropertyInt("Audio", "Audio Buffer Size", 4096);
	}

	public void setAudioBufferSize(int audioBufferSize) {
		iniReader.setProperty("Audio", "Audio Buffer Size", audioBufferSize);
	}
}