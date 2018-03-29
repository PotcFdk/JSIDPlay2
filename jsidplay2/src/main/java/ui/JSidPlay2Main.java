package ui;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
import sidplay.Player;
import ui.common.Convenience;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

/**
 * @author Ken Händel
 * @author Joakim Eriksson
 * 
 *         SID Player main class
 */
@Parameters(resourceBundle = "ui.JSidPlay2Main")
public class JSidPlay2Main extends Application {

	private static final String LOG_CONFIG_RES = "/sidplay/logconfig.properties";

	static {
		try {
			// turn off HSQL logging re-configuration
			System.setProperty("hsqldb.reconfig_logging", "false");
			// configure JSIDPlay2 logging
			LogManager.getLogManager().readConfiguration(JSidPlay2Main.class.getResourceAsStream(LOG_CONFIG_RES));
		} catch (final IOException e) {
			Logger.getAnonymousLogger().severe("Could not load " + LOG_CONFIG_RES + ": " + e.getMessage());
		}
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--configurationType", "-c" }, descriptionKey = "CONFIGURATION_TYPE")
	private ConfigurationType configurationType = ConfigurationType.XML;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<String>();

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
	 * Config service class.
	 */
	private ConfigService configService;

	private static JSidPlay2 testInstance;

	@Override
	public void start(Stage primaryStage) {
		testInstance = null;
		player = new Player(getConfigurationFromCommandLineArgs());
		player.setMenuHook(menuHook);
		// automatically load tune on start-up
		Optional<String> filename = filenames.stream().findFirst();
		if (filename.isPresent()) {
			try {
				new Convenience(player).autostart(new File(filename.get()), Convenience.LEXICALLY_FIRST_MEDIA, null);
			} catch (IOException | SidTuneError | URISyntaxException e) {
				System.err.println(e.getMessage());
			}
		}
		final JSidPlay2 jSidplay2 = new JSidPlay2(primaryStage, player);
		// Set default position and size
		final SidPlay2Section section = (SidPlay2Section) player.getConfig().getSidplay2Section();
		primaryStage.setFullScreen(Boolean.TRUE.equals(section.getFullScreen()));
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
		configService.save((Configuration) player.getConfig());
		configService.close();
	}

	//
	// Helper methods
	//

	/**
	 * Parse optional command line arguments.
	 * 
	 * @return configuration database chosen by command line arguments
	 */
	private Configuration getConfigurationFromCommandLineArgs() {
		try {
			Parameters parameters = getParameters();
			if (parameters != null) {
				JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName())
						.build();
				commander.parse(parameters.getRaw().toArray(new String[0]));
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

	/**
	 * Get the players configuration, create a new one, if absent.
	 * 
	 * @return the players configuration to be used
	 */
	private Configuration getConfiguration() {
		configService = new ConfigService(configurationType);
		return configService.load();
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
