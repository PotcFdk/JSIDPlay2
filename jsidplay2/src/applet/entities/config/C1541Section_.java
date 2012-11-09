package applet.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.C1541.FloppyType;

@Generated(value="Dali", date="2012-11-09T20:55:57.976+0100")
@StaticMetamodel(C1541Section.class)
public class C1541Section_ {
	public static volatile SingularAttribute<C1541Section, Boolean> driveOn;
	public static volatile SingularAttribute<C1541Section, Boolean> driveSoundOn;
	public static volatile SingularAttribute<C1541Section, Boolean> parallelCable;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled0;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled1;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled2;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled3;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled4;
	public static volatile SingularAttribute<C1541Section, ExtendImagePolicy> extendImagePolicy;
	public static volatile SingularAttribute<C1541Section, FloppyType> floppyType;
}
