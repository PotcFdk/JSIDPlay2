package applet.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.common.ISID2Types.CPUClock;
import resid_builder.resid.ISIDDefs.ChipModel;

@Generated(value="Dali", date="2012-11-09T20:55:57.992+0100")
@StaticMetamodel(EmulationSection.class)
public class EmulationSection_ {
	public static volatile SingularAttribute<EmulationSection, CPUClock> defaultClockSpeed;
	public static volatile SingularAttribute<EmulationSection, CPUClock> userClockSpeed;
	public static volatile SingularAttribute<EmulationSection, ChipModel> defaultSidModel;
	public static volatile SingularAttribute<EmulationSection, ChipModel> userSidModel;
	public static volatile SingularAttribute<EmulationSection, Integer> hardsid6581;
	public static volatile SingularAttribute<EmulationSection, Integer> hardsid8580;
	public static volatile SingularAttribute<EmulationSection, Boolean> filter;
	public static volatile SingularAttribute<EmulationSection, String> filter6581;
	public static volatile SingularAttribute<EmulationSection, String> filter8580;
	public static volatile SingularAttribute<EmulationSection, Boolean> digiBoosted8580;
	public static volatile SingularAttribute<EmulationSection, Integer> dualSidBase;
	public static volatile SingularAttribute<EmulationSection, Boolean> forceStereoTune;
	public static volatile SingularAttribute<EmulationSection, ChipModel> stereoSidModel;
}
