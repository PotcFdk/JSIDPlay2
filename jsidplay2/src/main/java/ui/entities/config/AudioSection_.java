package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import sidplay.audio.Audio;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(AudioSection.class)
public abstract class AudioSection_ {

	public static volatile SingularAttribute<AudioSection, Integer> vbrQuality;
	public static volatile SingularAttribute<AudioSection, Float> thirdBalance;
	public static volatile SingularAttribute<AudioSection, Boolean> delayBypass;
	public static volatile SingularAttribute<AudioSection, Boolean> vbr;
	public static volatile SingularAttribute<AudioSection, Boolean> playOriginal;
	public static volatile SingularAttribute<AudioSection, Float> reverbComb1Delay;
	public static volatile SingularAttribute<AudioSection, Float> reverbComb4Delay;
	public static volatile SingularAttribute<AudioSection, Float> reverbDryWetMix;
	public static volatile SingularAttribute<AudioSection, Integer> mainDelay;
	public static volatile SingularAttribute<AudioSection, Boolean> reverbBypass;
	public static volatile SingularAttribute<AudioSection, Float> secondBalance;
	public static volatile SingularAttribute<AudioSection, Integer> thirdDelay;
	public static volatile SingularAttribute<AudioSection, Float> aviCompressionQuality;
	public static volatile SingularAttribute<AudioSection, String> mp3File;
	public static volatile SingularAttribute<AudioSection, Audio> audio;
	public static volatile SingularAttribute<AudioSection, Float> reverbAllPass2Delay;
	public static volatile SingularAttribute<AudioSection, Integer> secondDelay;
	public static volatile SingularAttribute<AudioSection, Float> mainBalance;
	public static volatile SingularAttribute<AudioSection, Float> reverbSustainDelay;
	public static volatile SingularAttribute<AudioSection, Integer> delayFeedbackLevel;
	public static volatile SingularAttribute<AudioSection, Float> reverbComb2Delay;
	public static volatile SingularAttribute<AudioSection, SamplingMethod> sampling;
	public static volatile SingularAttribute<AudioSection, Integer> delayDryLevel;
	public static volatile SingularAttribute<AudioSection, Float> reverbAllPass1Delay;
	public static volatile SingularAttribute<AudioSection, Float> thirdVolume;
	public static volatile SingularAttribute<AudioSection, Integer> audioBufferSize;
	public static volatile SingularAttribute<AudioSection, Integer> cbr;
	public static volatile SingularAttribute<AudioSection, Integer> delay;
	public static volatile SingularAttribute<AudioSection, Float> secondVolume;
	public static volatile SingularAttribute<AudioSection, Float> reverbComb3Delay;
	public static volatile SingularAttribute<AudioSection, SamplingRate> samplingRate;
	public static volatile SingularAttribute<AudioSection, Float> mainVolume;
	public static volatile SingularAttribute<AudioSection, Integer> delayWetLevel;
	public static volatile SingularAttribute<AudioSection, Integer> device;
	public static volatile SingularAttribute<AudioSection, Integer> bufferSize;

}

