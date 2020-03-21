package ui.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(Genres.class)
public abstract class Genres_ {

	public static volatile SingularAttribute<Genres, Games> games;
	public static volatile SingularAttribute<Genres, String> genre;
	public static volatile SingularAttribute<Genres, PGenres> parentGenres;
	public static volatile SingularAttribute<Genres, Integer> id;

}

