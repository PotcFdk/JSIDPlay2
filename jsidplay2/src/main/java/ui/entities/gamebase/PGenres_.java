package ui.entities.gamebase;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value = "org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor")
@StaticMetamodel(PGenres.class)
public abstract class PGenres_ {

	public static volatile SingularAttribute<PGenres, String> parentGenre;
	public static volatile SingularAttribute<PGenres, Games> games;
	public static volatile SingularAttribute<PGenres, Integer> id;

}

