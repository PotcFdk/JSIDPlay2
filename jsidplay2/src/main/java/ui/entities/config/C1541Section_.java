package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.FloppyType;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(C1541Section.class)
public abstract class C1541Section_ {

	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled1;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled0;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled3;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled2;
	public static volatile SingularAttribute<C1541Section, Boolean> driveSoundOn;
	public static volatile SingularAttribute<C1541Section, FloppyType> floppyType;
	public static volatile SingularAttribute<C1541Section, Boolean> ramExpansionEnabled4;
	public static volatile SingularAttribute<C1541Section, Boolean> jiffyDosInstalled;
	public static volatile SingularAttribute<C1541Section, Boolean> parallelCable;
	public static volatile SingularAttribute<C1541Section, ExtendImagePolicy> extendImagePolicy;
	public static volatile SingularAttribute<C1541Section, Boolean> driveOn;

}

