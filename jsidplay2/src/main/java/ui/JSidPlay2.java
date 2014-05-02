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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
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
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;

import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.MediaType;
import sidplay.consoleplayer.State;
import ui.about.About;
import ui.common.C64Window;
import ui.common.dialog.YesNoDialog;
import ui.disassembler.Disassembler;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.Configuration;
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

public class JSidPlay2 extends C64Window implements IExtendImageListener {

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
	protected MenuItem previous, next;
	@FXML
	private ToggleButton pauseContinue2;
	@FXML
	protected Button previous2, next2;
	@FXML
	protected Tooltip previous2ToolTip, next2ToolTip;
	@FXML
	protected TabPane tabbedPane;
	@FXML
	protected Tab musicCollections, favorites;
	@FXML
	protected Video videoScreen;
	@FXML
	private Label status;
	@FXML
	protected ProgressBar progress;

	private ConfigService configService;

	private Scene scene;
	private Timeline timer;
	private long lastUpdate;
	private int oldHalfTrack, hardcopyCounter;
	private boolean duringInitialization, oldMotorOn;
	private StringBuilder tuneSpeed;
	private StringBuilder playerId;

	public JSidPlay2(Stage primaryStage, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		super(primaryStage, consolePlayer, player, config);
	}

	@FXML
	private void initialize() {
		this.duringInitialization = true;

		this.tuneSpeed = new StringBuilder();
		this.playerId = new StringBuilder();
		this.scene = tabbedPane.getScene();

		util.getConsolePlayer().setExtendImagePolicy(this);
		util.getConsolePlayer()
				.stateProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							if (newValue == State.EXIT
									|| newValue == State.RUNNING) {
								Platform.runLater(() -> {
									getPlayerId();
									lastUpdate = util.getPlayer().getC64()
											.getEventScheduler()
											.getTime(Phase.PHI1);
									updatePlayerButtons(newValue);

									SidTune sidTune = util.getPlayer()
											.getTune();
									if (sidTune == null
											|| (sidTune.getInfo().playAddr == 0
													&& !favorites.isSelected() && !musicCollections
														.isSelected())) {
										tabbedPane.getSelectionModel().select(
												videoScreen);
									}
								}

								);
							}
						}

				);
		pauseContinue.selectedProperty().bindBidirectional(
				pauseContinue2.selectedProperty());
		driveOn.selectedProperty().bindBidirectional(
				util.getPlayer().drivesEnabledProperty());

		CPUClock defClk = util.getConfig().getEmulation()
				.getDefaultClockSpeed();
		(defClk != null ? defClk == CPUClock.NTSC ? ntsc : pal : pal)
				.setSelected(true);
		driveSoundOn.setSelected(util.getConfig().getC1541().isDriveSoundOn());
		parCable.setSelected(util.getConfig().getC1541().isParallelCable());
		FloppyType floppyType = util.getConfig().getC1541().getFloppyType();
		(floppyType == FloppyType.C1541 ? c1541 : c1541_II).setSelected(true);
		ExtendImagePolicy extendImagePolicy = util.getConfig().getC1541()
				.getExtendImagePolicy();
		(extendImagePolicy == ExtendImagePolicy.EXTEND_NEVER ? neverExtend
				: extendImagePolicy == ExtendImagePolicy.EXTEND_ACCESS ? askExtend
						: accessExtend).setSelected(true);
		expand2000.setSelected(util.getConfig().getC1541()
				.isRamExpansionEnabled0());
		expand4000.setSelected(util.getConfig().getC1541()
				.isRamExpansionEnabled1());
		expand6000.setSelected(util.getConfig().getC1541()
				.isRamExpansionEnabled2());
		expand8000.setSelected(util.getConfig().getC1541()
				.isRamExpansionEnabled3());
		expandA000.setSelected(util.getConfig().getC1541()
				.isRamExpansionEnabled4());
		turnPrinterOn.setSelected(util.getConfig().getPrinter().isPrinterOn());

		this.duringInitialization = false;

		final Duration oneFrameAmt = Duration.millis(1000);
		final KeyFrame oneFrame = new KeyFrame(oneFrameAmt,
				(evt) -> setStatusLine());
		timer = new Timeline(oneFrame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	private void updatePlayerButtons(State state) {
		pauseContinue.setSelected(false);
		normalSpeed.setSelected(true);

		SidTune tune = util.getPlayer().getTune();
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

		previous.setText(String.format(util.getBundle().getString("PREVIOUS2")
				+ " (%d/%d)", prevSong, maxSong));
		previous2ToolTip.setText(previous.getText());

		next.setText(String.format(util.getBundle().getString("NEXT2")
				+ " (%d/%d)", nextSong, maxSong));
		next2ToolTip.setText(next.getText());
	}

	@Override
	public void doClose() {
		timer.stop();
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TuneFileExtensions.DESCRIPTION,
						TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			playTune(file);
		}
	}

	@FXML
	private void video() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			final File tmpFile = new File(util.getConfig().getSidplay2()
					.getTmpDir(), "nuvieplayer-v1.0.prg");
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
				util.getPlayer().getC64().insertCartridge(file);
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
		close();
		Platform.exit();
	}

	@FXML
	private void pause() {
		util.getConsolePlayer().pause();
	}

	@FXML
	private void previousSong() {
		util.getConsolePlayer().previousSong();
	}

	@FXML
	private void nextSong() {
		util.getConsolePlayer().nextSong();
	}

	@FXML
	private void playNormalSpeed() {
		util.getConsolePlayer().normalSpeed();
	}

	@FXML
	private void playFastForward() {
		util.getConsolePlayer().fastForward();
	}

	@FXML
	private void stopSong() {
		util.getConsolePlayer().quit();
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
		util.getConfig().getEmulation().setDefaultClockSpeed(CPUClock.PAL);
		reset();
	}

	@FXML
	private void videoStandardNtsc() {
		util.getConfig().getEmulation().setDefaultClockSpeed(CPUClock.NTSC);
		reset();
	}

	@FXML
	private void soundSettings() {
		C64Window window = new SoundSettings(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		window.open();
	}

	@FXML
	private void emulationSettings() {
		C64Window window = new EmulationSettings(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		window.open();
	}

	@FXML
	private void joystickSettings() {
		C64Window window = new JoystickSettings(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		window.open();
	}

	@FXML
	private void record() {
		util.getPlayer().getDatasette().control(Datasette.Control.RECORD);
	}

	@FXML
	private void play() {
		util.getPlayer().getDatasette().control(Datasette.Control.START);
	}

	@FXML
	private void rewind() {
		util.getPlayer().getDatasette().control(Datasette.Control.REWIND);
	}

	@FXML
	private void forward() {
		util.getPlayer().getDatasette().control(Datasette.Control.FORWARD);
	}

	@FXML
	private void stop() {
		util.getPlayer().getDatasette().control(Datasette.Control.STOP);
	}

	@FXML
	private void resetCounter() {
		util.getPlayer().getDatasette()
				.control(Datasette.Control.RESET_COUNTER);
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(TapeFileExtensions.DESCRIPTION,
						TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			util.getConsolePlayer().insertMedia(new TFile(file), null,
					MediaType.TAPE);
		}
	}

	@FXML
	private void ejectTape() {
		try {
			util.getPlayer().getDatasette().ejectTape();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	@FXML
	private void turnDriveOn() {
		util.getPlayer().enableFloppyDiskDrives(driveOn.isSelected());
		util.getConfig().getC1541().setDriveOn(driveOn.isSelected());
	}

	@FXML
	private void driveSound() {
		util.getConfig().getC1541().setDriveSoundOn(driveSoundOn.isSelected());
	}

	@FXML
	private void parallelCable() {
		util.getPlayer().connectC64AndC1541WithParallelCable(
				parCable.isSelected());
		util.getConfig().getC1541().setParallelCable(parCable.isSelected());
	}

	@FXML
	private void floppyTypeC1541() {
		getFirstFloppy().setFloppyType(FloppyType.C1541);
		util.getConfig().getC1541().setFloppyType(FloppyType.C1541);
	}

	@FXML
	private void floppyTypeC1541_II() {
		getFirstFloppy().setFloppyType(FloppyType.C1541_II);
		util.getConfig().getC1541().setFloppyType(FloppyType.C1541_II);
	}

	@FXML
	private void extendNever() {
		util.getConfig().getC1541()
				.setExtendImagePolicy(ExtendImagePolicy.EXTEND_NEVER);
	}

	@FXML
	private void extendAsk() {
		util.getConfig().getC1541()
				.setExtendImagePolicy(ExtendImagePolicy.EXTEND_ASK);
	}

	@FXML
	private void extendAccess() {
		util.getConfig().getC1541()
				.setExtendImagePolicy(ExtendImagePolicy.EXTEND_ACCESS);
	}

	@FXML
	private void expansion0x2000() {
		getFirstFloppy().setRamExpansion(0, expand2000.isSelected());
		util.getConfig().getC1541().setRamExpansion0(expand2000.isSelected());
	}

	@FXML
	private void expansion0x4000() {
		getFirstFloppy().setRamExpansion(1, expand4000.isSelected());
		util.getConfig().getC1541().setRamExpansion1(expand4000.isSelected());
	}

	@FXML
	private void expansion0x6000() {
		getFirstFloppy().setRamExpansion(2, expand6000.isSelected());
		util.getConfig().getC1541().setRamExpansion2(expand6000.isSelected());
	}

	@FXML
	private void expansion0x8000() {
		getFirstFloppy().setRamExpansion(3, expand8000.isSelected());
		util.getConfig().getC1541().setRamExpansion3(expand8000.isSelected());
	}

	@FXML
	private void expansion0xA000() {
		getFirstFloppy().setRamExpansion(4, expandA000.isSelected());
		util.getConfig().getC1541().setRamExpansion4(expandA000.isSelected());
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(DiskFileExtensions.DESCRIPTION,
						DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			util.getConsolePlayer().insertMedia(new TFile(file), null,
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
		util.getPlayer().turnPrinterOnOff(turnPrinterOn.isSelected());
		util.getConfig().getPrinter().setPrinterOn(turnPrinterOn.isSelected());
	}

	@FXML
	private void insertCartridge() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			util.getConsolePlayer().insertMedia(new TFile(file), null,
					MediaType.CART);
		}
	}

	@FXML
	private void insertGeoRAM() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			try {
				util.getPlayer().getC64()
						.insertRAMExpansion(C64.RAMExpansion.GEORAM, file);
				reset();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	@FXML
	private void insertGeoRAM64() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 64);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM128() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 128);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM256() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 256);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM512() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 512);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM1024() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 1024);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertGeoRAM2048() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.GEORAM, 2048);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU128() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 128);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU256() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 256);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU512() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 512);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU2048() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 2048);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void insertREU16384() {
		try {
			util.getPlayer().getC64()
					.insertRAMExpansion(C64.RAMExpansion.REU, 16384);
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void installJiffyDos() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog
				.setTitle(util.getBundle().getString("CHOOSE_C64_KERNAL_ROM"));
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(RomFileExtensions.DESCRIPTION,
						RomFileExtensions.EXTENSIONS));
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		final File c64kernalFile = fileDialog.showOpenDialog(scene.getWindow());
		if (c64kernalFile != null) {
			util.getConfig()
					.getSidplay2()
					.setLastDirectory(
							c64kernalFile.getParentFile().getAbsolutePath());
			final FileChooser c1541FileDialog = new FileChooser();
			c1541FileDialog.setTitle(util.getBundle().getString(
					"CHOOSE_C1541_KERNAL_ROM"));
			fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
					.getSidplay2())).getLastDirectoryFolder());
			fileDialog.getExtensionFilters().add(
					new ExtensionFilter(RomFileExtensions.DESCRIPTION,
							RomFileExtensions.EXTENSIONS));
			final File c1541kernalFile = c1541FileDialog.showOpenDialog(scene
					.getWindow());
			if (c1541kernalFile != null) {
				util.getConfig()
						.getSidplay2()
						.setLastDirectory(
								c1541kernalFile.getParentFile()
										.getAbsolutePath());
				try (FileInputStream c64KernalStream = new FileInputStream(
						c64kernalFile);
						FileInputStream c1541KernalStream = new FileInputStream(
								c1541kernalFile)) {
					util.getPlayer().installJiffyDOS(c64KernalStream,
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
			util.getPlayer().uninstallJiffyDOS();
			reset();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	@FXML
	private void ejectCartridge() {
		util.getPlayer().getC64().ejectCartridge();
		reset();
	}

	@FXML
	private void freezeCartridge() {
		util.getPlayer().getC64().getCartridge().freeze();
	}

	@FXML
	private void memory() {
		C64Window window = new Disassembler(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		window.open();
	}

	@FXML
	private void sidDump() {
		C64Window window = new SidDump(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		window.open();
	}

	@FXML
	private void sidRegisters() {
		C64Window window = new SidReg(util.getConsolePlayer(), util.getPlayer(),
				util.getConfig());
		window.open();
	}

	@FXML
	private void exportConfiguration() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(ConfigFileExtension.DESCRIPTION,
						ConfigFileExtension.EXTENSIONS));
		final File file = fileDialog.showSaveDialog(scene.getWindow());
		if (file != null) {
			File target = new File(file.getParentFile(),
					PathUtils.getBaseNameNoExt(file) + ".xml");
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			configService.exportCfg(util.getConfig(), target);
		}
	}

	@FXML
	private void importConfiguration() {
		YesNoDialog dialog = new YesNoDialog(util.getConsolePlayer(),
				util.getPlayer(), util.getConfig());
		dialog.getStage().setTitle(
				util.getBundle().getString("IMPORT_CONFIGURATION"));
		dialog.setText(util.getBundle().getString("PLEASE_RESTART"));
		dialog.open();
		if (dialog.getConfirmed().get()) {
			final FileChooser fileDialog = new FileChooser();
			fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
					.getSidplay2())).getLastDirectoryFolder());
			fileDialog.getExtensionFilters().add(
					new ExtensionFilter(ConfigFileExtension.DESCRIPTION,
							ConfigFileExtension.EXTENSIONS));
			final File file = fileDialog.showOpenDialog(scene.getWindow());
			if (file != null) {
				util.getConfig()
						.getSidplay2()
						.setLastDirectory(
								file.getParentFile().getAbsolutePath());
				util.getConfig().setReconfigFilename(file.getAbsolutePath());
			}
		}
	}

	@FXML
	private void about() {
		C64Window window = new About(util.getConsolePlayer(), util.getPlayer(),
				util.getConfig());
		window.open();

	}

	private void playTune(final File file) {
		util.setPlayingTab(videoScreen);
		try {
			util.getConsolePlayer().playTune(SidTune.load(file), null);
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
		boolean motorOn = util.getConfig().getC1541().isDriveSoundOn()
				&& util.getConsolePlayer().stateProperty().get() == State.RUNNING
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
		final Datasette datasette = util.getPlayer().getDatasette();
		// Datasette tape progress
		if (datasette.getMotor()) {
			progress.setProgress(datasette.getProgress() / 100f);
		}
		// Current play time / well-known song length
		String statusTime = determinePlayTime();
		String statusSongLength = determineSongLength();
		// Memory usage
		int totalMemory = determineMemusage(Runtime.getRuntime().totalMemory());
		int freeMemory = determineMemusage(Runtime.getRuntime().freeMemory());
		// tune speed
		determineTuneSpeed();
		// final status bar text
		StringBuilder format = new StringBuilder();
		format.append(util.getBundle().getString("RELEASE")).append(" %s, ")
				.append(util.getBundle().getString("DATASETTE_COUNTER"))
				.append(" %03d, ")
				.append(util.getBundle().getString("FLOPPY_TRACK"))
				.append(" %02d, ").append("%s%s")
				.append(util.getBundle().getString("TIME")).append(" %s%s, ")
				.append(util.getBundle().getString("MEM")).append(" %d/%d MB");
		status.setText(String.format(format.toString(), DATE,
				datasette.getCounter(), halfTrack >> 1, playerId, tuneSpeed,
				statusTime, statusSongLength, (totalMemory - freeMemory),
				totalMemory));
	}

	private String determinePlayTime() {
		int time = util.getPlayer().time();
		return String.format("%02d:%02d", time / 60 % 100, time % 60);
	}

	private String determineSongLength() {
		int songLength = util.getConsolePlayer().getSongLength(
				util.getPlayer().getTune());
		if (songLength > 0) {
			// song length well-known?
			return String.format("/%02d:%02d", (songLength / 60 % 100),
					(songLength % 60));
		}
		return "";
	}

	private int determineMemusage(long mem) {
		return (int) (mem / (1 << 20));
	}

	private void getPlayerId() {
		playerId.setLength(0);
		if (util.getPlayer().getTune() != null) {
			for (final String id : util.getPlayer().getTune().identify()) {
				playerId.append(util.getBundle().getString("PLAYER_ID"))
						.append(" ").append(id);
				int length = id.length();
				playerId.setLength(playerId.length()
						- (length - Math.min(length, 14)));
				if (length > 14) {
					playerId.append("...");
				}
				playerId.append(", ");
				break;
			}
		}
	}

	private void determineTuneSpeed() {
		if (util.getPlayer().getTune() != null) {
			C64 c64 = util.getPlayer().getC64();
			final EventScheduler ctx = c64.getEventScheduler();
			final CPUClock systemClock = c64.getClock();
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
				tuneSpeed.setLength(0);
				tuneSpeed.append(util.getBundle().getString("SPEED")).append(
						String.format(" %.1fx, ", callsSinceLastRead
								/ systemClock.getRefresh()));
			}
		} else {
			tuneSpeed.setLength(0);
		}
	}

	private void createHardCopy(String format) {
		try {
			ImageIO.write(SwingFXUtils.fromFXImage(videoScreen.getVicImage(),
					null), format, new File(util.getConfig().getSidplay2()
					.getTmpDir(), "screenshot" + (++hardcopyCounter) + "."
					+ format));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private C1541 getFirstFloppy() {
		return util.getPlayer().getFloppies()[0];
	}

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			// EXTEND_ASK
			YesNoDialog dialog = new YesNoDialog(util.getConsolePlayer(),
					util.getPlayer(), util.getConfig());
			dialog.getStage().setTitle(
					util.getBundle().getString("EXTEND_DISK_IMAGE"));
			dialog.setText(util.getBundle().getString(
					"EXTEND_DISK_IMAGE_TO_40_TRACKS"));
			dialog.open();
			return dialog.getConfirmed().get();
		} else if (util.getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
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
