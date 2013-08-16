package ui;

import static sidplay.ConsolePlayer.playerExit;
import static sidplay.ConsolePlayer.playerRunning;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.ResourceBundle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TimelineBuilder;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import libsidplay.C64;
import libsidplay.common.ISID2Types.CPUClock;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PRG2TAP;
import libsidutils.SidDatabase;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ConsolePlayer;
import ui.about.About;
import ui.common.C64Stage;
import ui.common.C64Tab;
import ui.common.dialog.YesNoDialog;
import ui.disassembler.Disassembler;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;
import ui.events.IInsertMedia;
import ui.events.IMadeProgress;
import ui.events.IPlayTune;
import ui.events.UIEvent;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.ConfigFileExtension;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.RomFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.filefilter.TuneFileExtensions;
import ui.joysticksettings.JoystickSettings;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.soundsettings.SoundSettings;
import ui.videoscreen.Video;

public class JSidPlay2 extends C64Stage implements IExtendImageListener {

	/** Build date calculated from our own modify time */
	private static String DATE = "unknown";
	static {
		try {
			URL us = JSIDPlay2Main.class.getResource("/"
					+ JSidPlay2.class.getName().replace('.', '/') + ".class");
			Date date = new Date(us.openConnection().getLastModified());
			DATE = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
		} catch (IOException e) {
		}
	}

	private static final ImageView PLAY_ICON = new ImageView(
			"/ui/icons/play.png");
	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/track.wav").toString());

	@FXML
	private CheckMenuItem pauseContinue, driveOn, driveSoundOn, parCable,
			expand2000, expand4000, expand6000, expand8000, expandA000,
			turnPrinterOn;
	@FXML
	private RadioMenuItem normalSpeed, fastForward, ntsc, pal, c1541, c1541_II,
			neverExtend, askExtend, accessExtend;
	@FXML
	private MenuItem previous, next, load, video, reset, quit, stop,
			hardcopyPng, insertTape, insertDisk, insertCartridge;
	@FXML
	private ToggleButton pauseContinue2;
	@FXML
	private Button previous2, next2;
	@FXML
	private Tooltip previous2ToolTip, next2ToolTip;
	@FXML
	private TabPane tabbedPane;
	@FXML
	private Video videoScreen;
	@FXML
	private Label status;
	@FXML
	private ProgressBar progress;

	private boolean duringInitialization;
	private Timeline timer;
	private boolean oldMotorOn;
	private int oldHalfTrack;
	private Scene scene;
	private int hardcopyCounter;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.duringInitialization = true;
		this.scene = tabbedPane.getScene();

