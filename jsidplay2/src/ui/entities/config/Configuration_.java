package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2013-05-12T20:53:55.526+0200")
@StaticMetamodel(Configuration.class)
public class Configuration_ {
	public static volatile SingularAttribute<Configuration, Integer> id;
	public static volatile SingularAttribute<Configuration, String> reconfigFilename;
	public static volatile SingularAttribute<Configuration, SidPlay2Section> sidplay2;
	public static volatile SingularAttribute<Configuration, OnlineSection> online;
	public static volatile SingularAttribute<Configuration, C1541Section> c1541;
	public static volatile SingularAttribute<Configuration, PrinterSection> printer;
	public static volatile SingularAttribute<Configuration, JoystickSection> joystick;
	public static volatile SingularAttribute<Configuration, ConsoleSection> console;
	public static volatile SingularAttribute<Configuration, AudioSection> audio;
	public static volatile SingularAttribute<Configuration, EmulationSection> emulation;
	public static volatile SingularAttribute<Configuration, String> currentFavorite;
	public static volatile ListAttribute<Configuration, FavoritesSection> favorites;
	public static volatile ListAttribute<Configuration, FilterSection> filter;
	public static volatile ListAttribute<Configuration, KeyTableEntity> keyCodeMap;
}
