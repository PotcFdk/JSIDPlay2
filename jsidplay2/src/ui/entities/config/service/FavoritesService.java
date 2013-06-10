package ui.entities.config.service;

import java.util.Collections;

import javax.persistence.EntityManager;
import javax.persistence.metamodel.SingularAttribute;

import ui.entities.config.FavoriteColumn;
import ui.entities.config.FavoritesSection;


public class FavoritesService {
	private EntityManager em;

	public FavoritesService(EntityManager em) {
		this.em = em;
	}

	public void addColumn(FavoritesSection favorite,
			SingularAttribute<?, ?> field) {
		FavoritesSection favoritesSection = (favorite);
		FavoriteColumn column = new FavoriteColumn();
		column.setColumnProperty(field.getJavaMember().getName());
		favoritesSection.getColumns().add(column);
		em.persist(column);
	}

	public void removeColumn(FavoritesSection favorite,
			SingularAttribute<?, ?> field) {
		FavoritesSection favoritesSection = (favorite);
		for (FavoriteColumn column : favoritesSection.getColumns()) {
			if (column.getColumnProperty().equals(
					field.getJavaMember().getName())) {
				favoritesSection.getColumns().remove(column);
				em.remove(column);
				break;
			}
		}
	}

	public void moveColumn(FavoritesSection favorite, int fromIndex, int toIndex) {
		Collections.swap(favorite.getColumns(), fromIndex, toIndex);
		em.persist(favorite);
	}

}
