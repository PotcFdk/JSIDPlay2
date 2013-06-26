package ui.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T20:55:58.007+0100")
@StaticMetamodel(PGenres.class)
public class PGenres_ {
	public static volatile SingularAttribute<PGenres, Integer> id;
	public static volatile SingularAttribute<PGenres, Games> games;
	public static volatile SingularAttribute<PGenres, String> parentGenre;
}
