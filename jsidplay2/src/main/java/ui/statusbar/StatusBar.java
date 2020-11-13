package ui.statusbar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.DesktopIntegration;
import libsidutils.psid64.PSid64TuneInfo;
import libsidutils.psid64.Psid64;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.entities.config.C1541Section;
import ui.entities.config.EmulationSection;

public class StatusBar extends C64VBox implements UIPart {

	private static final Clip MOTORSOUND_AUDIOCLIP;
	private static final Clip TRACKSOUND_AUDIOCLIP;

	static {
		try {
			AudioInputStream motorSoundAudioClip = AudioSystem
					.getAudioInputStream(StatusBar.class.getResource("/ui/sounds/motor.wav"));
			MOTORSOUND_AUDIOCLIP = (Clip) AudioSystem
					.getLine(new DataLine.Info(Clip.class, motorSoundAudioClip.getFormat()));
			MOTORSOUND_AUDIOCLIP.open(motorSoundAudioClip);
			MOTORSOUND_AUDIOCLIP.setLoopPoints(0, -1);

			AudioInputStream trackSoundAudioClip = AudioSystem
					.getAudioInputStream(StatusBar.class.getResource("/ui/sounds/track.wav"));
			TRACKSOUND_AUDIOCLIP = (Clip) AudioSystem
					.getLine(new DataLine.Info(Clip.class, trackSoundAudioClip.getFormat()));
			TRACKSOUND_AUDIOCLIP.open(trackSoundAudioClip);
			TRACKSOUND_AUDIOCLIP.setLoopPoints(0, -1);
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
			throw new ExceptionInInitializerError();
		}
	}

	@FXML
	private Label status;

	private StringBuilder playerId, playerinfos;
	private Tooltip statusTooltip;
	private Timeline scrollText;

