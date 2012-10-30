package applet;

import static sidplay.ConsolePlayer.playerExit;
import static sidplay.ConsolePlayer.playerFast;
import static sidplay.ConsolePlayer.playerRestart;
import static sidplay.ini.intf.IConfig.REQUIRED_CONFIG_VERSION;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;
import javax.swing.JApplet;
import javax.swing.JOptionPane;

import libsidplay.Player;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidutils.PRG2TAP;
import libsidutils.zip.ZipEntryFileProxy;

import org.swixml.SwingEngine;

import sidplay.ConsolePlayer;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.ISidPlay2Section;
import applet.entities.PersistenceProperties;
import applet.entities.config.Configuration;
import applet.entities.config.service.ConfigService;
import applet.events.IGotoURL;
import applet.events.IInsertMedia;
import applet.events.IPlayTune;
import applet.events.IReplayTune;
import applet.events.IStopTune;
import applet.events.ITuneStateChanged;
import applet.events.Reset;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;

/**
 * @author Ken Händel
 * @author Joakim Eriksson
 * 
 *         SID Player main class
 */
public class JSIDPlay2 extends JApplet implements UIEventListener {

	/**
	 * Filename of the jsidplay2 configuration database.
	 */
	public static final String CONFIG_DATABASE = "JSIDPLAY2";

	/**
	 * Console player
	 */
	protected final ConsolePlayer cp;
	/**
	 * Console player thread.
	 */
	protected Thread fPlayerThread;

	/**
	 * Event management of UI events.
	 */
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	/**
	 * Main window user interface.
	 */
	protected JSIDPlay2UI ui;

	/**
	 * Database support.
	 */
	protected EntityManager em;

	/**
	 * Config service class.
	 */
	protected ConfigService configService;

	/**
	 * Applet constructor.
	 */
	public JSIDPlay2() {
		this(new String[0]);
	}

	/**
	 * Application constructor.
	 */
	public JSIDPlay2(final String[] args) {
		IConfig config = getConfiguration();
		initializeTmpDir(config);

		uiEvents.addListener(this);

		cp = new ConsolePlayer(config);
		if (args.length != 0) {
			cp.args(args);
		}
	}

