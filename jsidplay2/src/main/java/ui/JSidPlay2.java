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
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Function;

import javax.imageio.ImageIO;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
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
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;
import libsidplay.C64;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.WebUtils;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.audio.JavaSound.Device;
import sidplay.ini.IniReader;
import sidplay.player.PlayList;
import sidplay.player.State;
import ui.about.About;
import ui.asm.Asm;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.UIPart;
import ui.common.dialog.AlertDialog;
import ui.common.dialog.YesNoDialog;
import ui.console.Console;
import ui.disassembler.Disassembler;
import ui.diskcollection.DiskCollection;
import ui.diskcollection.DiskCollectionType;
import ui.emulationsettings.EmulationSettings;
import ui.entities.config.AudioSection;
import ui.entities.config.C1541Section;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.PrinterSection;
import ui.entities.config.SidPlay2Section;
import ui.entities.config.ViewEntity;
import ui.favorites.Favorites;
import ui.favorites.PlaybackType;
import ui.filefilter.CartFileExtensions;
import ui.filefilter.DiskFileExtensions;
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

public class JSidPlay2 extends C64Window implements IExtendImageListener, Function<SidTune, String> {

	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/track.wav").toString());

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	/** Build date calculated from our own modify time */
	private static String DATE = "unknown";
	/** NUVIE video player */
	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static byte[] NUVIE_PLAYER;
	/** Empty disk image */
	private static final String EMPTY_D64 = "/libsidplay/components/c1541/empty.d64";
	private static byte[] EMPTY_DISK;

