package applet.entities.config.service;

import java.io.File;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IFavoritesSection;
import applet.entities.config.Configuration;
import applet.entities.config.FavoritesSection;

public class ConfigService {
	private EntityManager em;

	public ConfigService(EntityManager em) {
		this.em = em;
	};

	public Configuration get() {
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Configuration> q = cb.createQuery(Configuration.class);
		Root<Configuration> h = q.from(Configuration.class);
		q.select(h);
		List<Configuration> resultList = em.createQuery(q).setMaxResults(1)
				.getResultList();
		if (resultList.size() != 0) {
			return resultList.get(0);
		}
		return null;
	}

	public IConfig create() {
		Configuration config = new Configuration();
		config.getSidplay2().setVersion(IConfig.REQUIRED_CONFIG_VERSION);
		em.persist(config);
		flush();
		return config;
	}

	public void remove(Configuration config) {
		// remove old configuration from DB
		em.getTransaction().begin();
		em.remove(config);
		em.flush();
		em.clear();
		em.getTransaction().commit();
	}

	public boolean shouldBeRestored(Configuration config) {
		return config.getReconfigFilename() != null;
	}

	public Configuration restore(Configuration config) {
		try {
			// import configuration from file
			JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Object obj = unmarshaller.unmarshal(new File(config
					.getReconfigFilename()));
			if (obj instanceof Configuration) {
				Configuration detachedDbConfig = (Configuration) obj;

				remove(config);

				// restore configuration in DB
				Configuration mergedDbConfig = em.merge(detachedDbConfig);
				em.getTransaction().begin();
				em.persist(mergedDbConfig);
				em.flush();
				em.getTransaction().commit();
				return mergedDbConfig;
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		return config;
	}

	public IFavoritesSection addFavorite(IConfig config, String title) {
		Configuration dbConfig = (Configuration) config;
		FavoritesSection toAdd = new FavoritesSection();
		toAdd.setDbConfig(dbConfig);
		toAdd.setName(title);
		dbConfig.getFavoritesInternal().add(toAdd);
		em.persist(toAdd);
		flush();
		return toAdd;
	}

	public void removeFavorite(IConfig config, int index) {
		Configuration dbConfig = (Configuration) config;
		FavoritesSection toRemove = (FavoritesSection) dbConfig
				.getFavorites().get(index);
		toRemove.setDbConfig(null);
		dbConfig.getFavorites().remove(index);
		em.remove(toRemove);
		flush();
	}

	public void write(IConfig iConfig) {
		em.getTransaction().begin();
		try {
			em.persist(iConfig);
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	private void flush() {
		em.getTransaction().begin();
		try {
			em.flush();
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

}
