package ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import sidplay.fingerprinting.FingerprintJsonClient;
import ui.common.Convenience;
import ui.common.util.DebugUtil;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.WhatsSidSection;
import ui.entities.config.service.ConfigService;
import ui.entities.config.service.ConfigService.ConfigurationType;

/**
 * 
 * Main class of the UI version of JSIDPlay2.
 * 
 * @author Ken Händel
 * @author Joakim Eriksson
 *
 *         SID Player main class
 */
@Parameters(resourceBundle = "ui.JSidPlay2Main")
public class JSidPlay2Main extends Application {

	static {
		DebugUtil.init();
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--configurationType", "-c" }, descriptionKey = "CONFIGURATION_TYPE")
	private ConfigurationType configurationType = ConfigurationType.XML;

	@Parameter(description = "filename")
	private List<String> filenames = new ArrayList<>();

	/**
	 * Main Window
	 */
	protected JSidPlay2 jSidplay2;

	/**
	 * Config service class.
	 */
	private ConfigService configService;

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

	@Override
	public void start(Stage primaryStage) {
		try {
			final Configuration configuration = getConfigurationFromCommandLineArgs();
			final SidPlay2Section sidplay2Section = configuration.getSidplay2Section();
			final WhatsSidSection whatsSidSection = configuration.getWhatsSidSection();

			String url = whatsSidSection.getUrl();
			String username = whatsSidSection.getUsername();
			String password = whatsSidSection.getPassword();
			int connectionTimeout = whatsSidSection.getConnectionTimeout();

			player = new Player(configuration);
			player.setMenuHook(menuHook);
			player.setFingerPrintMatcher(new FingerprintJsonClient(url, username, password, connectionTimeout));

			autostartFilenames();

			jSidplay2 = new JSidPlay2(primaryStage, player);

			Scene scene = primaryStage.getScene();
			if (scene != null) {
				Window window = scene.getWindow();

				window.setX(sidplay2Section.getFrameX());
				sidplay2Section.frameXProperty().bind(window.xProperty());

				window.setY(sidplay2Section.getFrameY());
				sidplay2Section.frameYProperty().bind(window.yProperty());

				window.setWidth(sidplay2Section.getFrameWidth());
				sidplay2Section.frameWidthProperty().bind(window.widthProperty());

				window.setHeight(sidplay2Section.getFrameHeight());
				sidplay2Section.frameHeightProperty().bind(window.heightProperty());

				jSidplay2.open();
			}
		} catch (Throwable t) {
			// Uncover unparsable view or other development errors
			t.printStackTrace();
		}
	}

	@Override
	public void stop() {
		player.stopC64();
		// Eject media: Make it possible to auto-delete temporary files
		for (final C1541 floppy : player.getFloppies()) {
			try {
				floppy.getDiskController().ejectDisk();
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
		}
		try {
			player.getDatasette().ejectTape();
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		configService.save((Configuration) player.getConfig());
		configService.close();
		System.exit(0);
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
				String[] args = parameters.getRaw().toArray(new String[0]);
				JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName())
						.build();
				commander.parse(args);
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

	private void autostartFilenames() {
		Optional<String> filename = filenames.stream().findFirst();
		if (filename.isPresent()) {
			try {
				new Convenience(player).autostart(new File(filename.get()), Convenience.LEXICALLY_FIRST_MEDIA, null);
			} catch (IOException | SidTuneError e) {
				System.err.println(e.getMessage());
			}
		}
	}

	/**
	 * Main method. Create an application frame and start emulation.
	 *
	 * @param args command line arguments
	 */
	public static void main(final String[] args) {
		launch(args);
	}

}
