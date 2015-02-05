package ui.oscilloscope;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.util.Duration;
import libsidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.player.State;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

/**
 * @author Ken Händel
 */
public class Oscilloscope extends Tab implements UIPart {

	public static final String ID = "OSCILLOSCOPE";

	protected class HighResolutionEvent extends Event {
		private EventScheduler ctx;

		public HighResolutionEvent() {
			super("High Resolution SID Register Sampler");
		}

		public void beginScheduling(final EventScheduler _ctx) {
			ctx = _ctx;
			ctx.cancel(this);
			ctx.schedule(this, 0, Event.Phase.PHI2);
		}

		@Override
		public void event() {
			util.getPlayer().configureSIDs((chipNum, sid) -> {
				sid.clock();
				for (int row = 0; row < 4; row++) {
					gauges[chipNum][row][0].sample(sid);
					gauges[chipNum][row][1].sample(sid);
					gauges[chipNum][row][2].sample(sid);

					gauges[chipNum][row][0].advance();
					if ((repaint & 127) == 0) {
						gauges[chipNum][row][1].advance();
						gauges[chipNum][row][2].advance();
					}
				}
			});
			++repaint;
			ctx.schedule(this, 128);
		}
	}

	@FXML
	protected CheckBox muteVoice1, muteVoice2, muteVoice3, muteVoice4,
			muteVoice5, muteVoice6, muteVoice7, muteVoice8, muteVoice9;
	@FXML
	private WaveGauge waveMono_0, waveMono_1, waveMono_2, waveStereo_0,
			waveStereo_1, waveStereo_2, wave3Sid_0, wave3Sid_1, wave3Sid_2;
	@FXML
	private EnvelopeGauge envMono_0, envMono_1, envMono_2, envStereo_0,
			envStereo_1, envStereo_2, env3Sid_0, env3Sid_1, env3Sid_2;
	@FXML
	private FrequencyGauge freqMono_0, freqMono_1, freqMono_2, freqStereo_0,
			freqStereo_1, freqStereo_2, freq3Sid_0, freq3Sid_1, freq3Sid_2;
	@FXML
	private VolumeGauge volumeMono, volumeStereo, volume3Sid;
	@FXML
	private ResonanceGauge resonanceMono, resonanceStereo, resonance3Sid;
	@FXML
	private FilterGauge filterMono, filterStereo, filter3Sid;

	private UIUtil util;

	protected SIDGauge[][][] gauges;
	protected int repaint;
	protected final HighResolutionEvent highResolutionEvent = new HighResolutionEvent();

