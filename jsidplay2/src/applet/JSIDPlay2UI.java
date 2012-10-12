package applet;

import java.applet.Applet;
import java.applet.AudioClip;
import java.awt.AWTException;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.EntityManager;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.UIManager;

import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.ISID2Types.Clock;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.sidtune.SidTune;
import libsidutils.SidDatabase;

import org.swixml.SwingEngine;

import sidplay.ConsolePlayer;
import sidplay.ini.intf.IConfig;
import applet.collection.Collection;
import applet.console.ConsoleView;
import applet.demos.DiskCollection;
import applet.disassembler.Disassembler;
import applet.emulationsettings.EmulationSettings;
import applet.events.IInsertMedia;
import applet.events.IMadeProgress;
import applet.events.IPlayTune;
import applet.events.ITuneStateChanged;
import applet.events.IUpdateUI;
import applet.events.Reset;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;
import applet.favorites.Favorites;
import applet.filechooser.ImageFileChooser;
import applet.filefilter.CartFileFilter;
import applet.filefilter.DiskFileFilter;
import applet.filefilter.RomFileFilter;
import applet.filefilter.TapeFileFilter;
import applet.filefilter.TuneFileFilter;
import applet.gamebase.GameBase;
import applet.joysticksettings.JoystickSettings;
import applet.oscilloscope.Oscilloscope;
import applet.printer.PrinterView;
import applet.siddump.SIDDump;
import applet.sidreg.SidReg;
import applet.soundsettings.SoundSettings;
import applet.videoscreen.VideoScreen;
import ch.randelshofer.screenrecorder.ScreenRecorder;

public class JSIDPlay2UI implements UIEventListener {

	/** Build date calculated from our own modify time */
	private static String DATE = "unknown";
	static {
		// Workaround a swixml bug for turkish (tr): default language is english
		if (!Locale.getDefault().getLanguage()
				.equals(Locale.GERMAN.getLanguage())
				&& !Locale.getDefault().getLanguage()
						.equals(Locale.ENGLISH.getLanguage())) {
			Locale.setDefault(Locale.ENGLISH);
		}
		try {
			URL us = JSIDPlay2.class.getResource("/applet/JSIDPlay2.class");
			Date date = new Date(us.openConnection().getLastModified());
			DATE = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
		} catch (IOException e) {
		}
	}

	/** Icon for indicating the playback source */
	private static final ImageIcon PLAY_ICON = new ImageIcon(
			JSIDPlay2.class.getResource("/applet/icons/play.png"));

	/**
	 * Event management of UI events.
	 */
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	/** C1541 track switch sound */
	private static final AudioClip trackSound = Applet
			.newAudioClip(JSIDPlay2.class
					.getResource("/applet/sounds/track.wav"));

	/** C1541 spinning floppy sound */
	private static final AudioClip motorSound = Applet
			.newAudioClip(JSIDPlay2.class
					.getResource("/applet/sounds/motor.wav"));

	/**
	 * XML based Swing support
	 */
	private SwingEngine swix;

	protected JMenuItem previous, next;
	protected JRadioButtonMenuItem ntsc, pal, c1541, c1541_II, neverExtend,
			askExtend, accessExtend;
	protected JCheckBoxMenuItem pauseContinue, normalSpeed, fastForward,
			driveOn, driveSoundOn, parCable, expand2000, expand4000,
			expand6000, expand8000, expandA000, turnPrinterOn;
	protected JToggleButton pauseContinue2;
	protected JButton previous2, next2;
	protected JTabbedPane tabbedPane;
	protected JLabel status;
	protected JProgressBar progress;
	protected Container parent;
	protected VideoScreen videoScreen;

	/**
	 * Console player
	 */
	protected ConsolePlayer cp;
	/**
	 * Old disk motor state.
	 */
	protected boolean oldDiskMotor;
	/**
	 * Last state of the Floppy track.
	 */
	protected int oldHalfTrack;
	/**
	 * Capture video.
	 */
	protected ScreenRecorder screenRecorder;

