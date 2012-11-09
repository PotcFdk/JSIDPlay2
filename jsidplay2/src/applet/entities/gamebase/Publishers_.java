package applet.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T20:55:58.007+0100")
@StaticMetamodel(Publishers.class)
public class Publishers_ {
	public static volatile SingularAttribute<Publishers, Integer> id;
	public static volatile SingularAttribute<Publishers, Games> games;
	public static volatile SingularAttribute<Publishers, String> publisher;
}
