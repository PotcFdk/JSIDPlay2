package ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import sidplay.Player;
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
@Parameters(resourceBundle = "ui.JSidPlay2Main")
public class JSidPlay2Main extends Application {

	/**
	 * Configuration types offered by JSIDPlay2
	 * 
	 * @author ken
	 *
	 */
	private enum ConfigurationType {
		/**
		 * Use XML configuration files
		 */
		XML,
		/**
		 * Use binary database files
		 */
		DATABASE
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--configurationType", "-c" }, descriptionKey = "CONFIGURATION_TYPE")
	private ConfigurationType configurationType = ConfigurationType.XML;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

	/**
	 * Filename of the jsidplay2 configuration XML file.
	 */
	public static final String CONFIG_FILE = "jsidplay2";

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
			String path = player.getSidDatabaseInfo(db -> db.getPath(player.getTune()), "");
			if (path.length() > 0) {
				System.out.print(", ");
				System.out.print(path);
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
		player = new Player(getConfigurationFromCommandLineArgs());
		player.setMenuHook(menuHook);
		// automatically load tune on start-up
		Optional<String> filename = filenames.stream().findFirst();
		if (filename.isPresent()) {
			try {
				player.setTune(SidTune.load(new File(filename.get())));
			} catch (IOException | SidTuneError e) {
				System.err.println(e.getMessage());
			}
		}
		final JSidPlay2 jSidplay2 = new JSidPlay2(primaryStage, player);
		// Set default position and size
		final SidPlay2Section section = (SidPlay2Section) player.getConfig().getSidplay2Section();
		if (section.getFullScreen() != null) {
			primaryStage.setFullScreen(section.getFullScreen());
		}
		primaryStage.fullScreenProperty()
				.addListener((observable, oldValue, newValue) -> section.setFullScreen(newValue));
		final Scene scene = primaryStage.getScene();
		if (scene != null) {
			Window window = scene.getWindow();
			window.setX(section.getFrameX());
			window.setY(section.getFrameY());
			window.setWidth(section.getFrameWidth());
			window.setHeight(section.getFrameHeight());
			window.widthProperty()
					.addListener((observable, oldValue, newValue) -> section.setFrameWidth(newValue.intValue()));
			window.heightProperty()
					.addListener((observable, oldValue, newValue) -> section.setFrameHeight(newValue.intValue()));
			window.xProperty().addListener((observable, oldValue, newValue) -> section.setFrameX(newValue.intValue()));
			window.yProperty().addListener((observable, oldValue, newValue) -> section.setFrameY(newValue.intValue()));
		}
		jSidplay2.open();
		testInstance = jSidplay2;
	}

	/**
	 * Parse optional command line arguments.
	 * 
	 * @return configuration database chosen by command line arguments
	 */
	private Configuration getConfigurationFromCommandLineArgs() {
		try {
			Parameters parameters = getParameters();
			if (parameters != null) {
				JCommander commander = new JCommander(this, parameters.getRaw().toArray(new String[0]));
				commander.setProgramName(getClass().getName());
				commander.setCaseSensitiveOptions(true);
				if (help) {
					commander.usage();
					System.out.println("Press <enter> to exit!");
					System.in.read();
					System.exit(0);
				}
			}
		} catch (ParameterException | IOException e) {
			System.err.println(e.getMessage());
		}
		return getConfiguration();
	}

	@Override
	public void stop() {
		player.stopC64();
		// Eject media: Make it possible to auto-delete temporary files
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
		switch (configurationType) {
		case DATABASE:
			configService.persist((Configuration) player.getConfig());
			break;

		case XML:
		default:
			configService.exportCfg((Configuration) player.getConfig(), getConfigPath());
			break;
		}
		configService.close();
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
		Logger.getLogger("org.hibernate").setLevel(Level.SEVERE);
		switch (configurationType) {
		case DATABASE:
			em = Persistence
					.createEntityManagerFactory(PersistenceProperties.CONFIG_DS, new PersistenceProperties(
							PathUtils.getFilenameWithoutSuffix(getConfigPath().getAbsolutePath()), Database.HSQL_FILE))
					.createEntityManager();
			configService = new ConfigService(em);
			return configService.getOrCreate();

		case XML:
		default:
			em = Persistence.createEntityManagerFactory(PersistenceProperties.CONFIG_DS,
					new PersistenceProperties(CONFIG_FILE, Database.HSQL_MEM)).createEntityManager();
			configService = new ConfigService(em);
			return configService.importCfg(getConfigPath());
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
