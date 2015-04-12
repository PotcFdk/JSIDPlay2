package ui;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import libsidplay.Player;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import ui.common.dialog.AlertDialog;
import ui.entities.Database;
import ui.entities.PersistenceProperties;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;

/**
 * @author Ken Händel
 * @author Joakim Eriksson
 * 
 *         SID Player main class
 */
public class JSidPlay2Main extends Application {

	/**
	 * Language dependent message.
	 */
	private static final String IMPORT_CONFIGURATION = "IMPORT_CONFIGURATION";
	/**
	 * Language dependent message.
	 */
	private static final String CONFIGURATION_ERROR = "CONFIGURATION_ERROR";

	/**
	 * Filename of the jsidplay2 configuration database.
	 */
	public static final String CONFIG_DATABASE = "JSIDPLAY2";

	/**
	 * Player
	 */
	private Player player;

	private Consumer<Player> menuHook = player -> {
		if (player.getTune() != SidTune.RESET) {
			SidTuneInfo info = player.getTune().getInfo();
			Iterator<String> detail = info.getInfoString().iterator();
			System.out.print("Playing: ");
			while (detail.hasNext()) {
				System.out.print(detail.next());
				if (detail.hasNext()) {
					System.out.print(", ");
				}
			}
			if (info.getSongs() > 1) {
				System.out.print(", sub-song: ");
				System.out.print(info.getCurrentSong());
			}
			System.out.println();
		}
	};

	/**
	 * Database support.
	 */
	private EntityManager em;

	/**
	 * Config service class.
	 */
	private ConfigService configService;

	private static JSidPlay2 testInstance;

	@Override
	public void start(Stage primaryStage) {
		player = new Player(getConfiguration());
		player.setMenuHook(menuHook);
		player.startC64();

		final JSidPlay2 jSidplay2 = new JSidPlay2(primaryStage, player);
		jSidplay2.setConfigService(configService);
		jSidplay2.open();
		// Set default position and size
		final SidPlay2Section section = (SidPlay2Section) player.getConfig()
				.getSidplay2();
		if (section.getFullScreen() != null) {
			primaryStage.setFullScreen(section.getFullScreen());
		}
		primaryStage.fullScreenProperty().addListener(
				(observable, oldValue, newValue) -> section
						.setFullScreen(newValue));
		final Scene scene = primaryStage.getScene();
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
			processCommandLineArgs();
		}
		testInstance = jSidplay2;
	}

	/**
	 * Play tune, if command line argument provided.
	 */
	private void processCommandLineArgs() {
		List<String> raw = getParameters().getRaw();
		if (raw.size() != 0) {
			try {
				player.play(SidTune.load(new File(raw.get(0))));
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		player.stopC64();
		// Eject medias: Make it possible to auto-delete temporary files
		for (final C1541 floppy : player.getFloppies()) {
			try {
				floppy.getDiskController().ejectDisk();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			player.getDatasette().ejectTape();
		} catch (IOException e) {
			e.printStackTrace();
		}
		configService.commit((Configuration) player.getConfig());

		em.getEntityManagerFactory().close();

		// Really persist the databases
		org.hsqldb.DatabaseManager
				.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
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
		ResourceBundle bundle = ResourceBundle.getBundle(JSidPlay2Main.class
				.getName());
		try {
			em = Persistence.createEntityManagerFactory(
					PersistenceProperties.CONFIG_DS,
					new PersistenceProperties(getConfigDatabasePath(),
							Database.HSQL)).createEntityManager();
			configService = new ConfigService(em);
			Configuration config = configService.getOrCreate();
			// Import configuration (if flagged)
			if (configService.shouldBeRestored(config)) {
				System.out.printf(bundle.getString(IMPORT_CONFIGURATION),
						config.getReconfigFilename());
				config = configService.importCfg(config);
			}
			return config;
		} catch (Throwable e) {
			// fatal database error?
			AlertDialog dialog = new AlertDialog(player);
			dialog.getStage().setTitle(
					bundle.getString("IMPORT_CONFIGURATION_FAILURE"));
			dialog.setText(e.getMessage()
					+ "\n"
					+ String.format(bundle.getString(CONFIGURATION_ERROR),
							getConfigDatabasePath().getAbsolutePath()));
			dialog.open();
			System.out.println(e.getMessage());
			System.out.printf(bundle.getString(CONFIGURATION_ERROR),
					getConfigDatabasePath().getAbsolutePath());
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
	 * Main method. Create an application frame and start emulation.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	public static JSidPlay2 getInstance() {
		return testInstance;
	}
}
