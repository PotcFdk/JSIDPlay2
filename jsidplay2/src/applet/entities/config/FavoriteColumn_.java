package applet.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

@Generated(value="Dali", date="2012-11-15T23:15:20.591+0100")
@StaticMetamodel(FavoriteColumn.class)
public class FavoriteColumn_ {
	public static volatile SingularAttribute<FavoriteColumn, Integer> id;
	public static volatile SingularAttribute<FavoriteColumn, String> columnProperty;
	public static volatile SingularAttribute<FavoriteColumn, FavoritesSection> favoritesSection;
}
