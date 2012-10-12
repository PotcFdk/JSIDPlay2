package applet.entities.config.service;

import javax.persistence.EntityManager;

import sidplay.ini.intf.IConfig;
import applet.entities.config.DbConfig;
import applet.entities.config.DbFavoritesSection;

public class DbConfigService {
	private EntityManager em;

	public DbConfigService(EntityManager em) {
		this.em = em;
	};

	public void addFavorite(IConfig config, String title) {
		DbConfig dbConfig = (DbConfig) config;
		DbFavoritesSection toAdd = new DbFavoritesSection();
		toAdd.setDbConfig(dbConfig);
		toAdd.setName(title);
		dbConfig.getFavoritesInternal().add(toAdd);
		em.persist(toAdd);
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
