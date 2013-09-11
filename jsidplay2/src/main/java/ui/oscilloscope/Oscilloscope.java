package ui.oscilloscope;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tab;
import javafx.util.Duration;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import sidplay.consoleplayer.State;
import ui.common.C64Tab;

/**
 * @author Ken Händel
 */
public class Oscilloscope extends C64Tab {

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
			for (int i = 0; i < 2; i++) {
				final SIDEmu sidemu = getPlayer().getC64().getSID(i);
				if (sidemu == null) {
					continue;
				}

				sidemu.clock();
				for (int j = 0; j < 4; j++) {
					gauges[i][j][0].sample(sidemu);
					gauges[i][j][1].sample(sidemu);
					gauges[i][j][2].sample(sidemu);

					gauges[i][j][0].advance();
					if ((repaint & 127) == 0) {
						gauges[i][j][1].advance();
						gauges[i][j][2].advance();
					}
				}
			}

			++repaint;
			ctx.schedule(this, 128);
		}
	}

	@FXML
	protected CheckBox muteVoice1, muteVoice2, muteVoice3, muteVoice4,
			muteVoice5, muteVoice6;
	@FXML
	private WaveGauge waveMono_0, waveMono_1, waveMono_2, waveStereo_0,
			waveStereo_1, waveStereo_2;
	@FXML
	private EnvelopeGauge envMono_0, envMono_1, envMono_2, envStereo_0,
			envStereo_1, envStereo_2;
	@FXML
	private FrequencyGauge freqMono_0, freqMono_1, freqMono_2, freqStereo_0,
			freqStereo_1, freqStereo_2;
	@FXML
	private VolumeGauge volumeMono, volumeStereo;
	@FXML
	private ResonanceGauge resonanceMono, resonanceStereo;
	@FXML
	private FilterGauge filterMono, filterStereo;

	protected SIDGauge[][][] gauges;
	protected int repaint;
	protected final HighResolutionEvent highResolutionEvent = new HighResolutionEvent();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		getConsolePlayer().stateProperty().addListener(new ChangeListener<State>() {
			@Override
			public void changed(ObservableValue<? extends State> arg0,
					State arg1, State arg2) {
				if (arg2 == State.RUNNING) {
					final EventScheduler ctx = getPlayer().getC64()
							.getEventScheduler();
					/* sample oscillator buffer */
					highResolutionEvent.beginScheduling(ctx);

					for (int i = 0; i < gauges.length; i++) {
						for (int j = 0; j < gauges[i].length; j++) {
							for (int k = 0; k < gauges[i][j].length; k++) {
								gauges[i][j][k].reset();
							}
						}
					}
					Platform.runLater(new Runnable() {
						
						@Override
						public void run() {
							for (int i = 0; i < gauges.length; i++) {
								for (int j = 0; j < gauges[i].length; j++) {
									for (int k = 0; k < gauges[i][j].length; k++) {
										gauges[i][j][k].updateGauge();
									}
								}
							}
						}
					});

					getPlayer().mute(0, 0, muteVoice1.isSelected());
					getPlayer().mute(0, 1, muteVoice2.isSelected());
					getPlayer().mute(0, 2, muteVoice3.isSelected());
					getPlayer().mute(1, 0, muteVoice4.isSelected());
					getPlayer().mute(1, 1, muteVoice5.isSelected());
					getPlayer().mute(1, 2, muteVoice6.isSelected());
				}
			}
		});
		waveMono_0.setLocalizer(getBundle());
		waveMono_1.setLocalizer(getBundle());
		waveMono_2.setLocalizer(getBundle());
		waveStereo_0.setLocalizer(getBundle());
		waveStereo_1.setLocalizer(getBundle());
		waveStereo_2.setLocalizer(getBundle());

		// Mono SID gauges
		gauges = new SIDGauge[2][4][3];
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

		final PauseTransition pt = new PauseTransition(Duration.millis(50));
		pt.setCycleCount(1);
		pt.setOnFinished(new EventHandler<ActionEvent>() {

			@Override
			public void handle(ActionEvent ae) {

				for (int i = 0; i < 2; i++) {
					final SIDEmu sidemu = getPlayer().getC64().getSID(i);
					if (sidemu == null) {
						continue;
					}
					for (int j = 0; j < 4; j++) {
						gauges[i][j][0].updateGauge(sidemu);
						gauges[i][j][1].updateGauge(sidemu);
						gauges[i][j][2].updateGauge(sidemu);
					}
				}
				pt.play();
			}
		});
		pt.play();
		pt.pause();
		getTabPane().getSelectionModel().selectedItemProperty()
				.addListener(new ChangeListener<Tab>() {
					@Override
					public void changed(
							ObservableValue<? extends Tab> observable,
							Tab oldValue, Tab newValue) {
						// performance optimizations!
						if (Oscilloscope.this.equals(newValue)) {
							pt.play();
						} else {
							pt.pause();
						}
					}
				});
	}

	@FXML
	private void doMuteVoice1() {
		getPlayer().mute(0, 0, muteVoice1.isSelected());
	}

	@FXML
	private void doMuteVoice2() {
		getPlayer().mute(0, 1, muteVoice2.isSelected());
	}

	@FXML
	private void doMuteVoice3() {
		getPlayer().mute(0, 2, muteVoice3.isSelected());
	}

	@FXML
	private void doMuteVoice4() {
		getPlayer().mute(1, 0, muteVoice4.isSelected());
	}

	@FXML
	private void doMuteVoice5() {
		getPlayer().mute(1, 1, muteVoice5.isSelected());
	}

	@FXML
	private void doMuteVoice6() {
		getPlayer().mute(1, 2, muteVoice6.isSelected());
	}

}
