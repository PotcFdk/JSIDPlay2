package applet.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T20:55:58.007+0100")
@StaticMetamodel(Musicians.class)
public class Musicians_ {
	public static volatile SingularAttribute<Musicians, Integer> id;
	public static volatile SingularAttribute<Musicians, Games> games;
	public static volatile SingularAttribute<Musicians, String> musician;
	public static volatile SingularAttribute<Musicians, String> group;
	public static volatile SingularAttribute<Musicians, String> nickname;
}
