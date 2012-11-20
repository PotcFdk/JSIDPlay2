package applet.entities.config;

import applet.entities.collection.HVSCEntry;
import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-20T21:18:16.872+0100")
@StaticMetamodel(FavoritesSection.class)
public class FavoritesSection_ {
	public static volatile SingularAttribute<FavoritesSection, Integer> id;
	public static volatile SingularAttribute<FavoritesSection, String> name;
	public static volatile ListAttribute<FavoritesSection, FavoriteColumn> columns;
	public static volatile ListAttribute<FavoritesSection, HVSCEntry> favorites;
}
