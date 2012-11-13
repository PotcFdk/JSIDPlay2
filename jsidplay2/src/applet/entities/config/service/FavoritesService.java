package applet.entities.config.service;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;

import sidplay.ini.intf.IFavoritesSection;
import applet.entities.config.FavoriteColumn;
import applet.entities.config.FavoritesSection;

public class FavoritesService {
	private EntityManager em;

	public FavoritesService(EntityManager em) {
		this.em = em;
	}

	public void addColumn(IFavoritesSection favorite,
			SingularAttribute<?, ?> field) {
		FavoritesSection favoritesSection = ((FavoritesSection) favorite);
		FavoriteColumn column = new FavoriteColumn();
		column.setColumnProperty(field.getJavaMember().getName());
		column.setFavoritesSection(favoritesSection);
		favoritesSection.getColumns().add(column);
		em.persist(column);
	}

	public void removeColumn(IFavoritesSection favorite,
			SingularAttribute<?, ?> field) {
		FavoritesSection favoritesSection = ((FavoritesSection) favorite);
		for (FavoriteColumn column : favoritesSection.getColumns()) {
			if (column.getColumnProperty().equals(
					field.getJavaMember().getName())) {
				column.setFavoritesSection(null);
				favoritesSection.getColumns().remove(column);
				em.remove(column);
				break;
			}
		}
	}

}
