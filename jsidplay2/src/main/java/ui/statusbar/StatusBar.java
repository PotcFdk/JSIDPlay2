package ui.statusbar;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

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
import libsidplay.common.ChipModel;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import libsidutils.psid64.PSid64TuneInfo;
import libsidutils.psid64.Psid64;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import libsidutils.status.Status;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.util.DesktopUtil;
import ui.entities.config.C1541Section;
import ui.entities.config.EmulationSection;

public class StatusBar extends C64VBox implements UIPart {

	private static Clip MOTORSOUND_AUDIOCLIP;
	private static Clip TRACKSOUND_AUDIOCLIP;

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
		} catch (UnsupportedAudioFileException | IOException | LineUnavailableException | IllegalArgumentException e) {
			System.err.println("Warn: drive sounds are not available:" + e.getMessage());
		}
	}

	@FXML
	private Label statusLbl;

	private StringBuilder playerId, playerinfos;
	private Status status;
	private Tooltip statusTooltip;
	private Timeline scrollText;

	private Timeline timer;
	private int oldHalfTrack;
	private boolean oldMotorOn;
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
					statusLbl.setUserData(null);
					if (playerInfo != null) {
						playerinfos.append(playerInfo.toString()).append("\n");
						statusLbl.setUserData(playerInfo.getReference());
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
		this.status = new Status(util.getPlayer(), util.getBundle());
		this.statusTooltip = new Tooltip();
		this.statusLbl.setOnMouseClicked(e -> {
			if (statusLbl.getUserData() != null) {
				DesktopUtil.browse(statusLbl.getUserData().toString());
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
		if (MOTORSOUND_AUDIOCLIP != null) {
			if (!oldMotorOn && motorOn) {
				MOTORSOUND_AUDIOCLIP.loop(Clip.LOOP_CONTINUOUSLY);
			} else if (oldMotorOn && !motorOn) {
				MOTORSOUND_AUDIOCLIP.stop();
			}
		}
		oldMotorOn = motorOn;
		// Read/Write head position (half tracks)
		final int halfTrack = c1541.getDiskController().getHalfTrack();
		if (TRACKSOUND_AUDIOCLIP != null) {
			if (oldHalfTrack != halfTrack && motorOn) {
				TRACKSOUND_AUDIOCLIP.stop();
				TRACKSOUND_AUDIOCLIP.setFramePosition(0);
				TRACKSOUND_AUDIOCLIP.start();
			}
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
		line.append(status.determineVideoNorm());
		line.append(", ");
		line.append(status.determineChipModels(true));
		line.append(", ");
		line.append(status.determineEmulations(true));
		line.append(detectPSID64ChipModel());
		line.append(playerId);
		line.append(status.determineTuneSpeed());
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
		line.append(String.format(", %s: %s%s", util.getBundle().getString("TIME"), status.determinePlayTime(true),
				status.determineSongLength()));
		if (util.getPlayer().getAudioDriver().isRecording()) {
			line.append(String.format(", %s: %s (%s)", util.getBundle().getString("RECORDING_FILENAME"),
					util.getPlayer().getRecordingFilename(),
					getFileSize(new File(util.getPlayer().getRecordingFilename()).length())));
		}
		statusLbl.setText(line.toString());
		statusLbl.setTooltip(playerinfos.length() > 0 ? statusTooltip : null);
		statusTooltip.setText(playerinfos.toString());
	}

	@Override
	public void doClose() {
		if (MOTORSOUND_AUDIOCLIP != null) {
			closeClip(MOTORSOUND_AUDIOCLIP);
		}
		if (TRACKSOUND_AUDIOCLIP != null) {
			closeClip(TRACKSOUND_AUDIOCLIP);
		}
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
	}

	private void recalculateScrollText() {
		if (scrollText != null) {
			scrollText.stop();
		}
		this.scrollText = createScrollTextTimeLine();
	}

	private Timeline createScrollTextTimeLine() {
		statusLbl.setTranslateX(0);

		double sceneWidth = getScene().getWidth() - 20 /* spacing */;
		double statusWidth = statusLbl.getLayoutBounds().getWidth();

		Timeline timeLine;
		if (statusWidth > sceneWidth) {
			double duration = (statusWidth - sceneWidth) / 40.;

			// 1. wait a moment doing nothing
			KeyValue initKeyValue = new KeyValue(statusLbl.translateXProperty(), 0);
			KeyFrame initFrame = new KeyFrame(Duration.seconds(duration), initKeyValue);

			// 2. move scroll text to the left
			KeyValue leftKeyValue = new KeyValue(statusLbl.translateXProperty(), -1.0 * (statusWidth - sceneWidth));
			KeyFrame leftFrame = new KeyFrame(Duration.seconds(duration + 5), leftKeyValue);

			// 3. wait a moment doing nothing
			KeyFrame stillLeftFrame = new KeyFrame(Duration.seconds(duration + 10), leftKeyValue);

			// 4. move scroll text to the right again
			KeyFrame rightFrame = new KeyFrame(Duration.seconds(duration + 15), initKeyValue);

			timeLine = new Timeline(initFrame, leftFrame, stillLeftFrame, rightFrame);
		} else {
			KeyValue initKeyValue = new KeyValue(statusLbl.translateXProperty(), 0);
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
		if (SidTune.isSolelyPrg(util.getPlayer().getTune()) && emulationSection.isDetectPSID64ChipModel()) {
			PSid64TuneInfo psid64TuneInfo = Psid64.detectPSid64TuneInfo(util.getPlayer().getC64().getRAM(),
					util.getPlayer().getC64().getVicMemBase()
							+ util.getPlayer().getC64().getVIC().getVideoMatrixBase());
			boolean update = false;
			if (psid64TuneInfo.hasDifferentUserChipModel(
					ChipModel.getChipModel(emulationSection, util.getPlayer().getTune(), 0))) {
				emulationSection.getOverrideSection().getSidModel()[0] = psid64TuneInfo.getUserChipModel();
				update = true;
			}
			if (psid64TuneInfo.hasDifferentStereoChipModel(
					ChipModel.getChipModel(emulationSection, util.getPlayer().getTune(), 1))) {
				emulationSection.getOverrideSection().getSidModel()[1] = psid64TuneInfo.getStereoChipModel();
				update = true;
			}
			if (psid64TuneInfo.hasDifferentStereoAddress(
					SidTune.getSIDAddress(emulationSection, util.getPlayer().getTune(), 1))) {
				emulationSection.getOverrideSection().getSidBase()[1] = psid64TuneInfo.getStereoAddress();
				update = true;
			}
			if (update) {
				util.getPlayer().updateSIDChipConfiguration();
			}
			if (psid64TuneInfo.isDetected()) {
				return ", PSID64";
			}
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
		final String[] units = new String[] { "b", "Kb", "Mb", "Gb", "Tb" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
	}
}