		getConsolePlayer().getState().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				if (arg2.intValue() == playerExit) {
					Platform.runLater(new Runnable() {
						public void run() {
							pauseContinue.setSelected(false);
							normalSpeed.setSelected(true);
						}
					});
				} else if (arg2.intValue() == playerRunning) {
					Platform.runLater(new Runnable() {
						public void run() {
							updatePlayerButtons();
						}
					});
				}
			}
		});
		this.load.setAccelerator(new KeyCodeCombination(KeyCode.L,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.video.setAccelerator(new KeyCodeCombination(KeyCode.V,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.reset.setAccelerator(new KeyCodeCombination(KeyCode.R,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.quit.setAccelerator(new KeyCodeCombination(KeyCode.Q,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.pauseContinue.setAccelerator(new KeyCodeCombination(KeyCode.P,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.previous.setAccelerator(new KeyCodeCombination(KeyCode.MINUS,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.next.setAccelerator(new KeyCodeCombination(KeyCode.PLUS,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.normalSpeed.setAccelerator(new KeyCodeCombination(KeyCode.COMMA,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.fastForward.setAccelerator(new KeyCodeCombination(KeyCode.DECIMAL,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.stop.setAccelerator(new KeyCodeCombination(KeyCode.T,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.hardcopyPng.setAccelerator(new KeyCodeCombination(KeyCode.N,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.insertTape.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT1,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.insertDisk.setAccelerator(new KeyCodeCombination(KeyCode.DIGIT8,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));
		this.insertCartridge.setAccelerator(new KeyCodeCombination(KeyCode.C,
				KeyCombination.CONTROL_DOWN, KeyCombination.SHORTCUT_DOWN));

		CPUClock defClk = getConfig().getEmulation().getDefaultClockSpeed();
		(defClk != null ? defClk == CPUClock.NTSC ? ntsc : pal : pal)
				.setSelected(true);
		driveOn.setSelected(getConfig().getC1541().isDriveOn());
		driveSoundOn.setSelected(getConfig().getC1541().isDriveSoundOn());
		parCable.setSelected(getConfig().getC1541().isParallelCable());
		FloppyType floppyType = getConfig().getC1541().getFloppyType();
		(floppyType == FloppyType.C1541 ? c1541 : c1541_II).setSelected(true);
		ExtendImagePolicy extendImagePolicy = getConfig().getC1541()
				.getExtendImagePolicy();
		(extendImagePolicy == ExtendImagePolicy.EXTEND_NEVER ? neverExtend
				: extendImagePolicy == ExtendImagePolicy.EXTEND_ACCESS ? askExtend
						: accessExtend).setSelected(true);
		expand2000.setSelected(getConfig().getC1541().isRamExpansionEnabled0());
		expand4000.setSelected(getConfig().getC1541().isRamExpansionEnabled1());
		expand6000.setSelected(getConfig().getC1541().isRamExpansionEnabled2());
		expand8000.setSelected(getConfig().getC1541().isRamExpansionEnabled3());
		expandA000.setSelected(getConfig().getC1541().isRamExpansionEnabled4());
		turnPrinterOn.setSelected(getConfig().getPrinter().isPrinterOn());

		pauseContinue.selectedProperty().bindBidirectional(
				pauseContinue2.selectedProperty());

		for (Tab tab : tabbedPane.getTabs()) {
			// XXX JavaFX: better initialization support using constructor
			// arguments?
			if (tab instanceof C64Tab) {
				C64Tab theTab = (C64Tab) tab;
				theTab.setConfig(getConfig());
				theTab.setPlayer(getPlayer());
				theTab.setConsolePlayer(getConsolePlayer());
				theTab.initialize(location, resources);
			}
		}
		this.duringInitialization = false;

		final Duration oneFrameAmt = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(oneFrameAmt,
				new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent evt) {
						setStatusLine();
					}
				});
		timer = TimelineBuilder.create().cycleCount(Animation.INDEFINITE)
				.keyFrames(oneFrame).build();
		timer.playFromStart();
	}

	@Override
	protected void doCloseWindow() {
		timer.stop();
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TuneFileExtensions.DESCRIPTION,
						TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {
				@Override
				public boolean switchToVideoTab() {
					return true;
				}

				@Override
				public SidTune getSidTune() {
					try {
						return SidTune.load(file);
					} catch (IOException | SidTuneError e) {
						e.printStackTrace();
						return null;
					}
				}

				@Override
				public Object getComponent() {
					return videoScreen;
				}
			});
		}
	}

	@FXML
	private void video() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				getPlayer().getC64().insertCartridge(file);
				final File tmpFile = new File(getConfig().getSidplay2()
						.getTmpDir(), "nuvieplayer-v1.0.prg");
				tmpFile.deleteOnExit();
				InputStream is = JSIDPlay2Main.class.getClassLoader()
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
				getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {
					@Override
					public boolean switchToVideoTab() {
						return true;
					}

					@Override
					public SidTune getSidTune() {
						try {
							return SidTune.load(tmpFile);
						} catch (IOException | SidTuneError e) {
							e.printStackTrace();
							return null;
						}
					}

					@Override
					public Object getComponent() {
						return videoScreen;
					}
				});
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@FXML
	private void reset() {
		if (!duringInitialization) {
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public Object getComponent() {
					return videoScreen;
				}

				@Override
				public boolean switchToVideoTab() {
					return false;
				}

				@Override
				public SidTune getSidTune() {
					return null;
				}
			});
		}
	}

	@FXML
	private void quit() {
		((Stage) scene.getWindow()).close();
	}

	@FXML
	private void pause() {
		getConsolePlayer().pause();
	}

	@FXML
	private void previousSong() {
		getConsolePlayer().previousSong();
	}

	@FXML
	private void nextSong() {
		getConsolePlayer().nextSong();
	}

	@FXML
	private void playNormalSpeed() {
		getConsolePlayer().normalSpeed();
	}

	@FXML
	private void playFastForward() {
		getConsolePlayer().fastForward();
	}

	@FXML
	private void stopSong() {
		getConsolePlayer().quit();
	}

	@FXML
	private void doHardcopyGif() {
		createHardCopy("gif");
	}

	@FXML
	private void doHardcopyJpg() {
		createHardCopy("jpg");
	}

	@FXML
	private void doHardcopyPng() {
		createHardCopy("png");
	}

	@FXML
	private void videoStandardPal() {
		getConfig().getEmulation().setDefaultClockSpeed(CPUClock.PAL);
		reset();
	}

	@FXML
	private void videoStandardNtsc() {
		getConfig().getEmulation().setDefaultClockSpeed(CPUClock.NTSC);
		reset();
	}

	@FXML
	private void soundSettings() {
		C64Stage window = new SoundSettings();
		window.setConsolePlayer(getConsolePlayer());
		window.setPlayer(getPlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void emulationSettings() {
		C64Stage window = new EmulationSettings();
		window.setConsolePlayer(getConsolePlayer());
		window.setPlayer(getPlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void joystickSettings() {
		C64Stage window = new JoystickSettings();
		window.setPlayer(getPlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void record() {
		getPlayer().getDatasette().control(Datasette.Control.RECORD);
	}

	@FXML
	private void play() {
		getPlayer().getDatasette().control(Datasette.Control.START);
	}

	@FXML
	private void rewind() {
		getPlayer().getDatasette().control(Datasette.Control.REWIND);
	}

	@FXML
	private void forward() {
		getPlayer().getDatasette().control(Datasette.Control.FORWARD);
	}

	@FXML
	private void stop() {
		getPlayer().getDatasette().control(Datasette.Control.STOP);
	}

	@FXML
	private void resetCounter() {
		getPlayer().getDatasette().control(Datasette.Control.RESET_COUNTER);
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TapeFileExtensions.DESCRIPTION,
						TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				insertTape(file, null, videoScreen);
			} catch (IOException e) {
				System.err.println(String.format("Cannot attach file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void ejectTape() {
		try {
			getPlayer().getDatasette().ejectTape();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@FXML
	private void turnDriveOn() {
		getPlayer().enableFloppyDiskDrives(driveOn.isSelected());
		getConfig().getC1541().setDriveOn(driveOn.isSelected());
	}

	@FXML
	private void driveSound() {
		getConfig().getC1541().setDriveSoundOn(driveSoundOn.isSelected());
	}

	@FXML
	private void parallelCable() {
		getPlayer().connectC64AndC1541WithParallelCable(parCable.isSelected());
		getConfig().getC1541().setParallelCable(parCable.isSelected());
	}

	@FXML
	private void floppyTypeC1541() {
		getFirstFloppy().setFloppyType(FloppyType.C1541);
		getConfig().getC1541().setFloppyType(FloppyType.C1541);
	}

	@FXML
	private void floppyTypeC1541_II() {
		getFirstFloppy().setFloppyType(FloppyType.C1541_II);
		getConfig().getC1541().setFloppyType(FloppyType.C1541_II);
	}

	@FXML
	private void extendNever() {
		getConfig().getC1541().setExtendImagePolicy(
				ExtendImagePolicy.EXTEND_NEVER);
	}

	@FXML
	private void extendAsk() {
		getConfig().getC1541().setExtendImagePolicy(
				ExtendImagePolicy.EXTEND_ASK);
	}

	@FXML
	private void extendAccess() {
		getConfig().getC1541().setExtendImagePolicy(
				ExtendImagePolicy.EXTEND_ACCESS);
	}

	@FXML
	private void expansion0x2000() {
		getFirstFloppy().setRamExpansion(0, expand2000.isSelected());
		getConfig().getC1541().setRamExpansion0(expand2000.isSelected());
	}

	@FXML
	private void expansion0x4000() {
		getFirstFloppy().setRamExpansion(1, expand4000.isSelected());
		getConfig().getC1541().setRamExpansion1(expand4000.isSelected());
	}

	@FXML
	private void expansion0x6000() {
		getFirstFloppy().setRamExpansion(2, expand6000.isSelected());
		getConfig().getC1541().setRamExpansion2(expand6000.isSelected());
	}

	@FXML
	private void expansion0x8000() {
		getFirstFloppy().setRamExpansion(3, expand8000.isSelected());
		getConfig().getC1541().setRamExpansion3(expand8000.isSelected());
	}

	@FXML
	private void expansion0xA000() {
		getFirstFloppy().setRamExpansion(4, expandA000.isSelected());
		getConfig().getC1541().setRamExpansion4(expandA000.isSelected());
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(DiskFileExtensions.DESCRIPTION,
						DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				insertDisk(file, null, videoScreen);
			} catch (IOException e) {
				System.err.println(String.format("Cannot attach file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void ejectDisk() {
		try {
			getFirstFloppy().getDiskController().ejectDisk();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@FXML
	private void resetDrive() {
		getFirstFloppy().reset();
	}

	@FXML
	private void printer() {
		getPlayer().turnPrinterOnOff(turnPrinterOn.isSelected());
		getConfig().getPrinter().setPrinterOn(turnPrinterOn.isSelected());
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		File lastDirectoryFile = ((SidPlay2Section) (getConfig().getSidplay2()))
				.getLastDirectoryFile();
		if (lastDirectoryFile != null && lastDirectoryFile.isDirectory())
			fileDialog.setInitialDirectory(lastDirectoryFile);
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				insertCartridge(file, videoScreen);
			} catch (IOException e) {
				System.err.println(String.format("Cannot attach file '%s'.",
						file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void insertGeoRAM() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				getPlayer().getC64().insertRAMExpansion(
						C64.RAMExpansion.GEORAM, file);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@FXML
	private void insertGeoRAM64() {
		try {
			getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 64);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM128() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.GEORAM,
					128);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM256() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.GEORAM,
					256);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM512() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.GEORAM,
					512);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM1024() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.GEORAM,
					1024);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU128() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU, 128);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU256() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU, 256);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU512() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU, 512);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU2048() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.REU, 2048);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU16384() {
		try {
			getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 16384);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void installJiffyDos() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setTitle(getBundle().getString("CHOOSE_C64_KERNAL_ROM"));
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(RomFileExtensions.DESCRIPTION,
						RomFileExtensions.EXTENSIONS));
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		final File c64kernalFile = fileDialog.showOpenDialog(scene.getWindow());
		if (c64kernalFile != null) {
			getConfig().getSidplay2().setLastDirectory(
					c64kernalFile.getParentFile().getAbsolutePath());
			final FileChooser c1541FileDialog = new FileChooser();
			c1541FileDialog.setTitle(getBundle().getString(
					"CHOOSE_C1541_KERNAL_ROM"));
			fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
					.getSidplay2())).getLastDirectoryFile());
			fileDialog.getExtensionFilters().add(
					new ExtensionFilter(RomFileExtensions.DESCRIPTION,
							RomFileExtensions.EXTENSIONS));
			final File c1541kernalFile = c1541FileDialog.showOpenDialog(scene
					.getWindow());
			if (c1541kernalFile != null) {
				getConfig().getSidplay2().setLastDirectory(
						c1541kernalFile.getParentFile().getAbsolutePath());
				try (FileInputStream c64KernalStream = new FileInputStream(
						c64kernalFile);
						FileInputStream c1541KernalStream = new FileInputStream(
								c1541kernalFile)) {
					getPlayer().installJiffyDOS(c64KernalStream,
							c1541KernalStream);
					reset();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@FXML
	private void uninstallJiffyDos() {
		try {
			getPlayer().uninstallJiffyDOS();
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM2048() {
		try {
			getPlayer().getC64().insertRAMExpansion(C64.RAMExpansion.GEORAM,
					2048);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void ejectCartridge() {
		getPlayer().getC64().ejectCartridge();
		reset();
	}

	@FXML
	private void freezeCartridge() {
		getPlayer().getC64().getCartridge().freeze();
	}

	@FXML
	private void memory() {
		C64Stage window = new Disassembler();
		window.setPlayer(getPlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void sidDump() {
		C64Stage window = new SidDump();
		window.setPlayer(getPlayer());
		window.setConsolePlayer(getConsolePlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void sidRegisters() {
		C64Stage window = new SidReg();
		window.setPlayer(getPlayer());
		window.setConsolePlayer(getConsolePlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void exportConfiguration() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFile());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(ConfigFileExtension.DESCRIPTION,
						ConfigFileExtension.EXTENSIONS));
		final File file = fileDialog.showSaveDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			try {
				JAXBContext jaxbContext = JAXBContext
						.newInstance(Configuration.class);
				Marshaller marshaller = jaxbContext.createMarshaller();
				marshaller.marshal(getConfig(), file);
			} catch (JAXBException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void importConfiguration() {
		YesNoDialog dialog = new YesNoDialog();
		dialog.setTitle(getBundle().getString("IMPORT_CONFIGURATION"));
		dialog.setText(getBundle().getString("PLEASE_RESTART"));
		try {
			dialog.open();
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (dialog.getConfirmed().get()) {
			final FileChooser fileDialog = new FileChooser();
			fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
					.getSidplay2())).getLastDirectoryFile());
			fileDialog.getExtensionFilters().add(
					new ExtensionFilter(ConfigFileExtension.DESCRIPTION,
							ConfigFileExtension.EXTENSIONS));
			final File file = fileDialog.showOpenDialog(scene.getWindow());
			if (file != null) {
				getConfig().getSidplay2().setLastDirectory(
						file.getParentFile().getAbsolutePath());
				getConfig().setReconfigFilename(file.getAbsolutePath());
			}
		}
	}

	@FXML
	private void about() {
		C64Stage window = new About();
		window.setPlayer(getPlayer());
		window.setConfig(getConfig());
		try {
			window.open();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void insertDisk(final File selectedDisk, final File autostartFile,
			final Object component) throws IOException {
		// automatically turn drive on
		getPlayer().enableFloppyDiskDrives(true);
		getConfig().getC1541().setDriveOn(true);
		// attach selected disk into the first disk drive
		DiskImage disk = getPlayer().getFloppies()[0].getDiskController()
				.insertDisk(selectedDisk);
		disk.setExtendImagePolicy(this);
		if (autostartFile != null) {
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public boolean switchToVideoTab() {
					return true;
				}

				@Override
				public SidTune getSidTune() {
					try {
						return SidTune.load(autostartFile);
					} catch (IOException | SidTuneError e) {
						e.printStackTrace();
						return null;
					}
				}

				@Override
				public Object getComponent() {
					return component;
				}
			});
		}
	}

	private void insertTape(final File selectedTape, final File autostartFile,
			final Object component) throws IOException {
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
			getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

				@Override
				public boolean switchToVideoTab() {
					return true;
				}

				@Override
				public SidTune getSidTune() {
					try {
						return SidTune.load(autostartFile);
					} catch (IOException | SidTuneError e) {
						e.printStackTrace();
						return null;
					}
				}

				@Override
				public Object getComponent() {
					return component;
				}
			});
		}
	}

	private void insertCartridge(final File selectedFile, final Object component)
			throws IOException {
		// Insert a cartridge
		getPlayer().getC64().insertCartridge(selectedFile);
		// reset required after inserting the cartridge
		getUiEvents().fireEvent(IPlayTune.class, new IPlayTune() {

			@Override
			public boolean switchToVideoTab() {
				return false;
			}

			@Override
			public SidTune getSidTune() {
				return null;
			}

			@Override
			public Object getComponent() {
				return component;
			}
		});
	}

	private void updatePlayerButtons() {
		SidTune tune = getPlayer().getTune();
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

		int prevSong = currentSong - 1;
		if (prevSong < 1) {
			prevSong = maxSong;
		}
		int nextSong = currentSong + 1;
		if (nextSong > maxSong) {
			nextSong = 1;
		}

		previous.setDisable(maxSong == 0 || currentSong == startSong);
		previous2.setDisable(previous.isDisable());
		next.setDisable(maxSong == 0 || nextSong == startSong);
		next2.setDisable(next.isDisable());

		previous.setText(String.format(getBundle().getString("PREVIOUS2")
				+ " (%d/%d)", prevSong, maxSong));
		previous2ToolTip.setText(previous.getText());

		next.setText(String.format(getBundle().getString("NEXT2") + " (%d/%d)",
				nextSong, maxSong));
		next2ToolTip.setText(next.getText());
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	private void setStatusLine() {
		// Get status information of the first disk drive
		final C1541 c1541 = getFirstFloppy();
		// Disk motor status
		boolean motorOn = getConfig().getC1541().isDriveSoundOn()
				&& getConsolePlayer().getState().get() == ConsolePlayer.playerRunning
				&& c1541.getDiskController().isMotorOn();
		if (!oldMotorOn && motorOn) {
			MOTORSOUND_AUDIOCLIP.setCycleCount(AudioClip.INDEFINITE);
			MOTORSOUND_AUDIOCLIP.play();
		} else if (oldMotorOn && !motorOn) {
			MOTORSOUND_AUDIOCLIP.stop();
		}
		oldMotorOn = motorOn;
		// Read/Write head position (half tracks)
		final int halfTrack = c1541.getDiskController().getHalfTrack();
		if (oldHalfTrack != halfTrack && motorOn) {
			TRACKSOUND_AUDIOCLIP.play();
		}
		oldHalfTrack = halfTrack;
		// Get status information of the datasette
		final Datasette datasette = getPlayer().getDatasette();
		// Datasette tape progress
		if (datasette.getMotor()) {
			progress.setProgress(datasette.getProgress() / 100f);
		}
		// Current play time / well-known song length
		String statusTime = String.format("%02d:%02d",
				getPlayer().time() / 60 % 100, getPlayer().time() % 60);
		String statusSongLength = "";
		final int songLength = getSongLength(getPlayer().getTune());
		// song length well-known?
		if (songLength > 0) {
			statusSongLength = String.format("/%02d:%02d",
					(songLength / 60 % 100), (songLength % 60));
		}
		// Memory usage
		Runtime runtime = Runtime.getRuntime();
		int totalMemory = (int) (runtime.totalMemory() / (1 << 20));
		int freeMemory = (int) (runtime.freeMemory() / (1 << 20));
		// final status bar text
		String text = String.format(getBundle().getString("DATASETTE_COUNTER")
				+ " %03d, " + getBundle().getString("FLOPPY_TRACK") + " %02d, "
				+ getBundle().getString("DATE") + " %s, "
				+ getBundle().getString("TIME") + " %s%s, "
				+ getBundle().getString("MEM") + " %d/%d MB",
				datasette.getCounter(), halfTrack >> 1, DATE, statusTime,
				statusSongLength, (totalMemory - freeMemory), totalMemory);
		status.setText(text);
	}

	private void createHardCopy(String format) {
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(videoScreen.getVicImage(),
					null), format, new File(getConfig().getSidplay2()
					.getTmpDir(), "screenshot" + (++hardcopyCounter) + "."
					+ format));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Get song length.
	 * 
	 * @param sidTune
	 *            tune to get song length for
	 * @return song length in seconds (0 means unknown, -1 means unconfigured)
	 */
	private int getSongLength(final SidTune sidTune) {
		File hvscRoot = ((SidPlay2Section) getConfig().getSidplay2())
				.getHvscFile();
		SidDatabase database = SidDatabase.getInstance(hvscRoot);
		getConsolePlayer().setSidDatabase(database);
		if (database != null && sidTune != null) {
			return database.length(sidTune);
		}
		return -1;
	}

	private C1541 getFirstFloppy() {
		return getPlayer().getFloppies()[0];
	}

	@Override
	public void notify(final UIEvent evt) {
		if (evt.isOfType(IMadeProgress.class)) {
			// Show current progress
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					IMadeProgress ifObj = (IMadeProgress) evt.getUIEventImpl();
					progress.setProgress(ifObj.getPercentage() / 100f);
				}
			});
		} else if (evt.isOfType(IPlayTune.class)) {
			pauseContinue.setSelected(false);
			// Play a tune
			Platform.runLater(new Runnable() {

				@Override
				public void run() {
					IPlayTune ifObj = (IPlayTune) evt.getUIEventImpl();
					if (ifObj.switchToVideoTab()) {
						tabbedPane.getSelectionModel().select(videoScreen);
					}
					// set player icon
					Object component = ifObj.getComponent();
					if (component != null && component instanceof Tab) {
						for (Tab tab : tabbedPane.getTabs()) {
							tab.setGraphic(null);
						}
						((Tab) component).setGraphic(PLAY_ICON);
					}
				}
			});
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
					// automatically turn drive on
					driveOn.setSelected(true);
					insertDisk(mediaFile, ifObj.getAutostartFile(),
							ifObj.getComponent());
					break;

				case CART:
					insertCartridge(mediaFile, ifObj.getComponent());
					break;

				default:
					break;
				}
			} catch (IOException e) {
				System.err.println(String.format("Cannot attach file '%s'.",
						mediaFile.getAbsolutePath()));
			}
		}
	}

	@Override
	public boolean isAllowed() {
		if (getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			// EXTEND_ASK
			YesNoDialog dialog = new YesNoDialog();
			dialog.setTitle(getBundle().getString("EXTEND_DISK_IMAGE"));
			dialog.setText(getBundle().getString(
					"EXTEND_DISK_IMAGE_TO_40_TRACKS"));
			try {
				dialog.open();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return dialog.getConfirmed().get();
		} else if (getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
			// EXTEND_ACCESS
			return true;
		} else {
			// EXTEND_NEVER
			return false;
		}
	}

}
