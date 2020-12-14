package ui.entities.config.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import libsidplay.config.IConfig;
import libsidutils.PathUtils;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.config.Configuration;

/**
 * Service of the configuration database.
 *
 * @author ken
 *
 */
public class ConfigService {
	/**
	 * Configuration types offered by JSIDPlay2
	 *
	 * @author ken
	 *
	 */
	public enum ConfigurationType {
		/**
		 * Use XML configuration files
		 */
		XML,
		/**
		 * Use binary database files
		 */
		DATABASE
	}

	/**
	 * Filename of the jsidplay2 configuration XML file.
	 */
	public static final String CONFIG_FILE = "jsidplay2";

	private EntityManager em;

	private ConfigurationType configurationType;

	public ConfigService(ConfigurationType configurationType) {
		this.configurationType = configurationType;
	}

	public Configuration load() {
		final File configPath = getConfigPath();

		switch (configurationType) {
		case DATABASE:
			em = Persistence.createEntityManagerFactory(PersistenceProperties.CONFIG_DS, new PersistenceProperties(
					Database.HSQL_FILE, "", "", PathUtils.getFilenameWithoutSuffix(configPath.getAbsolutePath())))
					.createEntityManager();
			return get(configPath);

		case XML:
		default:
			em = Persistence.createEntityManagerFactory(PersistenceProperties.CONFIG_DS,
					new PersistenceProperties(Database.HSQL_MEM, "", "", CONFIG_FILE)).createEntityManager();
			return importCfg(configPath);
		}
	}

	public void save(Configuration configuration) {
		final File configPath = getConfigPath();

		switch (configurationType) {
		case DATABASE:
			persist(configuration);
			break;

		case XML:
		default:
			exportCfg(configuration, configPath);
			break;
		}
	}

	/**
	 * Get configuration database.
	 *
	 * If absent or invalid, create a new one.
	 *
	 * @return configuration
	 */
	private Configuration get(File configPath) {
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
			createBackup(configuration, configPath);
		}
		// remove old configuration
		if (configuration != null) {
			remove(configuration);
		}
		// create new configuration
		return create();
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
	 * Remove configuration database.
	 *
	 * @param configuration configuration to remove
	 */
	private void remove(Configuration configuration) {
		try {
			em.getTransaction().begin();
			em.remove(configuration);
			em.getTransaction().commit();
		} catch (Exception e) {
			e.printStackTrace();
			if (em.getTransaction().isActive()) {
				em.getTransaction().rollback();
			}
		}
	}

	/**
	 * Persist configuration database.
	 *
	 * @param config configuration to persist
	 */
	private void persist(Configuration config) {
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
		em.close();
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
	}

	/**
	 * Import configuration database from an XML file.
	 *
	 * If absent or invalid, create a new one.
	 *
	 * @param file XML file to import
	 * @return imported configuration
	 */
	private Configuration importCfg(File file) {
		if (file.exists()) {
			try {
				JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
				Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
				Object obj = unmarshaller.unmarshal(file);
				if (obj instanceof Configuration) {
					Configuration detachedConfig = (Configuration) obj;

					// configuration version check
					if (detachedConfig.getSidplay2Section().getVersion() == IConfig.REQUIRED_CONFIG_VERSION) {
						Configuration mergedConfig = em.merge(detachedConfig);
						persist(mergedConfig);
						return mergedConfig;
					}
					createBackup(detachedConfig, file);
				}
			} catch (JAXBException e) {
				System.err.println(e.getMessage());
				createBackup(file);
			}
		}
		return create();
	}

	/**
	 * Export configuration database into an XML file.
	 *
	 * @param configuration configuration to export
	 * @param file          target file of the export
	 */
	private void exportCfg(Configuration configuration, File file) {
		try {
			JAXBContext jaxbContext = JAXBContext.newInstance(Configuration.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			marshaller.marshal(configuration, file);
		} catch (JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Search for the configuration. Search in CWD and in the HOME folder.
	 *
	 * @return XML configuration file
	 */
	private File getConfigPath() {
		for (final String s : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
			File configPlace = new File(s, CONFIG_FILE + ".xml");
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		// default directory
		return new File(System.getProperty("user.home"), CONFIG_FILE + ".xml");
	}

	/**
	 * Create a backup of the current configuration.
	 *
	 * @param configuration configuration to backup
	 * @param configPath    path to save the backup
	 */
	private void createBackup(Configuration configuration, File configPath) {
		File file = new File(configPath.getParentFile(), configPath.getName() + ".bak");
		exportCfg(configuration, file);
	}

	private void createBackup(File configPath) {
		File bakFile = new File(configPath.getParentFile(), configPath.getName() + ".bak");
		try {
			if (!bakFile.exists()) {
				Files.copy(configPath.toPath(), bakFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e1) {
			// ignore
		}
	}

}
