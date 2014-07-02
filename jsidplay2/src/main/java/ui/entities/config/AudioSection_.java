package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

import resid_builder.resid.SamplingMethod;
import sidplay.audio.Audio;

@Generated(value="Dali", date="2012-11-09T20:55:57.976+0100")
@StaticMetamodel(AudioSection.class)
public class AudioSection_ {
	public static volatile SingularAttribute<AudioSection, Audio> audio;
	public static volatile SingularAttribute<AudioSection, Integer> frequency;
	public static volatile SingularAttribute<AudioSection, SamplingMethod> sampling;
	public static volatile SingularAttribute<AudioSection, Boolean> playOriginal;
	public static volatile SingularAttribute<AudioSection, String> mp3File;
	public static volatile SingularAttribute<AudioSection, Float> leftVolume;
	public static volatile SingularAttribute<AudioSection, Float> rightVolume;
}
