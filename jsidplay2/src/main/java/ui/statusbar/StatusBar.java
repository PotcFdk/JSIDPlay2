package ui.statusbar;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.AnchorPane;
import javafx.scene.media.AudioClip;
import javafx.util.Duration;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.WebUtils;
import libsidutils.sidid.SidIdInfo.PlayerInfoSection;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.EmulationSection;

public class StatusBar extends AnchorPane implements UIPart {

	private static final AudioClip MOTORSOUND_AUDIOCLIP = new AudioClip(
			StatusBar.class.getResource("/ui/sounds/motor.wav").toString());
	private static final AudioClip TRACKSOUND_AUDIOCLIP = new AudioClip(
			StatusBar.class.getResource("/ui/sounds/track.wav").toString());

	static {
		MOTORSOUND_AUDIOCLIP.setCycleCount(AudioClip.INDEFINITE);
	}

	@FXML
	private Label status;
	@FXML
	protected ProgressBar progress;

	private StringBuilder playerId, playerinfos;
	private Tooltip statusTooltip;

	private Timeline timer;
	private int oldHalfTrack;
	private boolean oldMotorOn;

	protected UIUtil util;

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

	public StatusBar(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
	}

	@FXML
	private void initialize() {
		this.playerId = new StringBuilder();
		this.playerinfos = new StringBuilder();
		this.statusTooltip = new Tooltip();
		this.status.setOnMouseClicked(e -> {
			if (status.getUserData() != null)
				WebUtils.browse(status.getUserData().toString());
		});
		util.getPlayer().stateProperty().addListener(new StateChangeListener());

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

}
