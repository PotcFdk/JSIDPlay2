package ui;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;
import javax.sound.sampled.LineUnavailableException;

import libsidplay.C64;
import libsidplay.Player;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.cart.CartridgeType;
import libsidplay.player.PlayList;
import libsidplay.player.State;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import sidplay.audio.Audio;
import sidplay.audio.AudioConfig;
import sidplay.audio.CmpMP3File;
import sidplay.audio.JavaSound;
import sidplay.audio.JavaSound.Device;
import sidplay.audio.RecordingFilenameProvider;
import sidplay.ini.IniReader;
import sidplay.ini.intf.ISidPlay2Section;
import ui.about.About;
import ui.asm.Asm;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.dialog.YesNoDialog;
import ui.console.Console;
import ui.disassembler.Disassembler;
import ui.diskcollection.DiskCollection;
import ui.diskcollection.DiskCollectionType;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.C1541Section;
import ui.entities.config.PrinterSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.ViewEntity;
import ui.entities.config.service.ConfigService;
import ui.favorites.Favorites;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.ConfigFileExtension;
import ui.filefilter.DiskFileExtensions;
import ui.filefilter.RomFileExtensions;
import ui.filefilter.TapeFileExtensions;
import ui.filefilter.TuneFileExtensions;
import ui.gamebase.GameBase;
import ui.joysticksettings.JoystickSettings;
import ui.musiccollection.MusicCollection;
import ui.musiccollection.MusicCollectionType;
import ui.oscilloscope.Oscilloscope;
import ui.printer.Printer;
import ui.proxysettings.ProxySettings;
import ui.siddump.SidDump;
import ui.sidreg.SidReg;
import ui.update.Update;
import ui.videoscreen.Video;
import ui.webview.WebView;
import ui.webview.WebViewType;