	private Timeline timer;
	private int oldHalfTrack;
	private boolean oldMotorOn;
	private CPUClock rememberCPUClock;
	private ChipModel rememberUserSidModel, rememberStereoSidModel;
	private Boolean rememberForceStereoTune;
	private Integer rememberDualSidBase;
	private StateChangeListener propertyChangeListener;

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			SidTune sidTune = util.getPlayer().getTune();
			Platform.runLater(() -> {
				if (event.getNewValue() == State.START) {
					setPlayerIdAndInfos(sidTune);
					recalculateScrollText();
				}
			});
		}

		/**
		 * Set SID Tune Player (name) and Details (author, released, comment and CSDB
		 * link)
		 *
		 * e.g. player ID: Soedesoft, author: Jeroen Soede and Michiel Soede, etc.
		 *
		 * @param sidTune tune containing player details
		 */
		private void setPlayerIdAndInfos(SidTune sidTune) {
			playerId.setLength(0);
			playerinfos.setLength(0);
			if (sidTune != SidTune.RESET) {
				for (final String id : sidTune.identify()) {
					playerId.append(", ");
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
					break;
				}
			}
		}
	}

	public StatusBar() {
		super();
	}

	public StatusBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		this.playerId = new StringBuilder();
		this.playerinfos = new StringBuilder();
		this.statusTooltip = new Tooltip();
		this.status.setOnMouseClicked(e -> {
			if (status.getUserData() != null) {
				DesktopIntegration.browse(status.getUserData().toString());
			}
		});
		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		final Duration duration = Duration.millis(1000);
		final KeyFrame frame = new KeyFrame(duration, evt -> setStatusLine());
		timer = new Timeline(frame);
		timer.setCycleCount(Animation.INDEFINITE);
		timer.playFromStart();
	}

	/**
	 * Set all the internal information of the emulation in the status bar.
	 */
	protected void setStatusLine() {
		C1541Section c1541Section = util.getConfig().getC1541Section();

		// Get status information of the first disk drive
		final C1541 c1541 = getFirstFloppy();
		// Disk motor status
		boolean motorOn = c1541Section.isDriveSoundOn() && c1541.getDiskController().isMotorOn();
		if (!oldMotorOn && motorOn) {
			MOTORSOUND_AUDIOCLIP.loop(Clip.LOOP_CONTINUOUSLY);
		} else if (oldMotorOn && !motorOn) {
			MOTORSOUND_AUDIOCLIP.stop();
		}
		oldMotorOn = motorOn;
		// Read/Write head position (half tracks)
		final int halfTrack = c1541.getDiskController().getHalfTrack();
		if (oldHalfTrack != halfTrack && motorOn) {
			TRACKSOUND_AUDIOCLIP.stop();
			TRACKSOUND_AUDIOCLIP.setFramePosition(0);
			TRACKSOUND_AUDIOCLIP.start();
		}
		oldHalfTrack = halfTrack;
		// Get status information of the datasette
		final Datasette datasette = util.getPlayer().getDatasette();
		// Datasette tape progress
		if (datasette.getMotor()) {
			DoubleProperty progressProperty = util.progressProperty(getScene());
			progressProperty.setValue(datasette.getProgress());
		}
		// final status bar text
		StringBuilder line = new StringBuilder();
		line.append(determineVideoNorm());
		line.append(determineChipModel());
		line.append(determineEmulation());
		line.append(detectPSID64ChipModel());
		line.append(playerId);
		double tuneSpeed = util.getPlayer().getC64().determineTuneSpeed();
		if (tuneSpeed > 0) {
			line.append(String.format(", %s: %.1fx", util.getBundle().getString("SPEED"), tuneSpeed));
		}
		line.append(determineSong());
		if (datasette.getMotor()) {
			line.append(String.format(", %s: %03d", util.getBundle().getString("DATASETTE_COUNTER"),
					datasette.getCounter()));
		}
		if (c1541.getDiskController().isMotorOn()) {
			line.append(String.format(", %s: %02d", util.getBundle().getString("FLOPPY_TRACK"), halfTrack >> 1));
		}
		Runtime runtime = Runtime.getRuntime();
		line.append(String.format(", %s: %sMb/%sMb", util.getBundle().getString("MEMORY"),
				runtime.totalMemory() - runtime.freeMemory() >> 20, runtime.maxMemory() >> 20));
		line.append(String.format(", %s: %s%s", util.getBundle().getString("TIME"), determinePlayTime(),
				determineSongLength()));
		if (util.getPlayer().getAudioDriver().isRecording()) {
			line.append(String.format(", %s: %s (%s)", util.getBundle().getString("RECORDING_FILENAME"),
					util.getPlayer().getRecordingFilename(),
					getFileSize(new File(util.getPlayer().getRecordingFilename()).length())));
		}
		status.setText(line.toString());
		status.setTooltip(playerinfos.length() > 0 ? statusTooltip : null);
		statusTooltip.setText(playerinfos.toString());
	}

	@Override
	public void doClose() {
		closeClip(MOTORSOUND_AUDIOCLIP);
		closeClip(TRACKSOUND_AUDIOCLIP);
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
	}

	private void recalculateScrollText() {
		if (scrollText != null) {
			scrollText.stop();
		}
		this.scrollText = createScrollTextTimeLine();
	}

	private Timeline createScrollTextTimeLine() {
		status.setTranslateX(0);

		double sceneWidth = getScene().getWidth() - 20 /* spacing */;
		double statusWidth = status.getLayoutBounds().getWidth();

		Timeline timeLine;
		if (statusWidth > sceneWidth) {
			double duration = (statusWidth - sceneWidth) / 40.;

			// 1. wait a moment doing nothing
			KeyValue initKeyValue = new KeyValue(status.translateXProperty(), 0);
			KeyFrame initFrame = new KeyFrame(Duration.seconds(duration), initKeyValue);

			// 2. move scroll text to the left
			KeyValue leftKeyValue = new KeyValue(status.translateXProperty(), -1.0 * (statusWidth - sceneWidth));
			KeyFrame leftFrame = new KeyFrame(Duration.seconds(duration + 5), leftKeyValue);

			// 3. wait a moment doing nothing
			KeyFrame stillLeftFrame = new KeyFrame(Duration.seconds(duration + 10), leftKeyValue);

			// 4. move scroll text to the right again
			KeyFrame rightFrame = new KeyFrame(Duration.seconds(duration + 15), initKeyValue);

			timeLine = new Timeline(initFrame, leftFrame, stillLeftFrame, rightFrame);
		} else {
			KeyValue initKeyValue = new KeyValue(status.translateXProperty(), 0);
			KeyFrame initFrame = new KeyFrame(Duration.seconds(5), initKeyValue);
			timeLine = new Timeline(initFrame);
		}
		timeLine.setOnFinished(event -> {
			recalculateScrollText();
		});
		timeLine.playFromStart();
		return timeLine;
	}

	private void closeClip(Clip clip) {
		if (clip.isOpen()) {
			if (clip.isActive()) {
				clip.stop();
				clip.flush();
			}
			clip.close();
		}
	}

	private String detectPSID64ChipModel() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		if (SidTune.isSolelyPrg(util.getPlayer().getTune())) {
			if (emulationSection.isDetectPSID64ChipModel()) {
				PSid64TuneInfo psid64TuneInfo = Psid64.detectPSid64TuneInfo(util.getPlayer().getC64().getRAM(),
						util.getPlayer().getC64().getVicMemBase()
								+ util.getPlayer().getC64().getVIC().getVideoMatrixBase());
				if (psid64TuneInfo.hasDifferentCPUClock(util.getPlayer().getC64().getClock())) {
					if (rememberCPUClock == null) {
						rememberCPUClock = emulationSection.getDefaultClockSpeed();
					}
					emulationSection.setDefaultClockSpeed(psid64TuneInfo.getCpuClock());
					return "";
				}
				// remember saved state
				boolean update = false;
				if (psid64TuneInfo.hasDifferentUserChipModel(emulationSection.getUserSidModel())) {
					if (rememberUserSidModel == null) {
						rememberUserSidModel = emulationSection.getUserSidModel();
					}
					emulationSection.setUserSidModel(psid64TuneInfo.getUserChipModel());
					update = true;
				}
				if (psid64TuneInfo.hasDifferentStereoChipModel(emulationSection.getStereoSidModel())) {
					if (rememberStereoSidModel == null) {
						rememberStereoSidModel = emulationSection.getStereoSidModel();
					}
					emulationSection.setStereoSidModel(psid64TuneInfo.getStereoChipModel());
					update = true;
				}
				if (psid64TuneInfo.isMonoTune() && emulationSection.isForceStereoTune()) {
					// mono tune detected
					if (rememberForceStereoTune == null) {
						rememberForceStereoTune = emulationSection.isForceStereoTune();
					}
					emulationSection.setForceStereoTune(false);
					update = true;
				} else if (psid64TuneInfo.isStereoTune() && !emulationSection.isForceStereoTune()) {
					// stereo tune detected
					if (rememberForceStereoTune == null) {
						rememberForceStereoTune = emulationSection.isForceStereoTune();
					}
					emulationSection.setForceStereoTune(true);
					update = true;
				}
				if (psid64TuneInfo.hasDifferentStereoAddress(emulationSection.getDualSidBase())) {
					update = true;
					if (rememberDualSidBase == null) {
						rememberDualSidBase = emulationSection.getDualSidBase();
					}
					emulationSection.setDualSidBase(psid64TuneInfo.getStereoAddress());
				}
				if (update) {
					emulationSection.setForce3SIDTune(false);
					util.getPlayer().updateSIDChipConfiguration();
				}
				if (psid64TuneInfo.isDetected()) {
					return ", PSID64";
				}
			}
		} else {
			// restore saved state
			boolean update = false;
			if (rememberCPUClock != null && emulationSection.getDefaultClockSpeed() != rememberCPUClock) {
				emulationSection.setDefaultClockSpeed(rememberCPUClock);
				rememberCPUClock = null;
				return "";
			}
			if (rememberUserSidModel != null && emulationSection.getUserSidModel() != rememberUserSidModel) {
				emulationSection.setUserSidModel(rememberUserSidModel);
				rememberUserSidModel = null;
				update = true;
			}
			if (rememberStereoSidModel != null && emulationSection.getStereoSidModel() != rememberStereoSidModel) {
				emulationSection.setStereoSidModel(rememberStereoSidModel);
				rememberStereoSidModel = null;
				update = true;
			}
			if (rememberForceStereoTune != null && emulationSection.isForceStereoTune() != rememberForceStereoTune) {
				emulationSection.setForceStereoTune(rememberForceStereoTune);
				rememberForceStereoTune = null;
				update = true;
			}
			if (rememberDualSidBase != null && emulationSection.getDualSidBase() != rememberDualSidBase) {
				emulationSection.setDualSidBase(rememberDualSidBase);
				rememberDualSidBase = null;
				update = true;
			}
			if (update) {
				util.getPlayer().updateSIDChipConfiguration();
			}
		}
		return "";
	}

	private String determineChipModel() {
		EmulationSection emulation = util.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		line.append(", ");
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
		return line.toString();
	}

	private String determineEmulation() {
		EmulationSection emulation = util.getConfig().getEmulationSection();
		StringBuilder line = new StringBuilder();
		line.append(", ");
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
		case HARDSID:
		case SIDBLASTER:
			Integer deviceCount = util.getPlayer().getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceCount(),
					null);
			if (deviceCount != null) {
				Integer deviceId0 = util.getPlayer().getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceId(0),
						null);
				String deviceName0 = util.getPlayer()
						.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceName(0), null);
				ChipModel deviceChipModel0 = util.getPlayer()
						.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceChipModel(0), null);
				if (deviceId0 != null && deviceChipModel0 != null) {
					line.append(String.format(util.getBundle().getString("DEVICE"), emulation.getEngine().name(),
							deviceId0, Optional.ofNullable(deviceName0).orElse(""), deviceChipModel0));
				} else {
					line.append(emulation.getEngine().name());
				}
				if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 1)) {
					line.append("+");
					Integer deviceId1 = util.getPlayer()
							.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceId(1), null);
					String deviceName1 = util.getPlayer()
							.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceName(1), null);
					ChipModel deviceChipModel1 = util.getPlayer()
							.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceChipModel(1), null);
					if (deviceId1 != null && deviceChipModel1 != null) {
						line.append(String.format(util.getBundle().getString("DEVICE"), emulation.getEngine().name(),
								deviceId1, Optional.ofNullable(deviceName1).orElse(""), deviceChipModel1));
					} else {
						line.append(emulation.getEngine().name());
					}
					if (SidTune.isSIDUsed(emulation, util.getPlayer().getTune(), 2)) {
						line.append("+");
						Integer deviceId2 = util.getPlayer()
								.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceId(2), null);
						String deviceName2 = util.getPlayer()
								.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceName(2), null);
						ChipModel deviceChipModel2 = util.getPlayer()
								.getHardwareSIDBuilderInfo(sidBuilder -> sidBuilder.getDeviceChipModel(2), null);
						if (deviceId2 != null && deviceChipModel2 != null) {
							line.append(String.format(util.getBundle().getString("DEVICE"),
									emulation.getEngine().name(), deviceId2,
									Optional.ofNullable(deviceName2).orElse(""), deviceName2, deviceChipModel2));
						} else {
							line.append(emulation.getEngine().name());
						}
					}
				}
				line.append(" ").append(String.format(util.getBundle().getString("DEVICES"), deviceCount));
				break;
			}
			// $FALL-THROUGH$
		case NETSID:
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
		return line.toString();
	}

	private String determineVideoNorm() {
		return String.format("%s",
				CPUClock.getCPUClock(util.getConfig().getEmulationSection(), util.getPlayer().getTune()).name());
	}

	private String determineSong() {
		SidTune tune = util.getPlayer().getTune();
		if (tune != null) {
			SidTuneInfo info = tune.getInfo();
			if (info.getSongs() > 1) {
				return String.format(", %s: %d/%d", util.getBundle().getString("SONG"), info.getCurrentSong(),
						info.getSongs());
			}
		}
		return "";
	}

	private String determinePlayTime() {
		double timeInSeconds = util.getPlayer().time();
		return new SimpleDateFormat("mm:ss.SSS").format(new Date((long) (timeInSeconds * 1000)));
	}

	private String determineSongLength() {
		SidTune tune = util.getPlayer().getTune();
		double songLength = tune != null ? util.getPlayer().getSidDatabaseInfo(db -> db.getSongLength(tune), 0.) : 0;
		if (songLength > 0) {
			// song length well-known?
			return new SimpleDateFormat("/mm:ss.SSS").format(new Date((long) (songLength * 1000)));
		}
		return "";
	}

	private C1541 getFirstFloppy() {
		return util.getPlayer().getFloppies()[0];
	}

	private String getFileSize(long size) {
		if (size <= 0) {
			return "0 b";
		}
		final String[] units = new String[] { "b", "kb", "Mb", "Gb", "Tb" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
