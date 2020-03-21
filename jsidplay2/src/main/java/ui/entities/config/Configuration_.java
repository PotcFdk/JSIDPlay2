package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import libsidplay.components.keyboard.KeyTableEntry;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Configuration.class)
public abstract class Configuration_ {

	public static volatile ListAttribute<Configuration, FavoritesSection> favorites;
	public static volatile SingularAttribute<Configuration, String> currentFavorite;
	public static volatile SingularAttribute<Configuration, Assembly64Section> assembly64Section;
	public static volatile ListAttribute<Configuration, KeyTableEntity> keyCodeMap;
	public static volatile SingularAttribute<Configuration, SidPlay2Section> sidplay2Section;
	public static volatile ListAttribute<Configuration, FilterSection> filterSection;
	public static volatile SingularAttribute<Configuration, EmulationSection> emulationSection;
	public static volatile SingularAttribute<Configuration, AudioSection> audioSection;
	public static volatile SingularAttribute<Configuration, OnlineSection> onlineSection;
	public static volatile SingularAttribute<Configuration, C1541Section> c1541Section;
	public static volatile SingularAttribute<Configuration, JoystickSection> joystickSection;
	public static volatile SingularAttribute<Configuration, Integer> id;
	public static volatile SingularAttribute<Configuration, PrinterSection> printerSection;
	public static volatile ListAttribute<Configuration, ViewEntity> views;
	public static volatile SingularAttribute<Configuration, KeyTableEntry> keyTabEntry;

}