public class JSidPlay2 extends C64Window implements IExtendImageListener,
		RecordingFilenameProvider {

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

	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static final byte[] NUVIE_PLAYER = new byte[2884];
	static {
		try (DataInputStream is = new DataInputStream(
				JSidPlay2.class.getResourceAsStream(NUVIE_PLAYER_PRG))) {
			is.readFully(NUVIE_PLAYER);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/track.wav").toString());

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	protected CheckMenuItem pauseContinue, driveOn, driveSoundOn, parCable,
			expand2000, expand4000, expand6000, expand8000, expandA000,
			turnPrinterOn;
	@FXML
	protected RadioMenuItem fastForward, normalSpeed, c1541, c1541_II,
			neverExtend, askExtend, accessExtend;
	@FXML
	protected MenuItem previous, next;
	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> samplingRateBox, hardsid6581Box, hardsid8580Box;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private ComboBox<String> sidDriverBox;
	@FXML
	private ComboBox<Device> devicesBox;
	@FXML
	private ComboBox<Emulation> emulationBox;
	@FXML
	private CheckBox enableSldb, singleSong;
	@FXML
	private TextField defaultTime;
	@FXML
	private ToggleButton pauseContinue2, fastForward2;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	protected Button previous2, next2, nextFavorite, volumeButton, mp3Browse;
	@FXML
	protected Tooltip previous2ToolTip, next2ToolTip;
	@FXML
	protected TabPane tabbedPane;
	@FXML
	private Label status, hardsid6581Label, hardsid8580Label;
	@FXML
	protected ProgressBar progress;

	private ObservableList<Device> devices;

	private ConfigService configService;

	private Scene scene;
	private Timeline timer;
	private long lastUpdate;
	private int oldHalfTrack, hardcopyCounter;
	private boolean duringInitialization, oldMotorOn;
	private StringBuilder tuneSpeed, playerId;
	private BooleanProperty nextFavoriteDisabledState;

	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	private void initialize() {
		this.duringInitialization = true;

		this.tuneSpeed = new StringBuilder();
		this.playerId = new StringBuilder();
		this.scene = tabbedPane.getScene();

		util.getPlayer().setRecordingFilenameProvider(this);
		util.getPlayer().setExtendImagePolicy(this);
		util.getPlayer()
				.stateProperty()
				.addListener(
						(observable, oldValue, newValue) -> {
							SidTune sidTune = util.getPlayer().getTune();
							Platform.runLater(() -> nextFavoriteDisabledState
									.set(sidTune == SidTune.RESET));
							if (newValue == State.RUNNING) {
								Platform.runLater(() -> {
									getPlayerId();
									lastUpdate = util.getPlayer().getC64()
											.getEventScheduler()
											.getTime(Phase.PHI1);
									updatePlayerButtons(newValue);

									final Tab selectedItem = tabbedPane
											.getSelectionModel()
											.getSelectedItem();
									boolean doNotSwitch = selectedItem != null
											&& (MusicCollection.class
													.isAssignableFrom(selectedItem
															.getClass()) || Favorites.class
													.isAssignableFrom(selectedItem
															.getClass()));
									if (sidTune == null
											|| (sidTune.getInfo().getPlayAddr() == 0 && !doNotSwitch)) {
										video();
										tabbedPane
												.getSelectionModel()
												.select(tabbedPane
														.getTabs()
														.stream()
														.filter((tab) -> tab
																.getId()
																.equals(Video.ID))
														.findFirst().get());
									}
								}

								);
							}
						}

				);
		pauseContinue.selectedProperty().bindBidirectional(
				pauseContinue2.selectedProperty());
		
		fastForward2.selectedProperty().bindBidirectional(
				fastForward.selectedProperty());
		
		nextFavoriteDisabledState = new SimpleBooleanProperty(true);
		nextFavorite.disableProperty().bind(nextFavoriteDisabledState);

		updatePlayerButtons(util.getPlayer().stateProperty().get());
		
		Audio audio = util.getConfig().getAudio().getAudio();
		audioBox.getSelectionModel().select(audio);

		String sidDriver = util.getConfig().getAudio().getSidDriver();
		sidDriverBox.getSelectionModel().select(sidDriver);

		devicesBox.setDisable(Audio.NONE.equals(audio));
		samplingBox.setDisable(Audio.NONE.equals(audio));
		samplingRateBox.setDisable(Audio.NONE.equals(audio));

		mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(audio));
		playMP3.setDisable(!Audio.COMPARE_MP3.equals(audio));
		playEmulation.setDisable(!Audio.COMPARE_MP3.equals(audio));

		devices = JavaSound.getDevices();
		devicesBox.setItems(devices);
		int device = util.getConfig().getAudio().getDevice();
		if (device < devices.size()) {
			devicesBox.getSelectionModel().select(device);
		} else {
			devicesBox.getSelectionModel().select(0);
		}

		SamplingMethod sampling = util.getConfig().getAudio().getSampling();
		samplingBox.getSelectionModel().select(sampling);

		Integer samplingRate = Integer.valueOf(util.getConfig().getAudio()
				.getFrequency());
		samplingRateBox.getSelectionModel().select(samplingRate);

		CPUClock videoStandard = CPUClock.getCPUClock(util.getConfig(), util
				.getPlayer().getTune());
		videoStandardBox.getSelectionModel().select(videoStandard);

		hardsid6581Box.getSelectionModel().select(
				Integer.valueOf(util.getConfig().getEmulation()
						.getHardsid6581()));
		hardsid8580Box.getSelectionModel().select(
				Integer.valueOf(util.getConfig().getEmulation()
						.getHardsid8580()));

		Emulation emulation = util.getConfig().getEmulation().getEmulation();
		emulationBox.getSelectionModel().select(emulation);

		hardsid6581Box.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid8580Box.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid6581Label.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid8580Label.setDisable(!Emulation.HARDSID.equals(emulation));

		SidPlay2Section sidplay2 = (SidPlay2Section) util.getConfig()
				.getSidplay2();

		int seconds = sidplay2.getDefaultPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60,
				seconds % 60));
		sidplay2.defaultPlayLengthProperty().addListener(
				(observable, oldValue, newValue) -> defaultTime.setText(String
						.format("%02d:%02d", newValue.intValue() / 60,
								newValue.intValue() % 60)));

		enableSldb.setSelected(sidplay2.isEnableDatabase());
		sidplay2.enableDatabaseProperty().addListener(
				(observable, oldValue, newValue) -> enableSldb
						.setSelected(newValue));

		singleSong.setSelected(sidplay2.isSingle());
		sidplay2.singleProperty().addListener(
				(observable, oldValue, newValue) -> singleSong
						.setSelected(newValue));

		playMP3.setSelected(util.getConfig().getAudio().isPlayOriginal());
		playEmulation
				.setSelected(!util.getConfig().getAudio().isPlayOriginal());

		C1541Section c1541Section = (C1541Section) util.getConfig().getC1541();
		driveOn.selectedProperty().bindBidirectional(
				c1541Section.driveOnProperty());
		parCable.selectedProperty().bindBidirectional(
				c1541Section.parallelCableProperty());
		driveSoundOn.selectedProperty().bindBidirectional(
				c1541Section.driveSoundOnProperty());
		PrinterSection printer = (PrinterSection) util.getConfig().getPrinter();
		turnPrinterOn.selectedProperty().bindBidirectional(
				printer.printerOnProperty());

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

		for (ViewEntity view : util.getConfig().getViews()) {
			addView(view.getFxId());
		}
		this.duringInitialization = false;

		final Duration duration = Duration.millis(1000);
		final KeyFrame frame = new KeyFrame(duration, evt -> setStatusLine());
		timer = new Timeline(frame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	private void addView(String id) {
		if (Video.ID.equals(id)) {
			video();
		} else if (Asm.ID.equals(id)) {
			asm();
		} else if (Oscilloscope.ID.equals(id)) {
			oscilloscope();
		} else if (MusicCollection.HVSC_ID.equals(id)) {
			hvsc();
		} else if (MusicCollection.CGSC_ID.equals(id)) {
			cgsc();
		} else if (DiskCollection.HVMEC_ID.equals(id)) {
			hvmec();
		} else if (DiskCollection.DEMOS_ID.equals(id)) {
			demos();
		} else if (DiskCollection.MAGS_ID.equals(id)) {
			mags();
		} else if (GameBase.ID.equals(id)) {
			gamebase();
		} else if (Favorites.ID.equals(id)) {
			favorites();
		} else if (Printer.ID.equals(id)) {
			printer();
		} else if (Console.ID.equals(id)) {
			console();
		} else if (SidDump.ID.equals(id)) {
			sidDump();
		} else if (SidReg.ID.equals(id)) {
			sidRegisters();
		} else if (Disassembler.ID.equals(id)) {
			disassembler();
		} else if (WebViewType.CSDB.name().equals(id)) {
			csdb();
		} else if (WebViewType.CODEBASE64.name().equals(id)) {
			codebase64();
		} else if (WebViewType.REMIX_KWED_ORG.name().equals(id)) {
			remixKweqOrg();
		} else if (WebViewType.SID_OTH4_COM.name().equals(id)) {
			sidOth4Com();
		} else if (WebViewType.C64_SK.name().equals(id)) {
			c64();
		} else if (WebViewType.FORUM64_DE.name().equals(id)) {
			forum64();
		} else if (WebViewType.LEMON64_COM.name().equals(id)) {
			lemon64();
		} else if (WebViewType.JSIDPLAY2.name().equals(id)) {
			jsidplay2();
		}
	}

	private void updatePlayerButtons(State state) {
		pauseContinue.setSelected(false);
		normalSpeed.setSelected(true);

		PlayList playList = util.getPlayer().getPlayList();

		previous.setDisable(!playList.hasPrevious());
		previous2.setDisable(previous.isDisable());
		next.setDisable(!playList.hasNext());
		next2.setDisable(next.isDisable());

		previous.setText(String.format(util.getBundle().getString("PREVIOUS2")
				+ " (%d/%d)", playList.getPrevious(), playList.getLength()));
		previous2ToolTip.setText(previous.getText());
		next.setText(String.format(util.getBundle().getString("NEXT2")
				+ " (%d/%d)", playList.getNext(), playList.getLength()));
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
			util.getConfig().getSidplay2().setLastDirectory(file.getParent());
			try {
				playTune(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void playVideo() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			final File tmpFile = new File(util.getConfig().getSidplay2()
					.getTmpDir(), "nuvieplayer-v1.0.prg");
			tmpFile.deleteOnExit();
			try (DataOutputStream os = new DataOutputStream(
					new FileOutputStream(tmpFile))) {
				os.write(NUVIE_PLAYER);
				util.getPlayer().insertCartridge(CartridgeType.REU, file);
				util.getPlayer().play(SidTune.load(tmpFile));
			} catch (IOException | SidTuneError e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void reset() {
		if (!duringInitialization) {
			playTune(SidTune.RESET);
		}
	}

	@FXML
	private void quit() {
		close();
		Platform.exit();
	}

	@FXML
	private void pause() {
		util.getPlayer().pause();
	}

	@FXML
	private void previousSong() {
		util.getPlayer().previousSong();
	}

	@FXML
	private void nextSong() {
		util.getPlayer().nextSong();
	}

	@FXML
	private void playNormalSpeed() {
		util.getPlayer().normalSpeed();
	}

	@FXML
	private void playFastForward() {
		util.getPlayer().fastForward();
	}

	@FXML
	private void fastForward() {
		if (util.getPlayer().isFastForward()) {
			util.getPlayer().normalSpeed();
			normalSpeed.setSelected(true);
		} else {
			util.getPlayer().fastForward();
			fastForward.setSelected(true);
		}
	}
	
	@FXML
	private void nextFavorite() {
		util.getPlayer().getTimer().end();
	}

	@FXML
	private void stopSong() {
		util.getPlayer().quit();
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
	private void setVideoStandard() {
		CPUClock videoStandard = videoStandardBox.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulation().setDefaultClockSpeed(videoStandard);
		restart();
	}

	@FXML
	private void proxySettings() {
		new ProxySettings(util.getPlayer()).open();
	}

	@FXML
	private void emulationSettings() {
		new EmulationSettings(util.getPlayer()).open();
	}

	@FXML
	private void joystickSettings() {
		new JoystickSettings(util.getPlayer()).open();
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
			try {
				util.getPlayer().insertTape(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
						file.getAbsolutePath()));
			}
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
		util.getConfig().getC1541().setDriveOn(driveOn.isSelected());
		util.getPlayer().enableFloppyDiskDrives(driveOn.isSelected());
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
		util.getConfig().getC1541()
				.setRamExpansionEnabled0(expand2000.isSelected());
	}

	@FXML
	private void expansion0x4000() {
		getFirstFloppy().setRamExpansion(1, expand4000.isSelected());
		util.getConfig().getC1541()
				.setRamExpansionEnabled1(expand4000.isSelected());
	}

	@FXML
	private void expansion0x6000() {
		getFirstFloppy().setRamExpansion(2, expand6000.isSelected());
		util.getConfig().getC1541()
				.setRamExpansionEnabled2(expand6000.isSelected());
	}

	@FXML
	private void expansion0x8000() {
		getFirstFloppy().setRamExpansion(3, expand8000.isSelected());
		util.getConfig().getC1541()
				.setRamExpansionEnabled3(expand8000.isSelected());
	}

	@FXML
	private void expansion0xA000() {
		getFirstFloppy().setRamExpansion(4, expandA000.isSelected());
		util.getConfig().getC1541()
				.setRamExpansionEnabled4(expandA000.isSelected());
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
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert media file '%s'.",
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
	private void printerOn() {
		util.getConfig().getPrinter().setPrinterOn(turnPrinterOn.isSelected());
		util.getPlayer().enablePrinter(turnPrinterOn.isSelected());
	}

	@FXML
	private void insertCartridge() {
		chooseCartridge(CartridgeType.CRT);
	}

	@FXML
	private void insertGeoRAM() {
		chooseCartridge(CartridgeType.GEORAM);
	}

	@FXML
	private void insertGeoRAM64() {
		insertCartridge(CartridgeType.GEORAM, 64);
	}

	@FXML
	private void insertGeoRAM128() {
		insertCartridge(CartridgeType.GEORAM, 128);
	}

	@FXML
	private void insertGeoRAM256() {
		insertCartridge(CartridgeType.GEORAM, 256);
	}

	@FXML
	private void insertGeoRAM512() {
		insertCartridge(CartridgeType.GEORAM, 512);
	}

	@FXML
	private void insertGeoRAM1024() {
		insertCartridge(CartridgeType.GEORAM, 1024);
	}

	@FXML
	private void insertGeoRAM2048() {
		insertCartridge(CartridgeType.GEORAM, 2048);
	}

	@FXML
	private void insertREU() {
		chooseCartridge(CartridgeType.REU);
	}

	@FXML
	private void insertREU128() {
		insertCartridge(CartridgeType.REU, 128);
	}

	@FXML
	private void insertREU256() {
		insertCartridge(CartridgeType.REU, 256);
	}

	@FXML
	private void insertREU512() {
		insertCartridge(CartridgeType.REU, 512);
	}

	@FXML
	private void insertREU2048() {
		insertCartridge(CartridgeType.REU, 2048);
	}

	@FXML
	private void insertREU16384() {
		insertCartridge(CartridgeType.REU, 16384);
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
			util.getConfig().getSidplay2()
					.setLastDirectory(c64kernalFile.getParent());
			final FileChooser c1541FileDialog = new FileChooser();
			c1541FileDialog.setTitle(util.getBundle().getString(
					"CHOOSE_C1541_KERNAL_ROM"));
			c1541FileDialog.setInitialDirectory(((SidPlay2Section) (util
					.getConfig().getSidplay2())).getLastDirectoryFolder());
			c1541FileDialog.getExtensionFilters().add(
					new ExtensionFilter(RomFileExtensions.DESCRIPTION,
							RomFileExtensions.EXTENSIONS));
			final File c1541kernalFile = c1541FileDialog.showOpenDialog(scene
					.getWindow());
			if (c1541kernalFile != null) {
				util.getConfig().getSidplay2()
						.setLastDirectory(c1541kernalFile.getParent());
				try {
					util.getPlayer().installJiffyDOS(c64kernalFile,
							c1541kernalFile);
					util.getPlayer().play(SidTune.RESET);
				} catch (IOException | SidTuneError ex) {
					ex.printStackTrace();
				}
			}
		}
	}

	@FXML
	private void uninstallJiffyDos() {
		util.getPlayer().uninstallJiffyDOS();
		reset();
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
	private void setAudio() {
		Audio audio = audioBox.getSelectionModel().getSelectedItem();
		util.getConfig().getAudio().setAudio(audio);

		devicesBox.setDisable(Audio.NONE.equals(audio));
		samplingBox.setDisable(Audio.NONE.equals(audio));
		samplingRateBox.setDisable(Audio.NONE.equals(audio));

		mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(audio));
		playMP3.setDisable(!Audio.COMPARE_MP3.equals(audio));
		playEmulation.setDisable(!Audio.COMPARE_MP3.equals(audio));

		util.getPlayer().updateDriverSettings();
		restart();
	}

	@FXML
	private void setSIDDriver() {
		String sidDriver = sidDriverBox.getSelectionModel().getSelectedItem();
		util.getConfig().getAudio().setSidDriver(sidDriver);
		SidTune.useDriver(sidDriver);
		restart();
	}

	@FXML
	public void setDevice() {
		Device device = devicesBox.getSelectionModel().getSelectedItem();
		int deviceIndex = devicesBox.getItems().indexOf(device);
		util.getConfig().getAudio().setDevice(deviceIndex);
		if (!this.duringInitialization) {
			JavaSound js = (JavaSound) Audio.SOUNDCARD.getAudioDriver();
			try {
				js.setAudioDevice(device.getInfo());
			} catch (LineUnavailableException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@FXML
	private void setSampling() {
		SamplingMethod sampling = samplingBox.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getAudio().setSampling(sampling);
	}

	@FXML
	private void setSamplingRate() {
		Integer samplingRate = samplingRateBox.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getAudio().setFrequency(samplingRate);
		restart();
	}

	@FXML
	private void setEmulation() {
		Emulation emulation = emulationBox.getSelectionModel()
				.getSelectedItem();
		util.getConfig().getEmulation().setEmulation(emulation);
		if (Emulation.HARDSID.equals(emulation)) {
			audioBox.getSelectionModel().select(Audio.NONE);
		} else if (Audio.NONE.equals(audioBox.getSelectionModel()
				.getSelectedItem())) {
			audioBox.getSelectionModel().select(Audio.SOUNDCARD);
		}
		hardsid6581Box.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid8580Box.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid6581Label.setDisable(!Emulation.HARDSID.equals(emulation));
		hardsid8580Label.setDisable(!Emulation.HARDSID.equals(emulation));
		util.getPlayer().updateDriverSettings();
		restart();
	}

	@FXML
	private void setSid6581() {
		int hardsid6581 = hardsid6581Box.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulation().setHardsid6581(hardsid6581);
		restart();
	}

	@FXML
	private void setSid8580() {
		int hardsid8580 = hardsid8580Box.getSelectionModel().getSelectedItem();
		util.getConfig().getEmulation().setHardsid8580(hardsid8580);
		restart();
	}

	@FXML
	private void doEnableSldb() {
		util.getConfig().getSidplay2()
				.setEnableDatabase(enableSldb.isSelected());
		util.getPlayer().getTimer().updateEnd();
	}

	@FXML
	private void playSingleSong() {
		util.getConfig().getSidplay2().setSingle(singleSong.isSelected());
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			util.getConfig().getSidplay2().setDefaultPlayLength(secs);
			util.getPlayer().getTimer().updateEnd();
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
			defaultTime.setTooltip(tooltip);
			defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void playEmulatedSound() {
		setPlayOriginal(false);
	}

	@FXML
	private void playRecordedSound() {
		setPlayOriginal(true);
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				"MP3 file (*.mp3)", "*.mp3");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getAudio().setMp3File(file.getAbsolutePath());
			restart();
		}
	}

	@FXML
	private void showVolume() {
		ISidPlay2Section section = util.getConfig().getSidplay2();
		int x = section.getFrameX() + section.getFrameWidth() / 2;
		try {
			Runtime.getRuntime().exec("sndvol -f " + x);
		} catch (IOException e) {
			try {
				Runtime.getRuntime().exec("sndvol32");
			} catch (IOException e1) {
				try {
					Runtime.getRuntime().exec("kmix");
				} catch (IOException e2) {
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(
							"Windows or Linux (KDE) only!"));
					System.err.println("sndvol or sndvol32 or kmix not found!");
				}
			}
		}
	}

	@FXML
	private void video() {
		if (!tabAlreadyOpen(Video.ID)) {
			addTab(new Video(this, util.getPlayer()));
		}
	}

	@FXML
	private void oscilloscope() {
		if (!tabAlreadyOpen(Oscilloscope.ID)) {
			addTab(new Oscilloscope(this, util.getPlayer()));
		}
	}

	@FXML
	private void hvsc() {
		if (!tabAlreadyOpen(MusicCollection.HVSC_ID)) {
			MusicCollection tab = new MusicCollection(this, util.getPlayer());
			tab.setType(MusicCollectionType.HVSC);
			addTab(tab);
		}
	}

	@FXML
	private void cgsc() {
		if (!tabAlreadyOpen(MusicCollection.CGSC_ID)) {
			MusicCollection tab = new MusicCollection(this, util.getPlayer());
			tab.setType(MusicCollectionType.CGSC);
			addTab(tab);
		}
	}

	@FXML
	private void hvmec() {
		if (!tabAlreadyOpen(DiskCollection.HVMEC_ID)) {
			DiskCollection tab = new DiskCollection(this, util.getPlayer());
			tab.setType(DiskCollectionType.HVMEC);
			addTab(tab);
		}
	}

	@FXML
	private void demos() {
		if (!tabAlreadyOpen(DiskCollection.DEMOS_ID)) {
			DiskCollection tab = new DiskCollection(this, util.getPlayer());
			tab.setType(DiskCollectionType.DEMOS);
			addTab(tab);
		}
	}

	@FXML
	private void mags() {
		if (!tabAlreadyOpen(DiskCollection.MAGS_ID)) {
			DiskCollection tab = new DiskCollection(this, util.getPlayer());
			tab.setType(DiskCollectionType.MAGS);
			addTab(tab);
		}
	}

	@FXML
	private void favorites() {
		if (!tabAlreadyOpen(Favorites.ID)) {
			addTab(new Favorites(this, util.getPlayer()));
		}
	}

	@FXML
	private void gamebase() {
		if (!tabAlreadyOpen(GameBase.ID)) {
			addTab(new GameBase(this, util.getPlayer()));
		}
	}

	@FXML
	private void asm() {
		if (!tabAlreadyOpen(Asm.ID)) {
			addTab(new Asm(this, util.getPlayer()));
		}
	}

	@FXML
	private void printer() {
		if (!tabAlreadyOpen(Printer.ID)) {
			addTab(new Printer(this, util.getPlayer()));
		}
	}

	@FXML
	private void console() {
		if (!tabAlreadyOpen(Console.ID)) {
			addTab(new Console(this, util.getPlayer()));
		}
	}

	@FXML
	private void sidDump() {
		if (!tabAlreadyOpen(SidDump.ID)) {
			addTab(new SidDump(this, util.getPlayer()));
		}
	}

	@FXML
	private void sidRegisters() {
		if (!tabAlreadyOpen(SidReg.ID)) {
			addTab(new SidReg(this, util.getPlayer()));
		}
	}

	@FXML
	private void disassembler() {
		if (!tabAlreadyOpen(Disassembler.ID)) {
			addTab(new Disassembler(this, util.getPlayer()));
		}
	}

	@FXML
	private void csdb() {
		if (!tabAlreadyOpen(WebViewType.CSDB.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.CSDB);
			addTab(tab);
		}
	}

	@FXML
	private void codebase64() {
		if (!tabAlreadyOpen(WebViewType.CODEBASE64.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.CODEBASE64);
			addTab(tab);
		}
	}

	@FXML
	private void remixKweqOrg() {
		if (!tabAlreadyOpen(WebViewType.REMIX_KWED_ORG.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.REMIX_KWED_ORG);
			addTab(tab);
		}
	}

	@FXML
	private void sidOth4Com() {
		if (!tabAlreadyOpen(WebViewType.SID_OTH4_COM.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.SID_OTH4_COM);
			addTab(tab);
		}
	}

	@FXML
	private void c64() {
		if (!tabAlreadyOpen(WebViewType.C64_SK.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.C64_SK);
			addTab(tab);
		}
	}

	@FXML
	private void forum64() {
		if (!tabAlreadyOpen(WebViewType.FORUM64_DE.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.FORUM64_DE);
			addTab(tab);
		}
	}

	@FXML
	private void lemon64() {
		if (!tabAlreadyOpen(WebViewType.LEMON64_COM.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.LEMON64_COM);
			addTab(tab);
		}
	}

	@FXML
	private void jsidplay2() {
		if (!tabAlreadyOpen(WebViewType.JSIDPLAY2.name())) {
			WebView tab = new WebView(this, util.getPlayer());
			tab.setType(WebViewType.JSIDPLAY2);
			addTab(tab);
		}
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
					PathUtils.getBaseNameNoExt(file.getName()) + ".xml");
			util.getConfig().getSidplay2().setLastDirectory(file.getParent());
			configService.exportCfg(util.getConfig(), target);
		}
	}

	@FXML
	private void importConfiguration() {
		YesNoDialog dialog = new YesNoDialog(util.getPlayer());
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
				util.getConfig().getSidplay2()
						.setLastDirectory(file.getParent());
				util.getConfig().setReconfigFilename(file.getAbsolutePath());
			}
		}
	}

	@FXML
	private void updateCheck() {
		new Update(util.getPlayer()).open();
	}

	@FXML
	private void about() {
		new About(util.getPlayer()).open();
	}

	private void addTab(Tab tab) {
		final List<ViewEntity> views = util.getConfig().getViews();
		if (!views.stream()
				.anyMatch(tool -> tool.getFxId().equals(tab.getId()))) {
			views.add(new ViewEntity(tab.getId()));
		}
		tab.setOnClosed((evt) -> {
			close((UIPart) tab);
			views.removeIf((view) -> view.getFxId().equals(tab.getId()));
		});
		tabbedPane.getTabs().add(tab);
		tabbedPane.getSelectionModel().select(tab);
	}

	private boolean tabAlreadyOpen(String fxId) {
		return tabbedPane.getTabs().stream()
				.anyMatch(tab -> tab.getId().equals(fxId));
	}

	private void chooseCartridge(final CartridgeType type) {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(
				new ExtensionFilter(CartFileExtensions.DESCRIPTION,
						CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(type, file);
				util.getPlayer().play(SidTune.RESET);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format(
						"Cannot insert file '%s' as cartridge of type '%s'.",
						file.getAbsolutePath(), type.name()));
			}
		}
	}

	private void insertCartridge(CartridgeType type, int sizeKB) {
		try {
			util.getPlayer().insertCartridge(type, sizeKB);
			util.getPlayer().play(SidTune.RESET);
		} catch (IOException | SidTuneError ex) {
			System.err.println(String.format(
					"Cannot insert cartridge of type '%s' and size '%d'KB.",
					type.name(), sizeKB));
		}
	}

	private void playTune(final SidTune tune) {
		util.setPlayingTab(tabbedPane.getTabs().stream()
				.filter((tab) -> tab.getId().equals(Video.ID)).findFirst()
				.get());
		util.getPlayer().play(tune);
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	protected void setStatusLine() {
		// Get status information of the first disk drive
		final C1541 c1541 = getFirstFloppy();
		// Disk motor status
		boolean motorOn = util.getConfig().getC1541().isDriveSoundOn()
				&& util.getPlayer().stateProperty().get() == State.RUNNING
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
		determineTuneSpeed();
		// final status bar text
		StringBuilder line = new StringBuilder();
		line.append(String.format("%s: %s, ",
				util.getBundle().getString("RELEASE"), DATE));
		line.append(determineVideoNorm());
		line.append(determineChipModel());
		line.append(playerId);
		line.append(tuneSpeed);
		line.append(determineSong());
		if (datasette.getMotor()) {
			line.append(String.format("%s: %03d, ",
					util.getBundle().getString("DATASETTE_COUNTER"),
					datasette.getCounter()));
		}
		if (c1541.getDiskController().isMotorOn()) {
			line.append(String.format("%s: %02d, ",
					util.getBundle().getString("FLOPPY_TRACK"), halfTrack >> 1));
		}
		line.append(String.format("%s: %s%s", util.getBundle()
				.getString("TIME"), determinePlayTime(), determineSongLength()));
		status.setText(line.toString());
	}

	private String determineChipModel() {
		StringBuilder line = new StringBuilder();
		ChipModel chipModel = ChipModel.getChipModel(util.getConfig(), util
				.getPlayer().getTune());
		line.append(String.format("%s", chipModel));
		if (AudioConfig.isStereo(util.getConfig(), util.getPlayer().getTune())) {
			ChipModel stereoModel = ChipModel.getStereoModel(util.getConfig(),
					util.getPlayer().getTune());
			int dualSidBase = AudioConfig.getStereoAddress(util.getConfig(),
					util.getPlayer().getTune());
			line.append(String
					.format("+%s(at 0x%4x)", stereoModel, dualSidBase));
		}
		line.append(", ");
		return line.toString();
	}

	private String determineVideoNorm() {
		return String.format(
				"%s, ",
				CPUClock.getCPUClock(util.getConfig(),
						util.getPlayer().getTune()).name());
	}

	private String determineSong() {
		SidTune tune = util.getPlayer().getTune();
		if (tune != null) {
			SidTuneInfo info = tune.getInfo();
			if (info.getSongs() > 1) {
				return String.format("%s: %d/%d, ",
						util.getBundle().getString("SONG"),
						info.getCurrentSong(), info.getSongs());
			}
		}
		return "";
	}

	private String determinePlayTime() {
		int time = util.getPlayer().time();
		return String.format("%02d:%02d", time / 60 % 100, time % 60);
	}

	private String determineSongLength() {
		SidTune tune = util.getPlayer().getTune();
		int songLength = tune != null ? util.getPlayer().getSidDatabaseInfo(
				db -> db.getSongLength(tune)) : 0;
		if (songLength > 0) {
			// song length well-known?
			return String.format("/%02d:%02d", (songLength / 60 % 100),
					(songLength % 60));
		}
		return "";
	}

	private void getPlayerId() {
		playerId.setLength(0);
		if (util.getPlayer().getTune() != null) {
			for (final String id : util.getPlayer().getTune().identify()) {
				playerId.append(util.getBundle().getString("PLAYER_ID"))
						.append(": ").append(id);
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
						String.format(": %.1fx, ", callsSinceLastRead
								/ systemClock.getRefresh()));
			}
		} else {
			tuneSpeed.setLength(0);
		}
	}

	private void createHardCopy(String format) {
		video();
		try {
			Video videoScreen = (Video) tabbedPane.getTabs().stream()
					.filter((tab) -> tab.getId().equals(Video.ID)).findFirst()
					.get();
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

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	private void setPlayOriginal(final boolean playOriginal) {
		util.getConfig().getAudio().setPlayOriginal(playOriginal);
		if (util.getConfig().getAudio().getAudio().getAudioDriver() instanceof CmpMP3File) {
			((CmpMP3File) util.getConfig().getAudio().getAudio()
					.getAudioDriver()).setPlayOriginal(playOriginal);
		}
	}

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			// EXTEND_ASK
			YesNoDialog dialog = new YesNoDialog(util.getPlayer());
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

	@Override
	public String getFilename(SidTune tune) {
		String defaultName = "jsidplay2";
		if (tune == null) {
			return new File(util.getConfig().getSidplay2().getTmpDir(),
					defaultName).getAbsolutePath();
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> infos = info.getInfoString().iterator();
		String name = infos.hasNext() ? infos.next().replaceAll(
				"[:\\\\/*?|<>]", "_") : defaultName;
		String filename = new File(util.getConfig().getSidplay2().getTmpDir(),
				PathUtils.getBaseNameNoExt(name)).getAbsolutePath();
		if (info.getSongs() > 1) {
			filename += String.format("-%02d", info.getCurrentSong());
		}
		return filename;
	}

}
