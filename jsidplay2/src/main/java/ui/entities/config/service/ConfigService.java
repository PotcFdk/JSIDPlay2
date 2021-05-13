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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import libsidplay.config.IConfig;
import libsidutils.PathUtils;
import ui.entities.DatabaseType;
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
		 * Use JSON configuration files
		 */
		JSON(".json"),
		/**
		 * Use XML configuration files
		 */
		XML(".xml"),
		/**
		 * Use binary database files
		 */
		DATABASE("");

		private String fileExt;

		private ConfigurationType(String fileExt) {
			this.fileExt = fileExt;
		}
	}

	/**
	 * Filename of the jsidplay2 configuration XML file.
	 */
	public static final String CONFIG_FILE = "jsidplay2";

	private EntityManager em;

	private final ConfigurationType configurationType;

	public ConfigService(ConfigurationType configurationType) {
		this.configurationType = configurationType;
	}

	public Configuration load() {
		final File configPath = getConfigPath();

		switch (configurationType) {
		case DATABASE:
			em = Persistence
					.createEntityManagerFactory(PersistenceProperties.CONFIG_DS,
							new PersistenceProperties(DatabaseType.HSQL_FILE, "", "",
									PathUtils.getFilenameWithoutSuffix(configPath.getAbsolutePath())))
					.createEntityManager();
			return get(configPath);

		case JSON:
			em = Persistence
					.createEntityManagerFactory(PersistenceProperties.CONFIG_DS,
							new PersistenceProperties(DatabaseType.HSQL_MEM, "", "", CONFIG_FILE))
					.createEntityManager();
			return importJson(configPath);

		case XML:
		default:
			em = Persistence
					.createEntityManagerFactory(PersistenceProperties.CONFIG_DS,
							new PersistenceProperties(DatabaseType.HSQL_MEM, "", "", CONFIG_FILE))
					.createEntityManager();
			return importXml(configPath);
		}
	}

	public void save(Configuration configuration) {
		final File configPath = getConfigPath();

		switch (configurationType) {
		case DATABASE:
			persist(configuration);
			break;

		case JSON:
			exportJson(configuration, configPath);
			break;

		case XML:
		default:
			exportXml(configuration, configPath);
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
	private Configuration importXml(File file) {
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
	 * Import configuration database from an JSON file.
	 *
	 * If absent or invalid, create a new one.
	 *
	 * @param file JSON file to import
	 * @return imported configuration
	 */
	private Configuration importJson(final File file) {
		if (file.exists()) {
			try {
				final ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				Configuration detachedConfig = objectMapper.readValue(file, Configuration.class);

				// configuration version check
				if (detachedConfig.getSidplay2Section().getVersion() == IConfig.REQUIRED_CONFIG_VERSION) {
					Configuration mergedConfig = em.merge(detachedConfig);
					persist(mergedConfig);
					return mergedConfig;
				}
				createBackup(detachedConfig, file);
				return detachedConfig;
			} catch (IOException e) {
				System.err.println(e.getMessage());
				createBackup(file);
			}
		} else {
			// migration XML to JSON
			File fileAsXml = new File(PathUtils.getFilenameWithoutSuffix(file.getAbsolutePath()) + ".xml");
			if (fileAsXml.exists()) {
				return importXml(fileAsXml);
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
	private void exportXml(Configuration configuration, File file) {
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
	 * Export configuration database into an JSON file.
	 *
	 * @param configuration configuration to export
	 * @param file          target file of the export
	 */
	private void exportJson(Configuration configuration, File file) {
		try {
			final ObjectMapper objectMapper = new ObjectMapper();
			objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, configuration);
		} catch (IOException e) {
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
			File configPlace = new File(s, CONFIG_FILE + configurationType.fileExt);
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		// default directory
		return new File(System.getProperty("user.home"), CONFIG_FILE + configurationType.fileExt);
	}

	/**
	 * Create a backup of the current configuration.
	 *
	 * @param configuration configuration to backup
	 * @param configPath    path to save the backup
	 */
	private void createBackup(Configuration configuration, File configPath) {
		File file = new File(configPath.getParentFile(), configPath.getName() + ".bak");
		exportXml(configuration, file);
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
