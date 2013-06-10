package ui.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T20:55:58.007+0100")
@StaticMetamodel(Games.class)
public class Games_ {
	public static volatile SingularAttribute<Games, Integer> id;
	public static volatile SingularAttribute<Games, String> name;
	public static volatile SingularAttribute<Games, Years> years;
	public static volatile SingularAttribute<Games, String> filename;
	public static volatile SingularAttribute<Games, String> fileToRun;
	public static volatile SingularAttribute<Games, Integer> filenameIdx;
	public static volatile SingularAttribute<Games, String> screenshotFilename;
	public static volatile SingularAttribute<Games, Musicians> musicians;
	public static volatile SingularAttribute<Games, Genres> genres;
	public static volatile SingularAttribute<Games, Publishers> publishers;
	public static volatile SingularAttribute<Games, String> sidFilename;
	public static volatile SingularAttribute<Games, Programmers> programmers;
	public static volatile SingularAttribute<Games, String> comment;
}
