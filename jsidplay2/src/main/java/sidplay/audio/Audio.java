package sidplay.audio;

import java.lang.reflect.InvocationTargetException;

import libsidplay.config.IAudioSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import sidplay.audio.MP3Driver.MP3File;

/**
 * Audio output.
 * 
 * @author ken
 *
 */
public enum Audio {
	/** Java Sound API. */
	SOUNDCARD(JavaSound.class),
	/** WAV file write. */
	WAV(WavFile.class),
	/** MP3 file write. */
	MP3(MP3File.class),
	/** Java Sound API plus WAV file write. */
	LIVE_WAV(ProxyDriver.class, JavaSound.class, WavFile.class),
	/** Java Sound API plus MP3 file write. */
	LIVE_MP3(ProxyDriver.class, JavaSound.class, MP3File.class),
	/** Java Sound API plus play-back of MP3 recording. */
	COMPARE_MP3(CmpMP3File.class);

	private final Class<? extends AudioDriver> audioDriverClass;
	private final Class<? extends AudioDriver>[] parameterClasses;
	private AudioDriver audioDriver;

	/**
	 * Create audio output using the audio driver
	 * 
	 * @param audioDriver
	 *            audio driver
	 */
	@SafeVarargs
	Audio(Class<? extends AudioDriver> audioDriverClass, Class<? extends AudioDriver>... parameters) {
		this.audioDriverClass = audioDriverClass;
		this.parameterClasses = parameters;
	}

	/**
	 * Get audio driver
	 * 
	 * @return audio driver
	 */
	public final AudioDriver getAudioDriver() {
		try {
			if (audioDriver == null) {
				int parameterNum = 0;
				Class<?> parameterTypes[] = new Class<?>[parameterClasses.length];
				Object parametersValues[] = new Object[parameterClasses.length];
				for (Class<?> parameterClass : parameterClasses) {
					parameterTypes[parameterNum] = AudioDriver.class;
					parametersValues[parameterNum++] = parameterClass.getConstructor().newInstance();
				}
				audioDriver = audioDriverClass.getConstructor(parameterTypes).newInstance(parametersValues);
			}
			return audioDriver;
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			throw new RuntimeException("Audiodriver cannot be instanciated: " + audioDriverClass.getName(), e);
		}
	}

	private AudioDriver oldAudioDriver;

	/**
	 * Get audio driver for tune.
	 * 
	 * @param audioSection
	 *            configuration
	 * @param tune
	 *            SID tune
	 * @return audio driver to use
	 */
	public final AudioDriver getAudioDriver(final IAudioSection audioSection, final SidTune tune) {
		return handleMP3(audioSection, tune, getAudioDriver());
	}

	/**
	 * MP3 play-back is using the COMPARE audio driver. Old settings are saved
	 * (playing mp3) and restored (next time normal tune is played).
	 */
	private AudioDriver handleMP3(final IAudioSection audioSection, final SidTune tune, final AudioDriver audioDriver) {
		AudioDriver newAudioDriver = audioDriver;
		if (oldAudioDriver == null && tune instanceof MP3Tune) {
			// save settings before MP3 gets played
			oldAudioDriver = audioDriver;
		} else if (oldAudioDriver != null && !(tune instanceof MP3Tune)) {
			// restore settings after MP3 has been played last time
			newAudioDriver = oldAudioDriver;
			oldAudioDriver = null;
		}
		if (tune instanceof MP3Tune) {
			// Change driver settings to use comparison driver for MP3 play-back
			audioSection.setPlayOriginal(true);
			audioSection.setMp3File(((MP3Tune) tune).getMP3Filename());
			newAudioDriver = COMPARE_MP3.getAudioDriver();
		}
		if (COMPARE_MP3.getAudioDriver().equals(newAudioDriver)) {
			((CmpMP3File) newAudioDriver).setAudioSection(audioSection);
		}
		return newAudioDriver;
	}

}