	static {
		try (DataInputStream is = new DataInputStream(JSidPlay2.class.getResourceAsStream(NUVIE_PLAYER_PRG));
				DataInputStream is2 = new DataInputStream(JSidPlay2.class.getResourceAsStream(EMPTY_D64))) {
			URL us = JSidPlay2Main.class.getResource("/" + JSidPlay2.class.getName().replace('.', '/') + ".class");
			Date date = new Date(us.openConnection().getLastModified());
			DATE = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);

			URL us2 = JSidPlay2Main.class.getResource(NUVIE_PLAYER_PRG);
			NUVIE_PLAYER = new byte[us2.openConnection().getContentLength()];
			is.readFully(NUVIE_PLAYER);

			URL us3 = JSidPlay2Main.class.getResource(EMPTY_D64);
			EMPTY_DISK = new byte[us3.openConnection().getContentLength()];
			is2.readFully(EMPTY_DISK);

			MOTORSOUND_AUDIOCLIP.setCycleCount(AudioClip.INDEFINITE);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@FXML
	protected CheckMenuItem pauseContinue, driveOn, driveSoundOn, parCable, expand2000, expand4000, expand6000,
			expand8000, expandA000, turnPrinterOn;
	@FXML
	protected RadioMenuItem fastForward, normalSpeed, c1541, c1541_II, neverExtend, askExtend, accessExtend;
	@FXML
	protected MenuItem previous, next;
	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box;
	@FXML
	private ComboBox<SamplingRate> samplingRateBox;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private ComboBox<Device> devicesBox;
	@FXML
	private ComboBox<Engine> engineBox;
	@FXML
	private CheckBox enableSldb, singleSong;
	@FXML
	private TextField defaultTime;
	@FXML
	private ToggleButton pauseContinue2, fastForward2;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	ToggleGroup playSourceGroup;
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

	private Scene scene;
	private Timeline timer;
	private int oldHalfTrack, hardcopyCounter;
	private boolean duringInitialization, oldMotorOn;
	private StringBuilder playerId, playerinfos;
	private BooleanProperty nextFavoriteDisabledState;
	private Tooltip statusTooltip;
	private StateChangeListener nextTuneListener;

	private class StateChangeListener implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> nextFavoriteDisabledState.set(sidTune == SidTune.RESET || newValue == State.QUIT));
			if (newValue == State.START) {
				Platform.runLater(() -> {
					updatePlayerButtons(newValue);

					setPlayerIdAndInfos();

					final Tab selectedItem = tabbedPane.getSelectionModel().getSelectedItem();
					boolean doNotSwitch = selectedItem != null
							&& (MusicCollection.class.isAssignableFrom(selectedItem.getClass())
									|| Favorites.class.isAssignableFrom(selectedItem.getClass()));
					if (sidTune == SidTune.RESET || (sidTune.getInfo().getPlayAddr() == 0 && !doNotSwitch)) {
						video();
					}
				});
			} else if (newValue.equals(State.END)) {
				Platform.runLater(() -> {
					SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig().getSidplay2Section();
					PlaybackType pt = sidPlay2Section.getPlaybackType();

					if (!sidPlay2Section.isLoop()) {
						if (pt == PlaybackType.RANDOM_HVSC) {
							playNextRandomHVSC();
						}
					}
				});
			}
		}
	}

	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	private void initialize() {
		this.duringInitialization = true;
		getStage().setTitle(util.getBundle().getString("TITLE")
				+ String.format(", %s: %s", util.getBundle().getString("RELEASE"), DATE));

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = (SidPlay2Section) config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();
		final C1541Section c1541Section = config.getC1541Section();
		final PrinterSection printer = (PrinterSection) config.getPrinterSection();

		this.playerId = new StringBuilder();
		this.playerinfos = new StringBuilder();
		this.scene = tabbedPane.getScene();
		this.statusTooltip = new Tooltip();
		this.nextTuneListener = new StateChangeListener();
		this.status.setOnMouseClicked(e -> {
			if (status.getUserData() != null)
				WebUtils.browse(status.getUserData().toString());
		});

		util.getPlayer().setRecordingFilenameProvider(this);
		util.getPlayer().setExtendImagePolicy(this);
		util.getPlayer().stateProperty().addListener(nextTuneListener);

		audioBox.setConverter(new EnumToString<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(Audio.SOUNDCARD, Audio.LIVE_WAV, Audio.LIVE_MP3,
				Audio.COMPARE_MP3));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setItems(JavaSound.getDevices());
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToString<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToString<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		samplingRateBox.valueProperty().bindBidirectional(audioSection.samplingRateProperty());

		videoStandardBox.setConverter(new EnumToString<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());

		engineBox.setConverter(new EnumToString<Engine>(bundle));
		engineBox.setItems(FXCollections.<Engine>observableArrayList(Engine.values()));
		engineBox.valueProperty().addListener((obj, o, n) -> {
			hardsid6581Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid6581Label.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Label.setDisable(!Engine.HARDSID.equals(n));
		});
		engineBox.valueProperty().bindBidirectional(emulationSection.engineProperty());

		int seconds = sidplay2Section.getDefaultPlayLength();
		defaultTime.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
		sidplay2Section.defaultPlayLengthProperty().addListener((observable, oldValue, newValue) -> defaultTime
				.setText(String.format("%02d:%02d", newValue.intValue() / 60, newValue.intValue() % 60)));

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		updatePlayerButtons(util.getPlayer().stateProperty().get());

		pauseContinue.selectedProperty().bindBidirectional(pauseContinue2.selectedProperty());
		fastForward2.selectedProperty().bindBidirectional(fastForward.selectedProperty());
		nextFavoriteDisabledState = new SimpleBooleanProperty(true);
		nextFavorite.disableProperty().bind(nextFavoriteDisabledState);
		driveOn.selectedProperty().bindBidirectional(c1541Section.driveOnProperty());
		parCable.selectedProperty().bindBidirectional(c1541Section.parallelCableProperty());
		driveSoundOn.selectedProperty().bindBidirectional(c1541Section.driveSoundOnProperty());
		turnPrinterOn.selectedProperty().bindBidirectional(printer.printerOnProperty());

		FloppyType floppyType = c1541Section.getFloppyType();
		(floppyType == FloppyType.C1541 ? c1541 : c1541_II).setSelected(true);
		ExtendImagePolicy extendImagePolicy = c1541Section.getExtendImagePolicy();
		(extendImagePolicy == ExtendImagePolicy.EXTEND_NEVER ? neverExtend
				: extendImagePolicy == ExtendImagePolicy.EXTEND_ASK ? askExtend : accessExtend).setSelected(true);
		expand2000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled0Property());
		expand4000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled1Property());
		expand6000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled2Property());
		expand8000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled3Property());
		expandA000.selectedProperty().bindBidirectional(c1541Section.ramExpansionEnabled4Property());

		for (ViewEntity view : config.getViews()) {
			Platform.runLater(() -> addView(view.getFxId()));
		}
		this.duringInitialization = false;

		final Duration duration = Duration.millis(1000);
		final KeyFrame frame = new KeyFrame(duration, evt -> setStatusLine());
		timer = new Timeline(frame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
		Platform.runLater(() -> {
			scene.setOnDragOver(event -> {
				Dragboard db = event.getDragboard();
				if (db.hasFiles()) {
					event.acceptTransferModes(TransferMode.COPY);
				} else {
					event.consume();
				}
			});
			scene.setOnDragDropped(event -> {
				Dragboard db = event.getDragboard();
				boolean success = false;
				if (db.hasFiles()) {
					success = true;
					List<File> files = db.getFiles();
					try {
						playTune(SidTune.load(files.get(0)));
					} catch (IOException | SidTuneError e) {
						openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
					}
				}
				event.setDropCompleted(success);
				event.consume();
			});
		});
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(nextTuneListener);
		timer.stop();
		stopSong();
		Platform.exit();
	}

	@FXML
	private void load() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TuneFileExtensions.DESCRIPTION, TuneFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			try {
				playTune(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	@FXML
	private void playVideo() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			final File tmpFile = new File(util.getConfig().getSidplay2Section().getTmpDir(), "nuvieplayer-v1.0.prg");
			tmpFile.deleteOnExit();
			try (DataOutputStream os = new DataOutputStream(new FileOutputStream(tmpFile))) {
				os.write(NUVIE_PLAYER);
				util.getPlayer().insertCartridge(CartridgeType.REU, file);
				util.getPlayer().play(SidTune.load(tmpFile));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
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
	}

	@FXML
	private void pause() {
		util.getPlayer().pauseContinue();
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
		util.getPlayer().configureMixer(mixer -> mixer.normalSpeed());
	}

	@FXML
	private void playFastForward() {
		util.getPlayer().configureMixer(mixer -> mixer.fastForward());
	}

	@FXML
	private void fastForward() {
		if (util.getPlayer().getMixerInfo(mixer -> mixer.isFastForward(), false)) {
			util.getPlayer().configureMixer(mixer -> mixer.normalSpeed());
			normalSpeed.setSelected(true);
		} else {
			util.getPlayer().configureMixer(mixer -> mixer.fastForward());
			fastForward.setSelected(true);
		}
	}

	@FXML
	private void nextFavorite() {
		final C64 c64 = util.getPlayer().getC64();
		final EventScheduler ctx = c64.getEventScheduler();
		ctx.scheduleThreadSafe(new Event("Timer End To Play Next Favorite!") {
			@Override
			public void event() {
				if (util.getPlayer().stateProperty().get() == State.PLAY) {
					util.getPlayer().getTimer().end();
				}
			}
		});
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
		util.getPlayer().getDatasette().control(Datasette.Control.RESET_COUNTER);
	}

	@FXML
	private void insertTape() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(TapeFileExtensions.DESCRIPTION, TapeFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_TAPE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertTape(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void ejectTape() {
		try {
			util.getPlayer().getDatasette().ejectTape();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void turnDriveOn() {
		util.getPlayer().enableFloppyDiskDrives(util.getConfig().getC1541Section().isDriveOn());
	}

	@FXML
	private void parallelCable() {
		util.getPlayer().connectC64AndC1541WithParallelCable(util.getConfig().getC1541Section().isParallelCable());
	}

	@FXML
	private void floppyTypeC1541() {
		getFirstFloppy().setFloppyType(FloppyType.C1541);
		util.getConfig().getC1541Section().setFloppyType(FloppyType.C1541);
	}

	@FXML
	private void floppyTypeC1541_II() {
		getFirstFloppy().setFloppyType(FloppyType.C1541_II);
		util.getConfig().getC1541Section().setFloppyType(FloppyType.C1541_II);
	}

	@FXML
	private void extendNever() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_NEVER);
	}

	@FXML
	private void extendAsk() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_ASK);
	}

	@FXML
	private void extendAccess() {
		util.getConfig().getC1541Section().setExtendImagePolicy(ExtendImagePolicy.EXTEND_ACCESS);
	}

	@FXML
	private void expansion0x2000() {
		getFirstFloppy().setRamExpansion(0, util.getConfig().getC1541Section().isRamExpansionEnabled0());
	}

	@FXML
	private void expansion0x4000() {
		getFirstFloppy().setRamExpansion(1, util.getConfig().getC1541Section().isRamExpansionEnabled1());
	}

	@FXML
	private void expansion0x6000() {
		getFirstFloppy().setRamExpansion(2, util.getConfig().getC1541Section().isRamExpansionEnabled2());
	}

	@FXML
	private void expansion0x8000() {
		getFirstFloppy().setRamExpansion(3, util.getConfig().getC1541Section().isRamExpansionEnabled3());
	}

	@FXML
	private void expansion0xA000() {
		getFirstFloppy().setRamExpansion(4, util.getConfig().getC1541Section().isRamExpansionEnabled4());
	}

	@FXML
	private void insertDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(DiskFileExtensions.DESCRIPTION, DiskFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_DISK"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertDisk(file);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
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
	private void insertEmptyDisk() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters().add(new ExtensionFilter("Disk Image (D64)", "*.d64"));
		fileDialog.setTitle(util.getBundle().getString("INSERT_EMPTY_DISK"));
		final File file = fileDialog.showSaveDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParent());
			File target = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()) + ".d64");
			try (DataOutputStream os = new DataOutputStream(new FileOutputStream(target))) {
				os.write(EMPTY_DISK);
			} catch (IOException e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_WRITE_ERROR"), e.getMessage()));
			}
			video();
			try {
				util.getPlayer().insertDisk(target);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert media file '%s'.", target.getAbsolutePath()));
			}
		}
	}

	@FXML
	private void printerOn() {
		util.getPlayer().enablePrinter(util.getConfig().getPrinterSection().isPrinterOn());
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
		util.getPlayer().installJiffyDOS();
		reset();
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
		restart();
	}

	@FXML
	public void setDevice() {
		int deviceIndex = devicesBox.getSelectionModel().getSelectedIndex();
		util.getConfig().getAudioSection().setDevice(deviceIndex);
		restart();
	}

	@FXML
	private void setSampling() {
		restart();
	}

	@FXML
	private void setSamplingRate() {
		restart();
	}

	@FXML
	private void setEngine() {
		restart();
	}

	@FXML
	private void setSid6581() {
		restart();
	}

	@FXML
	private void setSid8580() {
		restart();
	}

	@FXML
	private void doEnableSldb() {
		final C64 c64 = util.getPlayer().getC64();
		final EventScheduler ctx = c64.getEventScheduler();
		ctx.scheduleThreadSafe(new Event("Update Play Timer!") {
			@Override
			public void event() {
				util.getPlayer().getTimer().updateEnd();
			}
		});
	}

	@FXML
	private void setDefaultTime() {
		final Tooltip tooltip = new Tooltip();
		defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
		final int secs = IniReader.parseTime(defaultTime.getText());
		if (secs != -1) {
			util.getConfig().getSidplay2Section().setDefaultPlayLength(secs);
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
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MP3 file (*.mp3)", "*.mp3");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getAudioSection().setMp3File(file.getAbsolutePath());
			restart();
		}
	}

	@FXML
	private void showVolume() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			SidPlay2Section section = util.getConfig().getSidplay2Section();
			int x = section.getFrameX() + section.getFrameWidth() / 2;
			try {
				Runtime.getRuntime().exec("sndvol -f " + x);
			} catch (IOException e) {
				try {
					Runtime.getRuntime().exec("sndvol32");
				} catch (IOException e1) {
					String toolTip = "For Windows: sndvol or sndvol32 not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("nux") >= 0) {
			try {
				Runtime.getRuntime().exec("pavucontrol");
			} catch (IOException e2) {
				try {
					Runtime.getRuntime().exec("kmix");
				} catch (IOException e3) {
					String toolTip = "For Linux: pavucontrol(PulseAudio) or kmix(ALSA) not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("mac") >= 0) {
			String toolTip = "For OSX: N.Y.I!";
			volumeButton.setDisable(true);
			volumeButton.setTooltip(new Tooltip(toolTip));
			System.err.println(toolTip);
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
	private void updateCheck() {
		new Update(util.getPlayer()).open();
	}

	@FXML
	private void about() {
		new About(util.getPlayer()).open();
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

		previous.setText(String.format(util.getBundle().getString("PREVIOUS2") + " (%d/%d)", playList.getPrevious(),
				playList.getLength()));
		previous2ToolTip.setText(previous.getText());
		next.setText(String.format(util.getBundle().getString("NEXT2") + " (%d/%d)", playList.getNext(),
				playList.getLength()));
		next2ToolTip.setText(next.getText());
	}

	private void addTab(Tab tab) {
		final List<ViewEntity> views = util.getConfig().getViews();
		if (!views.stream().anyMatch(tool -> tool.getFxId().equals(tab.getId()))) {
			views.add(new ViewEntity(tab.getId()));
		}
		tab.setOnClosed(evt -> {
			close((UIPart) tab);
			views.removeIf(view -> view.getFxId().equals(tab.getId()));
		});
		tabbedPane.getTabs().add(tab);
		tabbedPane.getSelectionModel().select(tab);
	}

	private boolean tabAlreadyOpen(String fxId) {
		Optional<Tab> alreadyOpened = tabbedPane.getTabs().stream().filter(tab -> tab.getId().equals(fxId)).findFirst();
		if (alreadyOpened.isPresent()) {
			tabbedPane.getSelectionModel().select(alreadyOpened.get());
		}
		return alreadyOpened.isPresent();
	}

	private void chooseCartridge(final CartridgeType type) {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(
				((SidPlay2Section) (util.getConfig().getSidplay2Section())).getLastDirectoryFolder());
		fileDialog.getExtensionFilters()
				.add(new ExtensionFilter(CartFileExtensions.DESCRIPTION, CartFileExtensions.EXTENSIONS));
		fileDialog.setTitle(util.getBundle().getString("INSERT_CARTRIDGE"));
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			try {
				util.getPlayer().insertCartridge(type, file);
				util.getPlayer().play(SidTune.RESET);
			} catch (IOException | SidTuneError e) {
				System.err.println(String.format("Cannot insert file '%s' as cartridge of type '%s'.",
						file.getAbsolutePath(), type.name()));
			}
		}
	}

	private void insertCartridge(CartridgeType type, int sizeKB) {
		try {
			util.getPlayer().insertCartridge(type, sizeKB);
			util.getPlayer().play(SidTune.RESET);
		} catch (IOException | SidTuneError ex) {
			System.err.println(
					String.format("Cannot insert cartridge of type '%s' and size '%d'KB.", type.name(), sizeKB));
		}
	}

	private void playTune(final SidTune tune) {
		video();
		util.setPlayingTab(
				tabbedPane.getTabs().stream().filter((tab) -> tab.getId().equals(Video.ID)).findFirst().get());
		util.getPlayer().play(tune);
	}

	private void playNextRandomHVSC() {
		SidPlay2Section sidPlay2Section = (SidPlay2Section) util.getConfig().getSidplay2Section();
		String rndPath = util.getPlayer().getSidDatabaseInfo(db -> db.getRandomPath(), null);
		if (rndPath != null) {
			File file = PathUtils.getFile(rndPath, sidPlay2Section.getHvscFile(), sidPlay2Section.getCgscFile());
			hvsc();
			util.setPlayingTab(tabbedPane.getTabs().stream()
					.filter((tab) -> tab.getId().equals(MusicCollection.HVSC_ID)).findFirst().get());
			try {
				util.getPlayer().play(SidTune.load(file));
			} catch (IOException | SidTuneError e) {
				openErrorDialog(String.format(util.getBundle().getString("ERR_IO_ERROR"), e.getMessage()));
			}
		}
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	protected void setStatusLine() {
		ReadOnlyObjectProperty<State> state = util.getPlayer().stateProperty();
		if (!(state.get().equals(State.PLAY) || state.get().equals(State.PAUSE))) {
			return;
		}
		// Get status information of the first disk drive
		final C1541 c1541 = getFirstFloppy();
		// Disk motor status
		boolean motorOn = util.getConfig().getC1541Section().isDriveSoundOn() && c1541.getDiskController().isMotorOn();
		if (!oldMotorOn && motorOn) {
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
			progress.setProgress(datasette.getProgress());
		}
		// final status bar text
		StringBuilder line = new StringBuilder();
		line.append(determineVideoNorm());
		line.append(determineEmulation());
		line.append(determineChipModel());
		line.append(playerId);
		double tuneSpeed = util.getPlayer().getC64().determineTuneSpeed();
		if (tuneSpeed > 0) {
			line.append(String.format("%s: %.1fx, ", util.getBundle().getString("SPEED"), tuneSpeed));
		}
		line.append(determineSong());
		if (datasette.getMotor()) {
			line.append(String.format("%s: %03d, ", util.getBundle().getString("DATASETTE_COUNTER"),
					datasette.getCounter()));
		}
		if (c1541.getDiskController().isMotorOn()) {
			line.append(String.format("%s: %02d, ", util.getBundle().getString("FLOPPY_TRACK"), halfTrack >> 1));
		}
		line.append(String.format("%s: %s%s", util.getBundle().getString("TIME"), determinePlayTime(),
				determineSongLength()));
		status.setText(line.toString());
		status.setTooltip(playerinfos.length() > 0 ? statusTooltip : null);
		statusTooltip.setText(playerinfos.toString());
	}

	private String determineChipModel() {
		EmulationSection emulation = util.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		ChipModel chipModel = ChipModel.getChipModel(emulation, util.getPlayer().getTune(), 0);
		line.append(String.format("%s", chipModel));
		if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 1)) {
			ChipModel stereoModel = ChipModel.getChipModel(emulation, util.getPlayer().getTune(), 1);
			int dualSidBase = SidTune.getSIDAddress(emulation, util.getPlayer().getTune(), 1);
			line.append(String.format("+%s(at 0x%4x)", stereoModel, dualSidBase));
			if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 2)) {
				ChipModel thirdModel = ChipModel.getChipModel(emulation, util.getPlayer().getTune(), 2);
				int thirdSidBase = SidTune.getSIDAddress(emulation, util.getPlayer().getTune(), 2);
				line.append(String.format("+%s(at 0x%4x)", thirdModel, thirdSidBase));
			}
		}
		line.append(", ");
		return line.toString();
	}

	private String determineEmulation() {
		EmulationSection emulation = util.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		line.append(String.format("%s", Emulation.getEmulation(emulation, util.getPlayer().getTune(), 0).name()));
		if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 1)) {
			String stereoEmulation = Emulation.getEmulation(emulation, util.getPlayer().getTune(), 1).name();
			line.append(String.format("+%s", stereoEmulation));
			if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 2)) {
				String thirdEmulation = Emulation.getEmulation(emulation, util.getPlayer().getTune(), 2).name();
				line.append(String.format("+%s", thirdEmulation));
			}
		}
		line.append(", ");
		return line.toString();
	}

	private String determineVideoNorm() {
		return String.format("%s, ",
				CPUClock.getCPUClock(util.getConfig().getEmulationSection(), util.getPlayer().getTune()).name());
	}

	private String determineSong() {
		SidTune tune = util.getPlayer().getTune();
		if (tune != null) {
			SidTuneInfo info = tune.getInfo();
			if (info.getSongs() > 1) {
				return String.format("%s: %d/%d, ", util.getBundle().getString("SONG"), info.getCurrentSong(),
						info.getSongs());
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
		int songLength = tune != null ? util.getPlayer().getSidDatabaseInfo(db -> db.getSongLength(tune), 0) : 0;
		if (songLength > 0) {
			// song length well-known?
			return String.format("/%02d:%02d", (songLength / 60 % 100), (songLength % 60));
		}
		return "";
	}

	private void setPlayerIdAndInfos() {
		playerinfos.setLength(0);
		playerId.setLength(0);
		SidTune tune = util.getPlayer().getTune();
		if (tune != null) {
			for (final String id : tune.identify()) {
				playerId.append(util.getBundle().getString("PLAYER_ID")).append(": ").append(id);
				PlayerInfoSection playerInfo = tune.getPlayerInfo(id);
				if (playerInfo != null) {
					playerinfos.append(playerInfo.toString()).append("\n");
					status.setUserData(playerInfo.getReference());
				} else {
					status.setUserData(null);
				}
				int length = id.length();
				playerId.setLength(playerId.length() - (length - Math.min(length, 14)));
				if (length > 14) {
					playerId.append("...");
				}
				playerId.append(", ");
				break;
			}
		}
	}

	private void createHardCopy(String format) {
		video();
		try {
			Video videoScreen = (Video) tabbedPane.getTabs().stream().filter((tab) -> tab.getId().equals(Video.ID))
					.findFirst().get();
			ImageIO.write(SwingFXUtils.fromFXImage(videoScreen.getVicImage(), null), format,
					new File(util.getConfig().getSidplay2Section().getTmpDir(),
							"screenshot" + (++hardcopyCounter) + "." + format));
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

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			// EXTEND_ASK
			YesNoDialog dialog = new YesNoDialog(util.getPlayer());
			dialog.getStage().setTitle(util.getBundle().getString("EXTEND_DISK_IMAGE"));
			dialog.setText(util.getBundle().getString("EXTEND_DISK_IMAGE_TO_40_TRACKS"));
			dialog.open();
			return dialog.getConfirmed().get();
		} else if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
			// EXTEND_ACCESS
			return true;
		} else {
			// EXTEND_NEVER
			return false;
		}
	}

	/**
	 * Provide a filename for the tune containing some tune infos.
	 * 
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public String apply(SidTune tune) {
		String defaultName = "jsidplay2";
		if (tune == SidTune.RESET) {
			return new File(util.getConfig().getSidplay2Section().getTmpDir(), defaultName).getAbsolutePath();
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> infos = info.getInfoString().iterator();
		String name = infos.hasNext() ? infos.next().replaceAll("[:\\\\/*?|<>]", "_") : defaultName;
		String filename = new File(util.getConfig().getSidplay2Section().getTmpDir(),
				PathUtils.getFilenameWithoutSuffix(name)).getAbsolutePath();
		if (info.getSongs() > 1) {
			filename += String.format("-%02d", info.getCurrentSong());
		}
		return filename;
	}

	private void openErrorDialog(String msg) {
		AlertDialog alertDialog = new AlertDialog(util.getPlayer());
		alertDialog.getStage().setTitle(util.getBundle().getString("ALERT_TITLE"));
		alertDialog.setText(msg);
		alertDialog.setWait(true);
		alertDialog.open();
	}
}
