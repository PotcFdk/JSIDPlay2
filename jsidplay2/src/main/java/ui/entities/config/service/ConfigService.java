package ui.entities.config.service;

import java.io.File;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import libsidplay.config.IConfig;
import ui.entities.config.Configuration;

/**
 * Service of the configuration database.
 * 
 * @author ken
 *
 */
public class ConfigService {
	private EntityManager em;

	public ConfigService(EntityManager em) {
		this.em = em;
	}

	/**
	 * Get configuration database. If absent or invalid, create a new one.
	 * 
	 * @return configuration
	 */
	public Configuration getOrCreate() {
		Configuration configuration = null;
		// read configuration from database
		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<Configuration> q = cb.createQuery(Configuration.class);
		Root<Configuration> h = q.from(Configuration.class);
		q.select(h);
		Optional<Configuration> first = em.createQuery(q).getResultList().stream().findFirst();
		if (first.isPresent()) {
			configuration = first.get();
			// configuration version check
			if (configuration.getSidplay2Section().getVersion() == IConfig.REQUIRED_CONFIG_VERSION) {
				return configuration;
			}
		}
		// remove old configuration
		if (configuration != null) {
			remove(configuration);
		}
		// create new configuration
		return create();
	}

	/**
	 * Remove configuration database.
	 * 
	 * @param configuration
	 *            configuration to remove
	 */
	private void remove(Configuration configuration) {
		em.remove(configuration);
		em.clear();
	}

	/**
	 * Create a new configuration and persist into the database.
	 * 
	 * @return newly created configuration
	 */
	private Configuration create() {
		Configuration configuration = new Configuration();
		configuration.getSidplay2Section().setVersion(IConfig.REQUIRED_CONFIG_VERSION);
		persist(configuration);
		return configuration;
	}

	/**
	 * Export configuration database into an XML file.
	 * 
	 * @param configation
	 *            configuration to export
	 * @param file
	 *            target file of the export
	 */
	public void exportCfg(Configuration configation, File file) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(configation, file);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Import configuration database from an XML file.
	 * 
	 * @param file
	 *            XML file to import
	 * @return imported configuration
	 */
	public Configuration importCfg(File file) {
		if (file.exists()) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				Object obj = unmarshaller.unmarshal(file);
				if (obj instanceof Configuration) {
					Configuration detachedConfig = (Configuration) obj;

					Configuration mergedConfig = em.merge(detachedConfig);
					persist(mergedConfig);

					return mergedConfig;
				}
			} catch (JAXBException e) {
				System.err.println(e.getMessage());
			}
		}
		return create();
	}

	/**
	 * Persist configuration database.
	 * 
	 * @param config
	 *            configuration to persist
	 */
	public void persist(Configuration config) {
		try {
			em.getTransaction().begin();
			em.persist(config);
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	/**
	 * Close configuration database.
	 */
	public void close() {
		em.getEntityManagerFactory().close();
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
	}
}