	/**
	 * Player runnable to play music in the background.
	 */
	private transient final Runnable playerRunnable = new Runnable() {
		@Override
		public void run() {
			// Run until the player gets stopped
			while (true) {
				try {
					// Open tune and play
					if (!cp.open()) {
						return;
					}
					// Notify the views about the currently played tune
					ui.setTune(getPlayer().getTune());
					// Play next chunk of sound data, until it gets stopped
					while (true) {
						// Pause? sleep for awhile
						if (cp.getState() == ConsolePlayer.playerPaused) {
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
				if ((cp.getState() & ~playerFast) == playerRestart) {
					continue;
				}
				// Stop it
				break;

			}
			// Player has finished, play another favorite tune? Notify!
			uiEvents.fireEvent(ITuneStateChanged.class,
					new ITuneStateChanged() {
						@Override
						public File getTune() {
							return getPlayer().getTune().getInfo().file;
						}

						@Override
						public boolean naturalFinished() {
							return cp.getState() == playerExit;
						}

					});
		}

	};

	//
	// Applet methods
	//

	/**
	 * The user interface is set up, here.
	 * 
	 * @see java.applet.Applet#init()
	 */
	@Override
	public void init() {
		createUI();
	}

	/**
	 * Start the emulation.
	 * 
	 * @see java.applet.Applet#start()
	 */
	@Override
	public void start() {
		startC64();
	}

	/**
	 * Stop emulation.
	 * 
	 * @see java.applet.Applet#stop()
	 */
	@Override
	public void stop() {
		stopC64();
	}

	/**
	 * Free resources.
	 * 
	 * @see java.applet.Applet#destroy()
	 */
	@Override
	public void destroy() {
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
	private IConfig getConfiguration() {
		File dbFile = getDbPath();
		boolean dbFileExists = dbFile.exists();
		em = Persistence.createEntityManagerFactory(
				PersistenceProperties.CONFIG_DS,
				new PersistenceProperties(new File(dbFile.getParent(),
						CONFIG_DATABASE))).createEntityManager();
		configService = new ConfigService(em);
		if (!dbFileExists) {
			// No database found?
			return configService.create();
		} else {
			Configuration config = configService.get();
			if (config == null) {
				// No configuration in database found?
				return configService.create();
			}
			boolean shouldBeRestored = configService.shouldBeRestored(config);
			if (shouldBeRestored) {
				// import configuration (flagged by configuration viewer)
				config = configService.restore(config);
			}
			// Configuration version check
			if (config.getSidplay2().getVersion() != REQUIRED_CONFIG_VERSION) {
				// Wrong configuration version
				if (shouldBeRestored) {
					System.err.println("Imported configuration version "
							+ config.getSidplay2().getVersion()
							+ " is too old, expected version is "
							+ REQUIRED_CONFIG_VERSION + ": Ignored!");
				} else {
					System.err.println("Configuration version "
							+ config.getSidplay2().getVersion()
							+ " is too old, expected version is "
							+ REQUIRED_CONFIG_VERSION + ": Create a new one!");
					configService.remove(config);
					return configService.create();
				}
			}
			return config;
		}
	}

	/**
	 * Search for the database file (the players configuration). Search in CWD
	 * and in the HOME folder.
	 * 
	 * @return absolute path name of the database properties file
	 */
	private File getDbPath() {
		File configPlace = null;
		for (final String s : new String[] { System.getProperty("user.dir"),
				System.getProperty("user.home"), }) {
			configPlace = new File(s, CONFIG_DATABASE + ".properties");
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		return configPlace;
	}

	/**
	 * Create temp directory, if not exists (default is user home dir).
	 * 
	 * Note: system property jsidplay2.tmpdir is set accordingly.
	 * 
	 * @param config
	 */
	private void initializeTmpDir(IConfig config) {
		String tmpDirPath = config.getSidplay2().getTmpDir();
		File tmpDir = new File(tmpDirPath);
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
	}

	/**
	 * Create the user interface.
	 */
	private void createUI() {
		// Create main window
		ui = new JSIDPlay2UI(em, this, cp, getConfig());
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
				fPlayerThread.join(10000);
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
	 * @param file
	 *            file to play the tune (null means just reset C64)
	 */
	protected void playTune(final File file) {
		// Stop previous run
		stopC64();
		// Load tune
		cp.loadTune(file);
		if (file != null) {
			System.out.println("Play File: <" + file.getAbsolutePath() + ">");
		}
		// Start emulation
		startC64();
	}

	/**
	 * Insert a tape.
	 */
	private void insertTape(final File selectedTape, final File autostartFile,
			final Component component) throws IOException {
		if (!selectedTape.getName().toLowerCase().endsWith(".tap")) {
			// Everything, which is not a tape convert to tape first
			final File convertedTape = new File(getConfig().getSidplay2()
					.getTmpDir(), selectedTape.getName() + ".tap");
			convertedTape.deleteOnExit();
			String[] args = new String[] { selectedTape.getAbsolutePath(),
					convertedTape.getAbsolutePath() };
			PRG2TAP.main(args);
			getPlayer().getDatasette().insertTape(convertedTape);
		} else {
			getPlayer().getDatasette().insertTape(selectedTape);
		}
		if (autostartFile != null) {
			uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public boolean switchToVideoTab() {
					return true;
				}

				@Override
				public File getFile() {
					return autostartFile;
				}

				@Override
				public Component getComponent() {
					return component;
				}
			});
		}
	}

	/**
	 * Insert a disk.
	 */
	private void insertDisk(final File selectedDisk, final File autostartFile,
			final Component component) throws IOException {
		// automatically turn drive on
		getPlayer().enableFloppyDiskDrives(true);
		getConfig().getC1541().setDriveOn(true);
		// attach selected disk into the first disk drive
		DiskImage disk = getPlayer().getFloppies()[0].getDiskController()
				.insertDisk(selectedDisk);
		disk.setExtendImagePolicy(new IExtendImageListener() {

			@Override
			public boolean isAllowed() {
				if (getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
					// EXTEND_ASK
					return JOptionPane.YES_OPTION == JOptionPane
							.showConfirmDialog(
									JSIDPlay2.this,
									ui.getSwix()
											.getLocalizer()
											.getString(
													"EXTEND_DISK_IMAGE_TO_40_TRACKS"),
									ui.getSwix().getLocalizer()
											.getString("EXTEND_DISK_IMAGE"),
									JOptionPane.YES_NO_OPTION);
				} else if (getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
					// EXTEND_ACCESS
					return true;
				} else {
					// EXTEND_NEVER
					return false;
				}
			}
		});
		if (autostartFile != null) {
			uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public boolean switchToVideoTab() {
					return true;
				}

				@Override
				public File getFile() {
					return autostartFile;
				}

				@Override
				public Component getComponent() {
					return component;
				}
			});
		}
	}

	/**
	 * Insert a cartridge.
	 * 
	 * @throws IOException
	 *             cannot read cartridge file
	 */
	private void insertCartridge(final File selectedFile) throws IOException {
		// Insert a cartridge
		getPlayer().getC64().insertCartridge(selectedFile);
		// reset required after inserting the cartridge
		uiEvents.fireEvent(Reset.class, new Reset() {

			@Override
			public boolean switchToVideoTab() {
				return false;
			}

			@Override
			public String getCommand() {
				return null;
			}

			@Override
			public Component getComponent() {
				return JSIDPlay2.this;
			}
		});
	}

	/**
	 * Main method. Create an application frame and start emulation.
	 * 
	 * @param args
	 *            command line arguments
	 */
	public static void main(final String[] args) {
		final JSIDPlay2 sidplayApplet = new JSIDPlay2(args);
		// Create UI
		sidplayApplet.init();

		// Create application frame
		try {
			SwingEngine swix = new SwingEngine(sidplayApplet);
			final Window window = (Window) swix.render(JSIDPlay2.class
					.getResource("JSIDPlay2.xml"));
			window.add(sidplayApplet);

			// Set default position and size
			final ISidPlay2Section section = sidplayApplet.getConfig()
					.getSidplay2();
			// Restore saved coordinates
			window.setLocation(section.getFrameX(), section.getFrameY());
			window.setSize(section.getFrameWidth(), section.getFrameHeight());

			// Handle close button: execute applet's stop()/destroy()
			window.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(final WindowEvent e) {
					// Stop emulation
					sidplayApplet.stop();
					// Free resources
					sidplayApplet.destroy();
				}
			});

			// Handle move/resize: record new window position/size
			window.addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(final ComponentEvent e) {
					// Store window dimensions, if resized
					final Dimension size = window.getSize();
					sidplayApplet.getConfig().getSidplay2()
							.setFrameWidth(size.width);
					sidplayApplet.getConfig().getSidplay2()
							.setFrameHeight(size.height);
				}

				@Override
				public void componentMoved(final ComponentEvent e) {
					// Store window location, if moved
					final Point loc = window.getLocation();
					sidplayApplet.getConfig().getSidplay2().setFrameX(loc.x);
					sidplayApplet.getConfig().getSidplay2().setFrameY(loc.y);
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		/* Start emulation */
		sidplayApplet.start();
	}

	/**
	 * Capture events to do certain tasks. Play tune, open browser, etc.
	 * 
	 * @param evt
	 *            property change event
	 */
	@Override
	public void notify(final UIEvent evt) {
		if (evt.isOfType(IReplayTune.class)) {
			// Replay (restart) a tune
			if (getPlayer().getTune() != null) {
				playTune(getPlayer().getTune().getInfo().file);
			} else {
				playTune(null);
			}
		} else if (evt.isOfType(IPlayTune.class)) {
			// Play a tune
			IPlayTune ifObj = (IPlayTune) evt.getUIEventImpl();
			if (evt.isOfType(Reset.class)) {
				getPlayer().setCommand(
						((Reset) evt.getUIEventImpl()).getCommand());
			}
			playTune(ifObj.getFile());
		} else if (evt.isOfType(IGotoURL.class)) {
			// Open a browser URL
			IGotoURL ifObj = (IGotoURL) evt.getUIEventImpl();
			if (isActive()) {
				// Use applet context to open the URL
				getAppletContext().showDocument(ifObj.getCollectionURL(),
						"_blank");
			} else {
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
		} else if (evt.isOfType(IStopTune.class)) {
			// Stop C64
			stopC64();
		} else if (evt.isOfType(IInsertMedia.class)) {
			// Insert a disk/tape or cartridge
			IInsertMedia ifObj = (IInsertMedia) evt.getUIEventImpl();
			File mediaFile = ifObj.getSelectedMedia();
			try {
				if (mediaFile instanceof ZipEntryFileProxy) {
					// Extract ZIP file
					ZipEntryFileProxy zipEntryFileProxy = (ZipEntryFileProxy) mediaFile;
					mediaFile = ZipEntryFileProxy.extractFromZip(
							(ZipEntryFileProxy) mediaFile, getConfig()
									.getSidplay2().getTmpDir());
					getConfig().getSidplay2().setLastDirectory(
							zipEntryFileProxy.getZip().getAbsolutePath());
				} else {
					getConfig().getSidplay2().setLastDirectory(
							mediaFile.getParentFile().getAbsolutePath());
				}
				if (mediaFile.getName().endsWith(".gz")) {
					// Extract GZ file
					mediaFile = ZipEntryFileProxy.extractFromGZ(mediaFile,
							getConfig().getSidplay2().getTmpDir());
				}
				switch (ifObj.getMediaType()) {
				case TAPE:
					insertTape(mediaFile, ifObj.getAutostartFile(),
							ifObj.getComponent());
					break;

				case DISK:
					insertDisk(mediaFile, ifObj.getAutostartFile(),
							ifObj.getComponent());
					break;

				case CART:
					insertCartridge(mediaFile);
					break;

				default:
					break;
				}
			} catch (IOException e) {
				System.err.println(String.format("Cannot attach file '%s'.",
						mediaFile.getAbsolutePath()));
				return;
			}
		}
	}

	/**
	 * Get saved INI file configuration.
	 * 
	 * @return INI file configuration
	 */
	public IConfig getConfig() {
		return cp.getConfig();
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
