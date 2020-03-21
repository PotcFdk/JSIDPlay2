package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.Ultimate64Mode;
import server.restful.common.Connectors;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(EmulationSection.class)
public abstract class EmulationSection_ {

	public static volatile SingularAttribute<EmulationSection, ChipModel> userSidModel;
	public static volatile SingularAttribute<EmulationSection, Integer> appServerPort;
	public static volatile SingularAttribute<EmulationSection, String> netSIDStereoFilter6581;
	public static volatile SingularAttribute<EmulationSection, Integer> sidNumToRead;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpStereoFilter6581;
	public static volatile SingularAttribute<EmulationSection, Emulation> userEmulation;
	public static volatile SingularAttribute<EmulationSection, ChipModel> stereoSidModel;
	public static volatile SingularAttribute<EmulationSection, CPUClock> defaultClockSpeed;
	public static volatile SingularAttribute<EmulationSection, String> netSIDFilter6581;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteThirdSIDVoice1;
	public static volatile SingularAttribute<EmulationSection, Connectors> appServerConnectors;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpFilter6581;
	public static volatile SingularAttribute<EmulationSection, ChipModel> defaultSidModel;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpThirdSIDFilter6581;
	public static volatile SingularAttribute<EmulationSection, Integer> hardsid6581;
	public static volatile SingularAttribute<EmulationSection, Boolean> thirdSIDFilter;
	public static volatile SingularAttribute<EmulationSection, String> thirdSIDFilter8580;
	public static volatile SingularAttribute<EmulationSection, Integer> ultimate64SyncDelay;
	public static volatile SingularAttribute<EmulationSection, String> netSIDFilter8580;
	public static volatile SingularAttribute<EmulationSection, Emulation> stereoEmulation;
	public static volatile SingularAttribute<EmulationSection, String> filter6581;
	public static volatile SingularAttribute<EmulationSection, String> ultimate64Host;
	public static volatile SingularAttribute<EmulationSection, Emulation> defaultEmulation;
	public static volatile SingularAttribute<EmulationSection, String> netSIDThirdSIDFilter8580;
	public static volatile SingularAttribute<EmulationSection, String> stereoFilter6581;
	public static volatile SingularAttribute<EmulationSection, Ultimate64Mode> ultimate64Mode;
	public static volatile SingularAttribute<EmulationSection, Boolean> digiBoosted8580;
	public static volatile SingularAttribute<EmulationSection, String> appServerKeystoreFile;
	public static volatile SingularAttribute<EmulationSection, CPUClock> userClockSpeed;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpStereoFilter8580;
	public static volatile SingularAttribute<EmulationSection, Integer> netSIDDevPort;
	public static volatile SingularAttribute<EmulationSection, Boolean> detectPSID64ChipModel;
	public static volatile SingularAttribute<EmulationSection, String> netSIDStereoFilter8580;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteVoice4;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteVoice3;
	public static volatile SingularAttribute<EmulationSection, String> filter8580;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteVoice2;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteVoice1;
	public static volatile SingularAttribute<EmulationSection, ChipModel> sidBlaster0Model;
	public static volatile SingularAttribute<EmulationSection, Integer> ultimate64StreamingVideoPort;
	public static volatile SingularAttribute<EmulationSection, Integer> thirdSIDBase;
	public static volatile SingularAttribute<EmulationSection, String> ultimate64StreamingTarget;
	public static volatile SingularAttribute<EmulationSection, Engine> engine;
	public static volatile SingularAttribute<EmulationSection, Integer> hardsid8580;
	public static volatile SingularAttribute<EmulationSection, Integer> ultimate64Port;
	public static volatile SingularAttribute<EmulationSection, ChipModel> sidBlaster1Model;
	public static volatile SingularAttribute<EmulationSection, String> appServerKeyAlias;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpFilter8580;
	public static volatile SingularAttribute<EmulationSection, String> netSIDDevHost;
	public static volatile SingularAttribute<EmulationSection, Integer> dualSidBase;
	public static volatile SingularAttribute<EmulationSection, String> reSIDfpThirdSIDFilter8580;
	public static volatile SingularAttribute<EmulationSection, ChipModel> sidBlaster2Model;
	public static volatile SingularAttribute<EmulationSection, ChipModel> thirdSIDModel;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteStereoVoice2;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteThirdSIDVoice4;
	public static volatile SingularAttribute<EmulationSection, Boolean> stereoFilter;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteStereoVoice1;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteStereoVoice4;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteThirdSIDVoice2;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteStereoVoice3;
	public static volatile SingularAttribute<EmulationSection, Boolean> muteThirdSIDVoice3;
	public static volatile SingularAttribute<EmulationSection, String> thirdSIDFilter6581;
	public static volatile SingularAttribute<EmulationSection, Boolean> force3SIDTune;
	public static volatile SingularAttribute<EmulationSection, Emulation> thirdEmulation;
	public static volatile SingularAttribute<EmulationSection, Boolean> filter;
	public static volatile SingularAttribute<EmulationSection, Boolean> forceStereoTune;
	public static volatile SingularAttribute<EmulationSection, Boolean> fakeStereo;
	public static volatile SingularAttribute<EmulationSection, Integer> ultimate64StreamingAudioPort;
	public static volatile SingularAttribute<EmulationSection, String> stereoFilter8580;
	public static volatile SingularAttribute<EmulationSection, Integer> appServerSecurePort;
	public static volatile SingularAttribute<EmulationSection, String> netSIDThirdSIDFilter6581;

}

