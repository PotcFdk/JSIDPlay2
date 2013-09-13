package ui;

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
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.ISID2Types;
import libsidplay.common.ISID2Types.CPUClock;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.consoleplayer.MediaType;
import sidplay.consoleplayer.State;
import ui.about.About;
import ui.common.C64Stage;
import ui.common.C64Tab;
import ui.common.dialog.YesNoDialog;
import ui.disassembler.Disassembler;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.service.ConfigService;
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
import de.schlichtherle.truezip.file.TFile;

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

	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/track.wav").toString());

	@FXML
	protected CheckMenuItem pauseContinue, driveOn, driveSoundOn, parCable,
			expand2000, expand4000, expand6000, expand8000, expandA000,
			turnPrinterOn;
	@FXML
	protected RadioMenuItem normalSpeed, fastForward, ntsc, pal, c1541,
			c1541_II, neverExtend, askExtend, accessExtend;
	@FXML
	protected MenuItem previous, next, load, video, reset, quit, stop,
			hardcopyPng, insertTape, insertDisk, insertCartridge;
	@FXML
	private ToggleButton pauseContinue2;
	@FXML
	protected Button previous2, next2;
	@FXML
	protected Tooltip previous2ToolTip, next2ToolTip;
	@FXML
	protected TabPane tabbedPane, musicCollTabbedPane, diskCollTabbedPane;
	@FXML
	private Tab musicCollections, diskCollections;
	@FXML
	protected Video videoScreen;
	@FXML
	private Label status;
	@FXML
	protected ProgressBar progress;

	private ConfigService configService;

	private boolean duringInitialization;
	private Timeline timer;
	private boolean oldMotorOn;
	private int oldHalfTrack;
	private Scene scene;
	private int hardcopyCounter;
	protected long lastUpdate;
	private String tuneSpeed;
	private String playerId;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.duringInitialization = true;
		this.scene = tabbedPane.getScene();

		getConsolePlayer().setExtendImagePolicy(this);

		getConsolePlayer().stateProperty().addListener(
				new ChangeListener<State>() {
					@Override
					public void changed(ObservableValue<? extends State> arg0,
							State arg1, final State state) {
						if (state == State.EXIT || state == State.RUNNING) {
							Platform.runLater(new Runnable() {
								public void run() {
									lastUpdate = getPlayer().getC64()
											.getEventScheduler()
											.getTime(Phase.PHI1);
									updatePlayerButtons(state);

									SidTune sidTune = getPlayer().getTune();
									if (sidTune == null
											|| sidTune.getInfo().playAddr == 0) {
										tabbedPane.getSelectionModel().select(
												videoScreen);
									}
								}

							});
						}
					}

					protected void updatePlayerButtons(State state) {
						pauseContinue.setSelected(false);
						normalSpeed.setSelected(true);

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

						previous.setDisable(state == State.EXIT || maxSong == 0
								|| currentSong == startSong);
						previous2.setDisable(previous.isDisable());
						next.setDisable(state == State.EXIT || maxSong == 0
								|| nextSong == startSong);
						next2.setDisable(next.isDisable());

						previous.setText(String
								.format(getBundle().getString("PREVIOUS2")
										+ " (%d/%d)", prevSong, maxSong));
						previous2ToolTip.setText(previous.getText());

						next.setText(String.format(
								getBundle().getString("NEXT2") + " (%d/%d)",
								nextSong, maxSong));
						next2ToolTip.setText(next.getText());
					}

				});
		pauseContinue.selectedProperty().bindBidirectional(
				pauseContinue2.selectedProperty());
		driveOn.selectedProperty().bindBidirectional(
				getPlayer().drivesEnabledProperty());

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

		setModel(location, resources, tabbedPane);
		setModel(location, resources, musicCollTabbedPane);
		setModel(location, resources, diskCollTabbedPane);
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

	private void setModel(URL location, ResourceBundle resources,
			TabPane tabPane) {
		for (Tab tab : tabPane.getTabs()) {
			if (tab instanceof C64Tab) {
				C64Tab theTab = (C64Tab) tab;
				theTab.setConfig(getConfig());
				theTab.setPlayer(getPlayer());
				theTab.setConsolePlayer(getConsolePlayer());
				theTab.initialize(location, resources);
				if (theTab.getProgressValue() != null) {
					theTab.getProgressValue().addListener(
							progressUpdateListener());
				}
			}
		}
	}

	@Override
	protected void doCloseWindow() {
		timer.stop();
	}

	private ChangeListener<Number> progressUpdateListener() {
		return new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, final Number arg2) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						progress.progressProperty().set(
								arg2.doubleValue() / 100.);
					}
				});
			}
		};
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TuneFileExtensions.DESCRIPTION,
						TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			playTune(file);
		}
	}

	@FXML
	private void video() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			final File tmpFile = new File(
					getConfig().getSidplay2().getTmpDir(),
					"nuvieplayer-v1.0.prg");
			tmpFile.deleteOnExit();
			try (OutputStream os = new FileOutputStream(tmpFile);
					InputStream is = JSIDPlay2Main.class.getClassLoader()
							.getResourceAsStream(
									"libsidplay/mem/nuvieplayer-v1.0.prg")) {
				byte[] b = new byte[1024];
				while (is.available() > 0) {
					int len = is.read(b);
					if (len > 0) {
						os.write(b, 0, len);
					}
				}
				getPlayer().getC64().insertCartridge(file);
			} catch (IOException ex) {
				ex.printStackTrace();
			}
			playTune(tmpFile);
		}
	}

	@FXML
	private void reset() {
		if (!duringInitialization) {
			playTune(null);
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
		window.getProgressValue().addListener(progressUpdateListener());
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TapeFileExtensions.DESCRIPTION,
						TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getConsolePlayer().insertMedia(new TFile(file), null,
					MediaType.TAPE);
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(DiskFileExtensions.DESCRIPTION,
						DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getConsolePlayer().insertMedia(new TFile(file), null,
					MediaType.DISK);
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			getConsolePlayer().insertMedia(new TFile(file), null,
					MediaType.CART);
		}
	}

	@FXML
	private void insertGeoRAM() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
				.getSidplay2())).getLastDirectoryFolder());
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
				.getSidplay2())).getLastDirectoryFolder());
		final File c64kernalFile = fileDialog.showOpenDialog(scene.getWindow());
		if (c64kernalFile != null) {
			getConfig().getSidplay2().setLastDirectory(
					c64kernalFile.getParentFile().getAbsolutePath());
			final FileChooser c1541FileDialog = new FileChooser();
			c1541FileDialog.setTitle(getBundle().getString(
					"CHOOSE_C1541_KERNAL_ROM"));
			fileDialog.setInitialDirectory(((SidPlay2Section) (getConfig()
					.getSidplay2())).getLastDirectoryFolder());
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
		window.setConsolePlayer(getConsolePlayer());
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
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(ConfigFileExtension.DESCRIPTION,
						ConfigFileExtension.EXTENSIONS));
		final File file = fileDialog.showSaveDialog(scene.getWindow());
		if (file != null) {
			File target = new File(file.getParentFile(),
					PathUtils.getBaseNameNoExt(file) + ".xml");
			getConfig().getSidplay2().setLastDirectory(
					file.getParentFile().getAbsolutePath());
			configService.exportCfg(getConfig(), target);
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
					.getSidplay2())).getLastDirectoryFolder());
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

	private void playTune(final File file) {
		setPlayedGraphics(videoScreen.getContent());
		try {
			getConsolePlayer().playTune(SidTune.load(file), null);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	protected void setStatusLine() {
		// Get status information of the first disk drive
		final C1541 c1541 = getFirstFloppy();
		// Disk motor status
		boolean motorOn = getConfig().getC1541().isDriveSoundOn()
				&& getConsolePlayer().stateProperty().get() == State.RUNNING
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
		int songLength = getConsolePlayer()
				.getSongLength(getPlayer().getTune());
		// song length well-known?
		if (songLength > 0) {
			statusSongLength = String.format("/%02d:%02d",
					(songLength / 60 % 100), (songLength % 60));
		}
		// Memory usage
		Runtime runtime = Runtime.getRuntime();
		int totalMemory = (int) (runtime.totalMemory() / (1 << 20));
		int freeMemory = (int) (runtime.freeMemory() / (1 << 20));
		// tune speed
		determineTuneSpeed();
		// playerID
		getPlayerId();
		// final status bar text
		String text = String.format(getBundle().getString("DATASETTE_COUNTER")
				+ " %03d, " + getBundle().getString("FLOPPY_TRACK") + " %02d, "
				+ getBundle().getString("DATE") + " %s, "
				+ getBundle().getString("TIME") + " %s%s, %s%s"
				+ getBundle().getString("MEM") + " %d/%d MB",
				datasette.getCounter(), halfTrack >> 1, DATE, statusTime,
				statusSongLength, tuneSpeed, playerId,
				(totalMemory - freeMemory), totalMemory);
		status.setText(text);
	}

	private void getPlayerId() {
		if (getPlayer().getTune() != null) {
			final StringBuilder ids = new StringBuilder();
			for (final String s : getPlayer().getTune().identify()) {
				if (ids.length() > 0) {
					ids.append(", ");
				}
				ids.append(s);
			}
			if (ids.length() > 0) {
				playerId = getBundle().getString("PLAYER_ID") + " "
						+ ids.toString().substring(0, Math.min(ids.length(), 16));
				if (ids.length() > 16) {
					playerId += "...";
				}
				playerId += ", ";
			} else {
				playerId = "";
			}
		} else {
			playerId = "";
		}
	}

	private void determineTuneSpeed() {
		if (getPlayer().getTune() != null) {
			C64 c64 = getPlayer().getC64();
			final EventScheduler ctx = c64.getEventScheduler();
			final ISID2Types.CPUClock systemClock = c64.getClock();
			final double waitClocks = systemClock.getCpuFrequency();
			final long now = ctx.getTime(Event.Phase.PHI1);
			final double interval = now - lastUpdate;
			if (interval >= waitClocks) {
				lastUpdate = now;

				final double callsSinceLastRead = c64
						.callsToPlayRoutineSinceLastTime()
						* waitClocks
						/ interval;
				/* convert to number of calls per frame */
				tuneSpeed = getBundle().getString("SPEED")
						+ " "
						+ String.format("%.1f", callsSinceLastRead
								/ systemClock.getRefresh());
				tuneSpeed += "x, ";
			}
		} else {
			tuneSpeed = "";
		}
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

	private C1541 getFirstFloppy() {
		return getPlayer().getFloppies()[0];
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

	public void setConfigService(ConfigService configService) {
		this.configService = configService;
	}

}