	public JSIDPlay2UI(final EntityManager em, final Container container,
			final ConsolePlayer player) {
		this.parent = container;
		this.cp = player;
		uiEvents.addListener(this);
		// create GUI
		try {
			// Use system L&F
			try {
				UIManager.setLookAndFeel(UIManager
						.getSystemLookAndFeelClassName());
			} catch (final Exception e) {
				e.printStackTrace();
			}
			// Render UI
			swix = new SwingEngine(this);
			swix.insert(JSIDPlay2UI.class.getResource("JSIDPlay2UI.xml"),
					parent);

			// Create tabs
			videoScreen = new VideoScreen(getPlayer(), getConfig());
			tabbedPane.add(swix.getLocalizer().getString("VIDEO"), videoScreen);
			tabbedPane.add(swix.getLocalizer().getString("OSCILLOSCOPE"),
					new Oscilloscope(getPlayer()));
			Collection.HVSC hvsc = new Collection.HVSC(getPlayer(), getConfig());
			tabbedPane.add(swix.getLocalizer().getString("HVSC"), hvsc);
			Collection.CGSC cgsc = new Collection.CGSC(getPlayer(), getConfig());
			tabbedPane.add(swix.getLocalizer().getString("CGSC"), cgsc);
			tabbedPane.add(swix.getLocalizer().getString("HVMEC"),
					new DiskCollection.HVMEC(getPlayer(), getConfig()));
			tabbedPane.add(swix.getLocalizer().getString("DEMOS"),
					new DiskCollection.Demos(getPlayer(), getConfig()));
			tabbedPane.add(swix.getLocalizer().getString("MAGS"),
					new DiskCollection.Mags(getPlayer(), getConfig()));
			tabbedPane.add(swix.getLocalizer().getString("GAMEBASE"),
					new GameBase(getPlayer(), getConfig()));
			tabbedPane.add(swix.getLocalizer().getString("FAVORITES"),
					new Favorites(em, getPlayer(), getConfig(), hvsc, cgsc));
			tabbedPane.add(swix.getLocalizer().getString("PRINTER"),
					new PrinterView(getPlayer()));
			tabbedPane.add(swix.getLocalizer().getString("CONSOLE"),
					new ConsoleView());

			setDefaultsAndActions();

			new Timer(100, new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent arg0) {
					uiEvents.fireEvent(IUpdateUI.class, new IUpdateUI() {
					});
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	protected void setStatusLine() {
		// Get status information of the first disk drive
		final C1541 c1541 = getPlayer().getFloppies()[0];
		// Disk motor status
		boolean motor = getConfig().getC1541().isDriveSoundOn()
				&& cp.getState() == ConsolePlayer.playerRunning
				&& c1541.getDiskController().isMotorOn();
		if (!oldDiskMotor && motor) {
			motorSound.loop();
		} else if (oldDiskMotor && !motor) {
			motorSound.stop();
		}
		oldDiskMotor = motor;
		// Read/Write head position (half tracks)
		final int halfTrack = c1541.getDiskController().getHalfTrack();
		if (oldHalfTrack != halfTrack && motor) {
			trackSound.play();
		}
		oldHalfTrack = halfTrack;
		// Get status information of the datasette
		final Datasette datasette = getPlayer().getDatasette();
		// Datasette tape progress
		if (datasette.getMotor()) {
			progress.setValue(datasette.getProgress());
		}
		// Current play time / well-known song length
		String statusTime = String.format("%02d:%02d",
				getPlayer().time() / 60 % 100, getPlayer().time() % 60);
		String statusSongLength = "";
		if (getPlayer().getTune() != null) {
			final int songLength = getSongLength(getPlayer().getTune());
			// song length well-known?
			if (songLength > 0) {
				statusSongLength = String.format("/%02d:%02d",
						(songLength / 60 % 100), (songLength % 60));
			}
		}
		// Memory usage
		Runtime runtime = Runtime.getRuntime();
		int totalMemory = (int) (runtime.totalMemory() / (1 << 20));
		int freeMemory = (int) (runtime.freeMemory() / (1 << 20));
		// final status bar text
		String text = String.format(
				swix.getLocalizer().getString("DATASETTE_COUNTER") + " %03d, "
						+ swix.getLocalizer().getString("FLOPPY_TRACK")
						+ " %02d, " + swix.getLocalizer().getString("DATE")
						+ " %s, " + swix.getLocalizer().getString("TIME")
						+ " %s%s, " + swix.getLocalizer().getString("MEM")
						+ " %d/%d MB", datasette.getCounter(),
				oldHalfTrack >> 1, DATE, statusTime, statusSongLength,
				(totalMemory - freeMemory), totalMemory);
		status.setText(text);
	}

	/**
	 * Get song length.
	 * 
	 * @param sidTune
	 *            tune to get song length for
	 * @return song length in seconds (0 means unknown, -1 means unconfigured)
	 */
	private int getSongLength(final SidTune sidTune) {
		SidDatabase database = SidDatabase.getInstance(getConfig()
				.getSidplay2().getHvsc());
		if (database != null) {
			return database.length(sidTune);
		}
		return -1;
	}

	private void setDefaultsAndActions() {
		normalSpeed.setSelected(true);
		Clock defClk = getConfig().getEmulation().getDefaultClockSpeed();
		switch (defClk) {
		case NTSC:
			ntsc.setSelected(true);
			break;

		default:
			// PAL
			pal.setSelected(true);
			break;
		}
		driveOn.setSelected(getConfig().getC1541().isDriveOn());
		driveSoundOn.setSelected(getConfig().getC1541().isDriveSoundOn());
		parCable.setSelected(getConfig().getC1541().isParallelCable());
		switch (getConfig().getC1541().getFloppyType()) {
		case C1541:
			c1541.setSelected(true);
			break;

		default:
			// C1541 II
			c1541_II.setSelected(true);
			break;
		}
		switch (getConfig().getC1541().getExtendImagePolicy()) {
		case EXTEND_NEVER:
			neverExtend.setSelected(true);
			break;

		case EXTEND_ASK:
			askExtend.setSelected(true);
			break;

		case EXTEND_ACCESS:
			accessExtend.setSelected(true);
			break;

		default:
			break;
		}
		expand2000.setSelected(getConfig().getC1541().isRamExpansionEnabled0());
		expand4000.setSelected(getConfig().getC1541().isRamExpansionEnabled1());
		expand6000.setSelected(getConfig().getC1541().isRamExpansionEnabled2());
		expand8000.setSelected(getConfig().getC1541().isRamExpansionEnabled3());
		expandA000.setSelected(getConfig().getC1541().isRamExpansionEnabled4());
		turnPrinterOn.setSelected(getConfig().getPrinter().isPrinterOn());
	}

	//
	// Actions
	//

	public Action load = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new ImageFileChooser(getConfig(),
					new TuneFileFilter());
			fileDialog.setName("Open");
			final int rc = fileDialog.showDialog(parent, null);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				// play file
				uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {
					@Override
					public boolean switchToVideoTab() {
						return false;
					}

					@Override
					public File getFile() {
						return fileDialog.getSelectedFile();
					}

					@Override
					public Component getComponent() {
						return parent;
					}
				});
			}
		}
	};

	public Action video = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new ImageFileChooser(getConfig(),
					new CartFileFilter());
			final int rc = fileDialog.showDialog(parent, null);
			try {
				if (rc == JFileChooser.APPROVE_OPTION
						&& fileDialog.getSelectedFile() != null) {
					getPlayer().getC64().insertCartridge(
							fileDialog.getSelectedFile());
					final File tmpFile = new File(
							System.getProperty("jsidplay2.tmpdir"),
							"nuvieplayer-v1.0.prg");
					tmpFile.deleteOnExit();
					InputStream is = JSIDPlay2.class.getClassLoader()
							.getResourceAsStream(
									"libsidplay/mem/nuvieplayer-v1.0.prg");
					OutputStream os = null;
					try {
						os = new FileOutputStream(tmpFile);
						byte[] b = new byte[1024];
						while (is.available() > 0) {
							int len = is.read(b);
							if (len > 0) {
								os.write(b, 0, len);
							}
						}
					} finally {
						if (is != null) {
							is.close();
						}
						if (os != null) {
							os.close();
						}
					}
					// play file
					uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {
						@Override
						public boolean switchToVideoTab() {
							return false;
						}

						@Override
						public File getFile() {
							return tmpFile;
						}

						@Override
						public Component getComponent() {
							return parent;
						}
					});
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action reset = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			reset();
		}
	};

	public Action quit = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			System.exit(0);
		}
	};

	public Action pause = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.pause();
			// Keep both pause buttons in sync
			AbstractButton btn = (AbstractButton) e.getSource();
			pauseContinue.setSelected(btn.isSelected());
			pauseContinue2.setSelected(btn.isSelected());
		}
	};

	public Action previousSong = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.previousSong();
		}
	};

	public Action nextSong = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.nextSong();
		}
	};

	public Action playNormalSpeed = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.normalSpeed();
		}
	};

	public Action playFastForward = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.fastForward();
		}
	};

	public Action stopSong = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			cp.quit();
		}
	};

	public Action videoStandardPal = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getConfig().getEmulation().setDefaultClockSpeed(Clock.PAL);
			reset();
		}
	};

	public Action videoStandardNtsc = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getConfig().getEmulation().setDefaultClockSpeed(Clock.NTSC);
			reset();
		}
	};

	public Action soundSettings = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new SoundSettings(cp, getPlayer(), getConfig());
		}
	};

	public Action emulationSettings = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new EmulationSettings(cp, getPlayer(), getConfig());
		}
	};

	public Action joystickSettings = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new JoystickSettings(getPlayer(), getConfig());
		}
	};

	public Action record = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.RECORD);
		}
	};

	public Action play = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.START);
		}
	};

	public Action rewind = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.REWIND);
		}
	};

	public Action forward = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.FORWARD);
		}
	};

	public Action stop = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.STOP);
		}
	};

	public Action resetCounter = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getDatasette().control(Datasette.Control.RESET_COUNTER);
		}
	};

	public Action insertTape = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			// Choose tape file
			final ImageFileChooser fileDialog = new ImageFileChooser(
					getConfig(), new TapeFileFilter());
			final int rc = fileDialog.showDialog(parent, getSwix()
					.getLocalizer().getString("ATTACH_TAPE"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				uiEvents.fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.TAPE;
					}

					@Override
					public File getSelectedMedia() {
						return fileDialog.getSelectedFile();
					}

					@Override
					public File getAutostartFile() {
						return fileDialog.getAutostartFile();
					}

					@Override
					public Component getComponent() {
						return parent;
					}
				});
			}
		}
	};

	public Action ejectTape = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getDatasette().ejectTape();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Action turnDriveOn = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().enableFloppyDiskDrives(source.isSelected());
			getConfig().getC1541().setDriveOn(source.isSelected());
		}
	};

	public Action driveSound = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getConfig().getC1541().setDriveSoundOn(source.isSelected());
		}
	};

	public Action parallelCable = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer()
					.connectC64AndC1541WithParallelCable(source.isSelected());
			getConfig().getC1541().setParallelCable(source.isSelected());
		}
	};

	public Action floppyTypeC1541 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getFloppies()[0].setFloppyType(FloppyType.C1541);
			getConfig().getC1541().setFloppyType(FloppyType.C1541);
		}
	};

	public Action floppyTypeC1541_II = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getFloppies()[0].setFloppyType(FloppyType.C1541_II);
			getConfig().getC1541().setFloppyType(FloppyType.C1541_II);
		}
	};

	public Action extendNever = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getConfig().getC1541().setExtendImagePolicy(
					ExtendImagePolicy.EXTEND_NEVER);
		}
	};

	public Action extendAsk = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getConfig().getC1541().setExtendImagePolicy(
					ExtendImagePolicy.EXTEND_ASK);
		}
	};

	public Action extendAccess = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getConfig().getC1541().setExtendImagePolicy(
					ExtendImagePolicy.EXTEND_ACCESS);
		}
	};

	public Action expansion0x2000 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().getFloppies()[0]
					.setRamExpansion(0, source.isSelected());
			getConfig().getC1541().setRamExpansion0(source.isSelected());
		}
	};

	public Action expansion0x4000 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().getFloppies()[0]
					.setRamExpansion(1, source.isSelected());
			getConfig().getC1541().setRamExpansion1(source.isSelected());
		}
	};

	public Action expansion0x6000 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().getFloppies()[0]
					.setRamExpansion(2, source.isSelected());
			getConfig().getC1541().setRamExpansion2(source.isSelected());
		}
	};

	public Action expansion0x8000 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().getFloppies()[0]
					.setRamExpansion(3, source.isSelected());
			getConfig().getC1541().setRamExpansion3(source.isSelected());
		}
	};

	public Action expansion0xA000 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().getFloppies()[0]
					.setRamExpansion(4, source.isSelected());
			getConfig().getC1541().setRamExpansion4(source.isSelected());
		}
	};

	public Action insertDisk = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final ImageFileChooser fileDialog = new ImageFileChooser(
					getConfig(), new DiskFileFilter());
			fileDialog.setName("InsertDisk"); // For the test framework!
			final int rc = fileDialog.showDialog(parent, getSwix()
					.getLocalizer().getString("ATTACH_DISK"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				// attach disk
				uiEvents.fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.DISK;
					}

					@Override
					public File getSelectedMedia() {
						return fileDialog.getSelectedFile();
					}

					@Override
					public File getAutostartFile() {
						return fileDialog.getAutostartFile();
					}

					@Override
					public Component getComponent() {
						return parent;
					}
				});
			}
		}
	};

	public Action ejectDisk = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getFloppies()[0].getDiskController().ejectDisk();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Action resetDrive = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getFloppies()[0].reset();
		}
	};

	public Action printer = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JCheckBoxMenuItem source = (JCheckBoxMenuItem) e.getSource();
			getPlayer().turnPrinterOnOff(source.isSelected());
			getConfig().getPrinter().setPrinterOn(source.isSelected());
		}
	};

	public Action insertCartridge = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new ImageFileChooser(getConfig(),
					new CartFileFilter());
			final int rc = fileDialog.showDialog(parent, getSwix()
					.getLocalizer().getString("ATTACH_CART"));
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				uiEvents.fireEvent(IInsertMedia.class, new IInsertMedia() {

					@Override
					public MediaType getMediaType() {
						return MediaType.CART;
					}

					@Override
					public File getSelectedMedia() {
						return fileDialog.getSelectedFile();
					}

					@Override
					public File getAutostartFile() {
						return null;
					}

					@Override
					public Component getComponent() {
						return parent;
					}
				});
			}
		}
	};

	public Action insertGeoRAM = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final JFileChooser fileDialog = new JFileChooser(getConfig()
					.getSidplay2().getLastDirectory());
			fileDialog.setFileFilter(new CartFileFilter());
			final int rc = fileDialog.showOpenDialog(parent);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				getConfig().getSidplay2().setLastDirectory(
						fileDialog.getSelectedFile().getParentFile()
								.getAbsolutePath());
				try {
					getPlayer().getC64().insertRAMExpansion(
							C64.RAMExpansion.GEORAM,
							fileDialog.getSelectedFile());
					reset();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	};

	public Action insertGeoRAM64 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 64);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertGeoRAM128 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 128);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertGeoRAM256 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 256);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertGeoRAM512 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 512);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertGeoRAM1024 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 1024);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertREU128 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU,
						128);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertREU256 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU,
						256);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertREU512 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU,
						512);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertREU2048 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU,
						2048);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertREU16384 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU,
						16384);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action installJiffyDos = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				final JFileChooser c64FileDialog = new JFileChooser(getConfig()
						.getSidplay2().getLastDirectory());
				c64FileDialog.setDialogTitle(getSwix().getLocalizer()
						.getString("CHOOSE_C64_KERNAL_ROM"));
				c64FileDialog.setFileFilter(new RomFileFilter());
				final int c64Rc = c64FileDialog.showOpenDialog(parent);
				if (c64Rc == JFileChooser.APPROVE_OPTION
						&& c64FileDialog.getSelectedFile() != null) {
					getConfig().getSidplay2().setLastDirectory(
							c64FileDialog.getSelectedFile().getParentFile()
									.getAbsolutePath());
					final JFileChooser c1541FileDialog = new JFileChooser(
							getConfig().getSidplay2().getLastDirectory());
					c1541FileDialog.setDialogTitle(getSwix().getLocalizer()
							.getString("CHOOSE_C1541_KERNAL_ROM"));
					c1541FileDialog.setFileFilter(new RomFileFilter());
					final int c1541Rc = c1541FileDialog.showOpenDialog(parent);
					if (c1541Rc == JFileChooser.APPROVE_OPTION
							&& c1541FileDialog.getSelectedFile() != null) {
						getConfig().getSidplay2().setLastDirectory(
								c1541FileDialog.getSelectedFile()
										.getParentFile().getAbsolutePath());
						final File c64kernalFile = c64FileDialog
								.getSelectedFile();
						final File c1541kernalFile = c1541FileDialog
								.getSelectedFile();
						FileInputStream c64KernalStream = new FileInputStream(
								c64kernalFile);
						FileInputStream c1541KernalStream = new FileInputStream(
								c1541kernalFile);
						getPlayer().installJiffyDOS(c64KernalStream,
								c1541KernalStream);
						c64KernalStream.close();
						c1541KernalStream.close();
						reset();
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action uninstallJiffyDos = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().uninstallJiffyDOS();
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action insertGeoRAM2048 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, 2048);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	};

	public Action ejectCartridge = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getC64().ejectCartridge();
			reset();
		}
	};

	public Action freezeCartridge = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			getPlayer().getC64().getCartridge().freeze();
		}
	};

	public Action memory = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new Disassembler(getPlayer(), getConfig()).setTune(getPlayer()
					.getTune());
		}
	};

	public Action sidDump = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new SIDDump(getPlayer(), getConfig());
		}
	};

	public Action sidRegisters = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			new SidReg(getPlayer());
		}
	};

	public Action about = new AbstractAction() {

		protected JTextArea credits;
		protected JButton ok;
		protected JScrollPane scroller;

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				final Container sv = new SwingEngine(this)
						.render(JSIDPlay2UI.class
								.getResource("about/About.xml"));
				ok.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						sv.setVisible(false);
					}
				});
				Dimension d = sv.getToolkit().getScreenSize();
				Dimension s = sv.getSize();
				sv.setLocation(new Point((d.width - s.width) / 2,
						(d.height - s.height) / 2));
				credits.setText(getPlayer().getCredits());
				javax.swing.SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						scroller.getVerticalScrollBar().setValue(0);
					}
				});
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}

	};

	public Action doStartCapture = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (screenRecorder == null) {
				final Frame containerFrame = JOptionPane
						.getFrameForComponent(parent);
				try {
					String format = "QuickTime";// "AVI";
					screenRecorder = new ScreenRecorder(
							containerFrame.getGraphicsConfiguration(), format,
							16, ScreenRecorder.CursorEnum.WHITE, 15f, 15f,
							44100);
					screenRecorder.start();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (AWTException e1) {
					e1.printStackTrace();
				}
			}
		}
	};

	public Action doStopCapture = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (screenRecorder != null) {
				try {
					screenRecorder.stop();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					screenRecorder = null;
				}
			}
		}
	};

	public Action doHardcopyBmp = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Create a hardcopy of the Video screen
				final String outputDir = System.getProperty("jsidplay2.tmpdir");
				videoScreen
						.getC64Canvas()
						.getScreenCanvas()
						.hardCopy(
								"bmp",
								new File(outputDir, "screenshot")
										.getAbsolutePath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Action doHardcopyGif = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Create a hardcopy of the Video screen
				final String outputDir = System.getProperty("jsidplay2.tmpdir");
				videoScreen
						.getC64Canvas()
						.getScreenCanvas()
						.hardCopy(
								"gif",
								new File(outputDir, "screenshot")
										.getAbsolutePath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Action doHardcopyJpg = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Create a hardcopy of the Video screen
				final String outputDir = System.getProperty("jsidplay2.tmpdir");
				videoScreen
						.getC64Canvas()
						.getScreenCanvas()
						.hardCopy(
								"jpg",
								new File(outputDir, "screenshot")
										.getAbsolutePath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	public Action doHardcopyPng = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				// Create a hardcopy of the Video screen
				final String outputDir = System.getProperty("jsidplay2.tmpdir");
				videoScreen
						.getC64Canvas()
						.getScreenCanvas()
						.hardCopy(
								"png",
								new File(outputDir, "screenshot")
										.getAbsolutePath());
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	};

	/**
	 * Update the buttons according to the currently played song number
	 */
	public void setTune(final SidTune tune) {
		final int startSong, maxSong;
		final int currentSong;
		if (tune != null) {
			startSong = tune.getInfo().startSong;
			maxSong = tune.getInfo().songs;
			currentSong = tune.getInfo().currentSong;
		} else {
			maxSong = 0;
			currentSong = 0;
			startSong = 0;
		}

		pauseContinue.setSelected(false);
		pauseContinue2.setSelected(false);

		int prevSong = currentSong - 1;
		if (prevSong < 1) {
			prevSong = maxSong;
		}
		int nextSong = currentSong + 1;
		if (nextSong > maxSong) {
			nextSong = 1;
		}

		previous.setEnabled(maxSong != 0 && currentSong != startSong);
		previous2.setEnabled(previous.isEnabled());
		next.setEnabled(maxSong != 0 && nextSong != startSong);
		next2.setEnabled(next.isEnabled());

		previous.setText(String.format(
				getSwix().getLocalizer().getString("PREVIOUS") + " (%d/%d)",
				prevSong, maxSong));
		previous2.setToolTipText(previous.getText());

		next.setText(String.format(getSwix().getLocalizer().getString("NEXT")
				+ " (%d/%d)", nextSong, maxSong));
		next2.setToolTipText(next.getText());
		for (final Component tab : tabbedPane.getComponents()) {
			((TuneTab) tab).setTune(getPlayer(), tune);
		}
	}

	/**
	 * Set icon upon the component, that triggered the player
	 * 
	 * @param comp
	 *            component (source of the event to start the player)
	 */
	private void setPlayingIcon(final Component comp) {
		for (int i = 0; i < tabbedPane.getTabCount(); i++) {
			// Remove old tab icon
			if (tabbedPane.getIconAt(i) == PLAY_ICON) {
				tabbedPane.setIconAt(i, null);
			}
			// Set currently playing tab icon
			if (comp == tabbedPane.getComponentAt(i)) {
				tabbedPane.setIconAt(i, PLAY_ICON);
			}
		}
	}

	@Override
	public void notify(final UIEvent evt) {
		if (evt.isOfType(IUpdateUI.class)) {
			setStatusLine();
		} else if (evt.isOfType(ITuneStateChanged.class)) {
			pauseContinue.setSelected(false);
			pauseContinue2.setSelected(false);
		} else if (evt.isOfType(IMadeProgress.class)) {
			// Show current progress
			IMadeProgress ifObj = (IMadeProgress) evt.getUIEventImpl();
			progress.setValue(ifObj.getPercentage());
		} else if (evt.isOfType(IPlayTune.class)) {
			// Play a tune
			IPlayTune ifObj = (IPlayTune) evt.getUIEventImpl();
			if (ifObj.switchToVideoTab()) {
				tabbedPane.setSelectedIndex(0);
			}
			// set player icon
			setPlayingIcon(ifObj.getComponent());
		} else if (evt.isOfType(IInsertMedia.class)) {
			// Insert a disk/tape or cartridge
			IInsertMedia ifObj = (IInsertMedia) evt.getUIEventImpl();
			switch (ifObj.getMediaType()) {
			case DISK:
				// automatically turn drive on
				driveOn.setSelected(true);
			default:
			}
		}
	}

	public IConfig getConfig() {
		return cp.getConfig();
	}

	public Player getPlayer() {
		return cp.getPlayer();
	}

	public SwingEngine getSwix() {
		return swix;
	}

	protected void reset() {
		// reset
		uiEvents.fireEvent(Reset.class, new Reset() {

			@Override
			public Component getComponent() {
				return parent;
			}
		});
	}
}
