package applet.entities.config;

import applet.entities.collection.HVSCEntry;
import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-18T22:35:03.161+0100")
@StaticMetamodel(FavoritesSection.class)
public class FavoritesSection_ {
	public static volatile SingularAttribute<FavoritesSection, Integer> id;
	public static volatile SingularAttribute<FavoritesSection, String> name;
	public static volatile ListAttribute<FavoritesSection, FavoriteColumn> columns;
	public static volatile ListAttribute<FavoritesSection, HVSCEntry> favorites;
	public static volatile SingularAttribute<FavoritesSection, Configuration> configuration;
}
