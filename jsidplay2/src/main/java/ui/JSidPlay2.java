package ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.media.AudioClip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;
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
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import libsidutils.WebUtils;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import netsiddev_builder.NetSIDDevConnection;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.audio.JavaSound.Device;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.TimeToStringConverter;
import ui.common.dialog.YesNoDialog;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class JSidPlay2 extends C64Window implements IExtendImageListener, Function<SidTune, String> {

	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			JSidPlay2.class.getResource("/ui/sounds/track.wav").toString());

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	/** Build date calculated from our own modify time */
	private static String DATE = "unknown";

	static {
		try {
			URL us = JSidPlay2Main.class.getResource("/" + JSidPlay2.class.getName().replace('.', '/') + ".class");
			Date date = new Date(us.openConnection().getLastModified());
			DATE = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);

			MOTORSOUND_AUDIOCLIP.setCycleCount(AudioClip.INDEFINITE);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

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
	private TextField defaultTime, hostname, port;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	ToggleGroup playSourceGroup;
	@FXML
	protected Button volumeButton, mp3Browse;
	@FXML
	protected TabPane tabbedPane;
	@FXML
	private Label status, hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label;
	@FXML
	protected ProgressBar progress;

	public TabPane getTabbedPane() {
		return tabbedPane;
	}

	private Scene scene;
	private Timeline timer;
	private int oldHalfTrack;
	private boolean duringInitialization, oldMotorOn;
	private StringBuilder playerId, playerinfos;
	private Tooltip statusTooltip;
	private StateChangeListener nextTuneListener;

	private class StateChangeListener implements ChangeListener<State> {
		@Override
		public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> {
				if (newValue == State.START) {
					setPlayerIdAndInfos(sidTune);
				}
			});
		}

		/**
		 * Set SID Tune Player (name) and Details (author, released, comment and
		 * CSDB link)
		 * 
		 * e.g. player ID: Soedesoft, author: Jeroen Soede and Michiel Soede,
		 * etc.
		 * 
		 * @param sidTune
		 *            tune containing player details
		 */
		private void setPlayerIdAndInfos(SidTune sidTune) {
			playerId.setLength(0);
			playerinfos.setLength(0);
			if (sidTune != SidTune.RESET) {
				for (final String id : sidTune.identify()) {
					playerId.append(util.getBundle().getString("PLAYER_ID")).append(": ").append(id);
					PlayerInfoSection playerInfo = sidTune.getPlayerInfo(id);
					status.setUserData(null);
					if (playerInfo != null) {
						playerinfos.append(playerInfo.toString()).append("\n");
						status.setUserData(playerInfo.getReference());
					}
					playerId.setLength(playerId.length() - (id.length() - Math.min(id.length(), 14)));
					if (id.length() > 14) {
						playerId.append("...");
					}
					playerId.append(", ");
					break;
				}
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
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		this.playerId = new StringBuilder();
		this.playerinfos = new StringBuilder();
		this.scene = getStage().getScene();
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
			hostnameLabel.setDisable(!Engine.NETSID.equals(n));
			hostname.setDisable(!Engine.NETSID.equals(n));
			portLabel.setDisable(!Engine.NETSID.equals(n));
			port.setDisable(!Engine.NETSID.equals(n));
		});
		engineBox.valueProperty().bindBidirectional(emulationSection.engineProperty());

		Bindings.bindBidirectional(defaultTime.textProperty(), sidplay2Section.defaultPlayLengthProperty(),
				new TimeToStringConverter());
		sidplay2Section.defaultPlayLengthProperty().addListener((obj, o, n) -> {
			final Tooltip tooltip = new Tooltip();
			defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			if (n.intValue() != -1) {
				util.getPlayer().getTimer().updateEnd();
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_OK);
			} else {
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
			}
		});

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		Bindings.bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(),
				new IntegerStringConverter());

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		this.duringInitialization = false;

		final Duration duration = Duration.millis(1000);
		final KeyFrame frame = new KeyFrame(duration, evt -> setStatusLine());
		timer = new Timeline(frame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(nextTuneListener);
		timer.stop();
		util.getPlayer().quit();
		Platform.exit();
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
	private void setHostname() {
		NetSIDDevConnection.getInstance().invalidate();
		restart();
	}

	@FXML
	private void setPort() {
		NetSIDDevConnection.getInstance().invalidate();
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
	private void setVideoStandard() {
		restart();
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
		switch (emulation.getEngine()) {
		case EMULATION:
			line.append(String.format("%s", Emulation.getEmulation(emulation, util.getPlayer().getTune(), 0).name()));
			if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 1)) {
				String stereoEmulation = Emulation.getEmulation(emulation, util.getPlayer().getTune(), 1).name();
				line.append(String.format("+%s", stereoEmulation));
				if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 2)) {
					String thirdEmulation = Emulation.getEmulation(emulation, util.getPlayer().getTune(), 2).name();
					line.append(String.format("+%s", thirdEmulation));
				}
			}
			break;
		case NETSID:
		case HARDSID:
			line.append(String.format("%s", emulation.getEngine().name()));
			if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 1)) {
				line.append(String.format("+%s", emulation.getEngine().name()));
				if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 2)) {
					line.append(String.format("+%s", emulation.getEngine().name()));
				}
			}
			break;
		default:
			break;
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

}
