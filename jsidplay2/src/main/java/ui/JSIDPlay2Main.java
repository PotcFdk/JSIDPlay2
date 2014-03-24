package ui;

import java.io.File;
import java.io.IOException;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import libsidplay.components.c1541.C1541;

import org.hsqldb.DatabaseManager;

import sidplay.ConsolePlayer;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;

/**
 * @author Ken H�ndel
 * @author Joakim Eriksson
 * 
 *         SID Player main class
 */
public class JSIDPlay2Main extends Application {

	/**
	 * Filename of the jsidplay2 configuration database.
	 */
	public static final String CONFIG_DATABASE = "JSIDPLAY2";

	/**
	 * Console player
	 */
	private ConsolePlayer cp;
	/**
	 * Configuration
	 */
	private Configuration config;

	/**
	 * Database support.
	 */
	private EntityManager em;

	/**
	 * Config service class.
	 */
	private ConfigService configService;

	private JSidPlay2 jSidplay2;

	@Override
	public void start(Stage primaryStage) {
		config = getConfiguration();
		initializeTmpDir(config);

		cp = new ConsolePlayer(config);
		String[] args = getParameters().getRaw().toArray(new String[0]);
		if (args.length != 0) {
			cp.args(args);
		}

		jSidplay2 = new JSidPlay2();
		jSidplay2.setConfigService(configService);
		jSidplay2.setConsolePlayer(cp);
		jSidplay2.setPlayer(cp.getPlayer());
		jSidplay2.setConfig(config);
		try {
			jSidplay2.open(primaryStage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Set default position and size
		final SidPlay2Section section = (SidPlay2Section) config.getSidplay2();
		if (section.getFullScreen() != null) {
			primaryStage.setFullScreen(section.getFullScreen());
		}
		primaryStage.fullScreenProperty().addListener(
				(observable, oldValue, newValue) -> section
						.setFullScreen(newValue));
		Scene scene = primaryStage.getScene();
		if (scene != null) {
			Window window = scene.getWindow();
			window.setX(section.getFrameX());
			window.setY(section.getFrameY());
			window.setWidth(section.getFrameWidth());
			window.setHeight(section.getFrameHeight());
			window.widthProperty().addListener(
					(observable, oldValue, newValue) -> section
							.setFrameWidth(newValue.intValue()));
			window.heightProperty().addListener(
					(observable, oldValue, newValue) -> section
							.setFrameHeight(newValue.intValue()));
			window.xProperty().addListener(
					(observable, oldValue, newValue) -> section
							.setFrameX(newValue.intValue()));
			window.yProperty().addListener(
					(observable, oldValue, newValue) -> section
							.setFrameY(newValue.intValue()));
			cp.startC64();
		}
	}

	@Override
	public void stop() {
		cp.stopC64();
		// Eject medias: Make it possible to auto-delete temporary files
		for (final C1541 floppy : cp.getPlayer().getFloppies()) {
			try {
				floppy.getDiskController().ejectDisk();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			cp.getPlayer().getDatasette().ejectTape();
		} catch (IOException e) {
			e.printStackTrace();
		}
		configService.commit(config);

		// Really persist the databases
		DatabaseManager.closeDatabases(0);
		
		em.getEntityManagerFactory().close();
	}

	//
	// Helper methods
	//

	/**
	 * Get the players configuration, create a new one, if absent.
	 * 
	 * @return the players configuration to be used
	 */
	private Configuration getConfiguration() {
		try {
			em = Persistence.createEntityManagerFactory(
					PersistenceProperties.CONFIG_DS,
					new PersistenceProperties(getConfigDatabasePath(),
							Database.HSQL)).createEntityManager();
			configService = new ConfigService(em);
			Configuration config = configService.getOrCreate();
			// Import configuration (if flagged)
			if (configService.shouldBeRestored(config)) {
				System.out.println("Import Configuration!");
				config = configService.importCfg(config);
			}
			// Configuration version check
			if (config.getSidplay2().getVersion() != Configuration.REQUIRED_CONFIG_VERSION) {
				System.err.println("Configuration version "
						+ config.getSidplay2().getVersion()
						+ " is wrong, expected version is "
						+ Configuration.REQUIRED_CONFIG_VERSION);
				return configService.create(config);
			}
			return config;
		} catch (Throwable e) {
			e.printStackTrace();
			// fatal database error?
			System.exit(0);
			return null;
		}
	}

	/**
	 * Search for the database (the players configuration). Search in CWD and in
	 * the HOME folder.
	 * 
	 * @return absolute path name of the database
	 */
	private File getConfigDatabasePath() {
		for (final String s : new String[] { System.getProperty("user.dir"),
				System.getProperty("user.home"), }) {
			File configPlace = new File(s, CONFIG_DATABASE + ".properties");
			if (configPlace.exists()) {
				return new File(configPlace.getParent(), CONFIG_DATABASE);
			}
		}
		// default directory
		return new File(System.getProperty("user.home"), CONFIG_DATABASE);
	}

	/**
	 * Create temp directory, if not exists (default is user home dir).
	 * 
	 * @param config
	 */
	private void initializeTmpDir(Configuration config) {
		String tmpDirPath = config.getSidplay2().getTmpDir();
		File tmpDir = new File(tmpDirPath);
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	/**
	 * Main method. Create an application frame and start emulation.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		launch(args);
	}

}
