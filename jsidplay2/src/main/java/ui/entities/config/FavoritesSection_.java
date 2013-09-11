package ui.entities.config;

import javax.annotation.Generated;
import javax.persistence.metamodel.ListAttribute;
import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import ui.entities.collection.HVSCEntry;

@Generated(value="Dali", date="2013-06-09T13:36:37.478+0200")
@StaticMetamodel(FavoritesSection.class)
public class FavoritesSection_ {
	public static volatile SingularAttribute<FavoritesSection, Integer> id;
	public static volatile SingularAttribute<FavoritesSection, String> name;
	public static volatile SingularAttribute<FavoritesSection, Double> width;
	public static volatile SingularAttribute<FavoritesSection, Integer> selectedRowFrom;
	public static volatile SingularAttribute<FavoritesSection, Integer> selectedRowTo;
	public static volatile ListAttribute<FavoritesSection, FavoriteColumn> columns;
	public static volatile ListAttribute<FavoritesSection, HVSCEntry> favorites;
}
