package applet.entities.config.service;

import java.util.ArrayList;

import javax.persistence.EntityManager;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.entities.config.DbConfig;
import applet.entities.config.DbFavoritesSection;

public class DbFavoritesSectionService {
	private EntityManager em;

	public DbFavoritesSectionService(EntityManager em) {
		this.em = em;
	};

	public void addFavorite(IConfig config, String title) {
		DbConfig dbConfig = (DbConfig) config;
		// Liste mit neuem Element
		ArrayList<DbFavoritesSection> newFavoritesList = new ArrayList<DbFavoritesSection>();
		for (IFavoritesSection f : dbConfig.getFavorites()) {
			DbFavoritesSection newFavorite = new DbFavoritesSection();
			newFavorite.setDbConfig(dbConfig);
			newFavorite.setName(f.getName());
			newFavorite.setFilename(f.getFilename());
			newFavoritesList.add(newFavorite);
			((DbFavoritesSection) f).setDbConfig(null);
			em.remove(f);
		}
		DbFavoritesSection newFavorite = new DbFavoritesSection();
		newFavorite.setDbConfig(dbConfig);
		newFavorite.setName(title);
		newFavoritesList.add(newFavorite);

		// Alles alte löschen
		for (IFavoritesSection fav : dbConfig.getFavorites()) {
			((DbFavoritesSection) fav).setDbConfig(null);
		}
		dbConfig.getFavorites().clear();

		dbConfig.setFavorites(newFavoritesList);
	}

	public void removeFavorite(IConfig config, int index) {
		DbConfig dbConfig = (DbConfig) config;
		DbFavoritesSection toChange = (DbFavoritesSection) dbConfig
				.getFavorites().get(index);
		toChange.setDbConfig(null);
		dbConfig.getFavorites().remove(toChange);
		em.remove(toChange);
	}

}