	public Oscilloscope(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		util.getPlayer().stateProperty()
				.addListener((observable, oldValue, newValue) -> {
					if (newValue == State.RUNNING) {
						startOscilloscope();
					}
				});
		waveMono_0.setLocalizer(util.getBundle());
		waveMono_1.setLocalizer(util.getBundle());
		waveMono_2.setLocalizer(util.getBundle());
		waveStereo_0.setLocalizer(util.getBundle());
		waveStereo_1.setLocalizer(util.getBundle());
		waveStereo_2.setLocalizer(util.getBundle());
		wave3Sid_0.setLocalizer(util.getBundle());
		wave3Sid_1.setLocalizer(util.getBundle());
		wave3Sid_2.setLocalizer(util.getBundle());

		// Mono SID gauges
		gauges = new SIDGauge[3][4][3];
		gauges[0][0][0] = waveMono_0;
		gauges[0][1][0] = waveMono_1;
		gauges[0][2][0] = waveMono_2;
		gauges[0][0][1] = envMono_0;
		gauges[0][1][1] = envMono_1;
		gauges[0][2][1] = envMono_2;
		gauges[0][0][2] = freqMono_0;
		gauges[0][1][2] = freqMono_1;
		gauges[0][2][2] = freqMono_2;
		gauges[0][3][0] = volumeMono;
		gauges[0][3][1] = resonanceMono;
		gauges[0][3][2] = filterMono;

		// Stereo SID gauges
		gauges[1][0][0] = waveStereo_0;
		gauges[1][1][0] = waveStereo_1;
		gauges[1][2][0] = waveStereo_2;
		gauges[1][0][1] = envStereo_0;
		gauges[1][1][1] = envStereo_1;
		gauges[1][2][1] = envStereo_2;
		gauges[1][0][2] = freqStereo_0;
		gauges[1][1][2] = freqStereo_1;
		gauges[1][2][2] = freqStereo_2;
		gauges[1][3][0] = volumeStereo;
		gauges[1][3][1] = resonanceStereo;
		gauges[1][3][2] = filterStereo;

		// 3SID gauges
		gauges[2][0][0] = wave3Sid_0;
		gauges[2][1][0] = wave3Sid_1;
		gauges[2][2][0] = wave3Sid_2;
		gauges[2][0][1] = env3Sid_0;
		gauges[2][1][1] = env3Sid_1;
		gauges[2][2][1] = env3Sid_2;
		gauges[2][0][2] = freq3Sid_0;
		gauges[2][1][2] = freq3Sid_1;
		gauges[2][2][2] = freq3Sid_2;
		gauges[2][3][0] = volume3Sid;
		gauges[2][3][1] = resonance3Sid;
		gauges[2][3][2] = filter3Sid;

		final PauseTransition pt = new PauseTransition(Duration.millis(50));
		pt.setOnFinished((evt) -> {
			util.getPlayer().configureSIDs((chipNum, sid) -> {
				for (int row = 0; row < 4; row++) {
					gauges[chipNum][row][0].updateGauge(sid);
					gauges[chipNum][row][1].updateGauge(sid);
					gauges[chipNum][row][2].updateGauge(sid);
				}
			});
		});
		final SequentialTransition st = new SequentialTransition(pt);
		st.setCycleCount(Timeline.INDEFINITE);
		st.playFromStart();
		startOscilloscope();
	}

	private void startOscilloscope() {
		final EventScheduler ctx = util.getPlayer().getC64()
				.getEventScheduler();
		/* sample oscillator buffer */
		highResolutionEvent.beginScheduling(ctx);

		Platform.runLater(() -> {
			for (int chipNum = 0; chipNum < gauges.length; chipNum++) {
				for (int row = 0; row < gauges[chipNum].length; row++) {
					for (int col = 0; col < gauges[chipNum][row].length; col++) {
						gauges[chipNum][row][col].reset();
						gauges[chipNum][row][col].updateGauge();
					}
				}
			}
		});

		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(0, muteVoice1.isSelected()));
		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(1, muteVoice2.isSelected()));
		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(2, muteVoice3.isSelected()));
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(0, muteVoice4.isSelected()));
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(1, muteVoice5.isSelected()));
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(2, muteVoice6.isSelected()));
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(0, muteVoice7.isSelected()));
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(1, muteVoice8.isSelected()));
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(2, muteVoice9.isSelected()));
	}

	@FXML
	private void doMuteVoice1() {
		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(0, muteVoice1.isSelected()));
	}

	@FXML
	private void doMuteVoice2() {
		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(1, muteVoice2.isSelected()));
	}

	@FXML
	private void doMuteVoice3() {
		util.getPlayer().configureSID(0,
				sid -> sid.setVoiceMute(2, muteVoice3.isSelected()));
	}

	@FXML
	private void doMuteVoice4() {
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(0, muteVoice4.isSelected()));
	}

	@FXML
	private void doMuteVoice5() {
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(1, muteVoice5.isSelected()));
	}

	@FXML
	private void doMuteVoice6() {
		util.getPlayer().configureSID(1,
				sid -> sid.setVoiceMute(2, muteVoice6.isSelected()));
	}

	@FXML
	private void doMuteVoice7() {
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(0, muteVoice7.isSelected()));
	}

	@FXML
	private void doMuteVoice8() {
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(1, muteVoice8.isSelected()));
	}

	@FXML
	private void doMuteVoice9() {
		util.getPlayer().configureSID(2,
				sid -> sid.setVoiceMute(2, muteVoice9.isSelected()));
	}

}
