package applet.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-09T20:55:58.007+0100")
@StaticMetamodel(Genres.class)
public class Genres_ {
	public static volatile SingularAttribute<Genres, Integer> id;
	public static volatile SingularAttribute<Genres, Games> games;
	public static volatile SingularAttribute<Genres, PGenres> parentGenres;
	public static volatile SingularAttribute<Genres, String> genre;
}
