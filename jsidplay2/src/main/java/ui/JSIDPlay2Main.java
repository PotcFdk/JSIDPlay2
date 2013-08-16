package ui;

import static sidplay.ConsolePlayer.playerFast;
import static sidplay.ConsolePlayer.playerRestart;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import libsidplay.Player;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import sidplay.ConsolePlayer;
import sidplay.ini.intf.ISidPlay2Section;
import ui.entities.PersistenceUtil;
import ui.entities.config.Configuration;
import ui.entities.config.service.ConfigService;
import ui.events.IGotoURL;
import ui.events.IPlayTune;
import ui.events.UIEvent;
import ui.events.UIEventFactory;
import ui.events.UIEventListener;

/**
 * @author Ken Händel
 * @author Joakim Eriksson
 * 
 *         SID Player main class
 */
public class JSIDPlay2Main extends Application implements UIEventListener {

	/**
	 * Filename of the jsidplay2 configuration database.
	 */
	public static final String CONFIG_DATABASE = "JSIDPLAY2";

	/**
	 * Console player
	 */
	protected ConsolePlayer cp;
	/**
	 * Current tune to play.
	 */
	protected SidTune sidTune;
	/**
	 * Configuration
	 */
	private Configuration config;
	/**
	 * Console player thread.
	 */
	protected Thread fPlayerThread;

	/**
	 * Event management of UI events.
	 */
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	/**
	 * Database support.
	 */
	protected EntityManager em;

	/**
	 * Config service class.
	 */
	protected ConfigService configService;

	private JSidPlay2 jSidplay2;

	/**
	 * Player runnable to play music in the background.
	 */
	private transient final Runnable playerRunnable = new Runnable() {
		@Override
		public void run() {
			// Load tune
			cp.loadTune(sidTune);
			if (sidTune != null && sidTune.getInfo().file != null) {
				System.out.println("Play File: <"
						+ sidTune.getInfo().file.getAbsolutePath() + ">");
			}
			// Run until the player gets stopped
			while (true) {
				try {
					// Open tune and play
					if (!cp.open()) {
						return;
					}
					// Play next chunk of sound data, until it gets stopped
					while (true) {
						// Pause? sleep for awhile
						if (cp.getState().get() == ConsolePlayer.playerPaused) {
							Thread.sleep(250);
						}
						// Play a chunk
						if (!cp.play()) {
							break;
						}
					}
				} catch (InterruptedException e) {
				} finally {
					// Don't forget to close
					cp.close();
				}

				// "Play it once, Sam. For old times' sake."
				if ((cp.getState().get() & ~playerFast) == playerRestart) {
					continue;
				}
				// Stop it
				break;

			}
		}

	};

	@Override
	public void start(Stage primaryStage) {
		config = getConfiguration();
		initializeTmpDir(config);

		uiEvents.addListener(this);

		cp = new ConsolePlayer(config);
		String[] args = getParameters().getRaw().toArray(new String[0]);
		if (args.length != 0) {
			cp.args(args);
		}

		jSidplay2 = new JSidPlay2();
		jSidplay2.setConsolePlayer(cp);
		jSidplay2.setPlayer(getPlayer());
		jSidplay2.setConfig(getConfig());
		try {
			jSidplay2.open(primaryStage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Set default position and size
		final ISidPlay2Section section = getConfig().getSidplay2();
		Scene scene = primaryStage.getScene();
		if (scene != null) {
			Window window = scene.getWindow();
			window.setX(section.getFrameX());
			window.setY(section.getFrameY());
			window.setWidth(section.getFrameWidth());
			window.setHeight(section.getFrameHeight());
			window.widthProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(
						ObservableValue<? extends Number> observable,
						Number oldValue, Number newValue) {
					getConfig().getSidplay2()
							.setFrameWidth(newValue.intValue());
				}
			});
			window.heightProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(
						ObservableValue<? extends Number> observable,
						Number oldValue, Number newValue) {
					getConfig().getSidplay2().setFrameHeight(
							newValue.intValue());
				}
			});
			window.xProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(
						ObservableValue<? extends Number> observable,
						Number oldValue, Number newValue) {
					getConfig().getSidplay2().setFrameX(newValue.intValue());
				}
			});
			window.yProperty().addListener(new ChangeListener<Number>() {

				@Override
				public void changed(
						ObservableValue<? extends Number> observable,
						Number oldValue, Number newValue) {
					getConfig().getSidplay2().setFrameY(newValue.intValue());
				}
			});
			startC64();
		}
	}

	@Override
	public void stop() {
		stopC64();
		// Eject medias: Make it possible to auto-delete temporary files
		for (final C1541 floppy : getPlayer().getFloppies()) {
			try {
				floppy.getDiskController().ejectDisk();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			getPlayer().getDatasette().ejectTape();
		} catch (IOException e) {
			e.printStackTrace();
		}
		configService.write(getConfig());
		em.close();
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
					PersistenceUtil.CONFIG_DS,
					new PersistenceUtil(getConfigDatabasePath()))
					.createEntityManager();
			configService = new ConfigService(em);
			// Import configuration (flagged by configuration viewer)
			Configuration config = configService.getOrCreate();
			if (configService.shouldBeRestored(config)) {
				System.out.println("Import Configuration!");
				config = configService.restore(config);
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
			System.err.println(e.getMessage());
			// fatal database error? Delete it for the next startup!
			PersistenceUtil.databaseDeleteOnExit(getConfigDatabasePath());
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
	 * Start emulation (start player thread).
	 */
	private void startC64() {
		fPlayerThread = new Thread(playerRunnable);
		fPlayerThread.setPriority(Thread.MAX_PRIORITY);
		fPlayerThread.start();
	}

	/**
	 * Stop emulation (stop player thread).
	 */
	private void stopC64() {
		try {
			while (fPlayerThread.isAlive()) {
				cp.quit();
				fPlayerThread.join(3000);
				// This is only the last option, if the player can not be
				// stopped clean
				fPlayerThread.interrupt();
			}
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Play tune.
	 * 
	 * @param sidTune
	 *            file to play the tune (null means just reset C64)
	 */
	private void playTune(final SidTune sidTune) {
		// Stop previous run
		stopC64();
		// Set tune to play
		this.sidTune = sidTune;
		// Start emulation
		startC64();
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

	/**
	 * Capture events to do certain tasks. Play tune, open browser, etc.
	 * 
	 * @param evt
	 *            property change event
	 */
	@Override
	public void notify(final UIEvent evt) {
		if (evt.isOfType(IPlayTune.class)) {
			// Play a tune
			IPlayTune ifObj = (IPlayTune) evt.getUIEventImpl();
			playTune(ifObj.getSidTune());
		} else if (evt.isOfType(IGotoURL.class)) {
			// Open a browser URL
			IGotoURL ifObj = (IGotoURL) evt.getUIEventImpl();

			// As an application we open the default browser
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					try {
						desktop.browse(ifObj.getCollectionURL().toURI());
					} catch (final IOException ioe) {
						ioe.printStackTrace();
					} catch (final URISyntaxException urie) {
						urie.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Get saved INI file configuration.
	 * 
	 * @return INI file configuration
	 */
	public Configuration getConfig() {
		return config;
	}

	/**
	 * Get player (C64 and peripherals).
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return cp.getPlayer();
	}

}
