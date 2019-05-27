package ui.oscilloscope;

import java.beans.PropertyChangeListener;
import java.util.function.Consumer;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.util.Duration;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.entities.config.EmulationSection;

/**
 * @author Ken Händel
 */
public class Oscilloscope extends C64VBox implements UIPart {

	public static final String ID = "OSCILLOSCOPE";

	private long lastTime, ticksPerFrame;

	private class HighResolutionEvent extends Event {

		private int repaint;

		public HighResolutionEvent() {
			super("High Resolution SID Register Sampler");
		}

		@Override
		public void event() {
			long time = util.getPlayer().getC64().getEventScheduler().getTime(Phase.PHI2);
			final boolean addFrameImage = time - lastTime >= ticksPerFrame;
			util.getPlayer().getC64().configureSIDs((chipNum, sid) -> {
				sid.clock();
				sampleGauges(chipNum, sid, (repaint & 127) == 0);
				if (addFrameImage) {
					updateGauges(chipNum, gauge -> gauge.addImage(sid));
				}
			});
			if (addFrameImage) {
				lastTime += ticksPerFrame;
			}
			++repaint;
			util.getPlayer().getC64().getEventScheduler().schedule(highResolutionEvent, 128);
		}
	}

	@FXML
	protected CheckBox muteVoice1, muteVoice2, muteVoice3, muteVoice4, muteVoice5, muteVoice6, muteVoice7, muteVoice8,
			muteVoice9, muteVoice10, muteVoice11, muteVoice12;
	@FXML
	private WaveGauge waveMono_0, waveMono_1, waveMono_2, waveStereo_0, waveStereo_1, waveStereo_2, wave3Sid_0,
			wave3Sid_1, wave3Sid_2;
	@FXML
	private EnvelopeGauge envMono_0, envMono_1, envMono_2, envStereo_0, envStereo_1, envStereo_2, env3Sid_0, env3Sid_1,
			env3Sid_2;
	@FXML
	private FrequencyGauge freqMono_0, freqMono_1, freqMono_2, freqStereo_0, freqStereo_1, freqStereo_2, freq3Sid_0,
			freq3Sid_1, freq3Sid_2;
	@FXML
	private VolumeGauge volumeMono, volumeStereo, volume3Sid;
	@FXML
	private ResonanceGauge resonanceMono, resonanceStereo, resonance3Sid;
	@FXML
	private FilterGauge filterMono, filterStereo, filter3Sid;

	private PauseTransition pauseTransition;
	private SequentialTransition sequentialTransition;

	protected HighResolutionEvent highResolutionEvent;

	private PropertyChangeListener listener;

	public Oscilloscope() {
	}

	public Oscilloscope(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	protected void initialize() {
		pauseTransition = new PauseTransition(Duration.millis(50));
		sequentialTransition = new SequentialTransition(pauseTransition);
		highResolutionEvent = new HighResolutionEvent();
		listener = event -> {
			final EventScheduler ctx = util.getPlayer().getC64().getEventScheduler();
			if (event.getNewValue() == State.START) {
				if (!ctx.isPending(highResolutionEvent)) {
					prepareHighResolutionEvent();
					ctx.schedule(highResolutionEvent, 0, Phase.PHI2);
				}
				Platform.runLater(() -> {
					startOscilloscope();
				});
			} else if (event.getNewValue() == State.END || event.getNewValue() == State.RESTART
					|| event.getNewValue() == State.QUIT) {
				ctx.cancel(highResolutionEvent);
				Platform.runLater(() -> {
					stopOscilloscope();
				});
			}
		};
		util.getPlayer().stateProperty().addListener(listener);
		waveMono_0.setLocalizer(util.getBundle());
		waveMono_1.setLocalizer(util.getBundle());
		waveMono_2.setLocalizer(util.getBundle());
		waveStereo_0.setLocalizer(util.getBundle());
		waveStereo_1.setLocalizer(util.getBundle());
		waveStereo_2.setLocalizer(util.getBundle());
		wave3Sid_0.setLocalizer(util.getBundle());
		wave3Sid_1.setLocalizer(util.getBundle());
		wave3Sid_2.setLocalizer(util.getBundle());

		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		muteVoice1.selectedProperty().bindBidirectional(emulationSection.muteVoice1Property());
		muteVoice2.selectedProperty().bindBidirectional(emulationSection.muteVoice2Property());
		muteVoice3.selectedProperty().bindBidirectional(emulationSection.muteVoice3Property());
		muteVoice4.selectedProperty().bindBidirectional(emulationSection.muteVoice4Property());
		muteVoice5.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice1Property());
		muteVoice6.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice2Property());
		muteVoice7.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice3Property());
		muteVoice8.selectedProperty().bindBidirectional(emulationSection.muteStereoVoice4Property());
		muteVoice9.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice1Property());
		muteVoice10.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice2Property());
		muteVoice11.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice3Property());
		muteVoice12.selectedProperty().bindBidirectional(emulationSection.muteThirdSIDVoice4Property());

		startOscilloscope();

		prepareHighResolutionEvent();
		util.getPlayer().getC64().getEventScheduler().scheduleThreadSafe(highResolutionEvent);
	}

	private void prepareHighResolutionEvent() {
		lastTime = util.getPlayer().getC64().getEventScheduler().getTime(Phase.PHI2);
		CPUClock cpuClock = util.getPlayer().getC64().getClock();
		ticksPerFrame = (long) (cpuClock.getCpuFrequency() / cpuClock.getScreenRefresh());
	}

	private void startOscilloscope() {
		/* Initially clear all gauges (unused SIDs inclusive) */
		util.getPlayer().getC64().configureSIDs((chipNum, sid) -> updateGauges(chipNum, Gauge::reset));

		pauseTransition.setOnFinished(evt -> util.getPlayer().getC64()
				.configureSIDs((chipNum, sid) -> updateGauges(chipNum, gauge -> gauge.updateGauge(sid))));
		sequentialTransition.setCycleCount(Timeline.INDEFINITE);
		sequentialTransition.playFromStart();
	}

	private void stopOscilloscope() {
		sequentialTransition.stop();
	}

	@Override
	public void doClose() {
		util.getPlayer().getC64().getEventScheduler().cancel(highResolutionEvent);
		stopOscilloscope();
		util.getPlayer().stateProperty().removeListener(listener);
	}

	@FXML
	private void doMuteVoice1() {
		util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(0, muteVoice1.isSelected()));
	}

	@FXML
	private void doMuteVoice2() {
		util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(1, muteVoice2.isSelected()));
	}

	@FXML
	private void doMuteVoice3() {
		util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(2, muteVoice3.isSelected()));
	}

	@FXML
	private void doMuteVoice4() {
		util.getPlayer().configureSID(0, sid -> sid.setVoiceMute(3, muteVoice4.isSelected()));
	}

	@FXML
	private void doMuteVoice5() {
		util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(0, muteVoice5.isSelected()));
	}

	@FXML
	private void doMuteVoice6() {
		util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(1, muteVoice6.isSelected()));
	}

	@FXML
	private void doMuteVoice7() {
		util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(2, muteVoice7.isSelected()));
	}

	@FXML
	private void doMuteVoice8() {
		util.getPlayer().configureSID(1, sid -> sid.setVoiceMute(3, muteVoice8.isSelected()));
	}

	@FXML
	private void doMuteVoice9() {
		util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(0, muteVoice9.isSelected()));
	}

	@FXML
	private void doMuteVoice10() {
		util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(1, muteVoice10.isSelected()));
	}

	@FXML
	private void doMuteVoice11() {
		util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(2, muteVoice11.isSelected()));
	}

	@FXML
	private void doMuteVoice12() {
		util.getPlayer().configureSID(2, sid -> sid.setVoiceMute(3, muteVoice12.isSelected()));
	}

	/**
	 * Sample audio from provided SID.
	 * 
	 * @param chipNum           SID chip number
	 * @param sid               provided SID
	 * @param isLowerResolution lower resolution event occurred (less precision)
	 */
	private void sampleGauges(Integer chipNum, SIDEmu sid, boolean isLowerResolution) {
		switch (chipNum) {
		case 0:
			waveMono_0.sample(sid).advance();
			waveMono_1.sample(sid).advance();
			waveMono_2.sample(sid).advance();
			envMono_0.sample(sid);
			envMono_1.sample(sid);
			envMono_2.sample(sid);
			freqMono_0.sample(sid);
			freqMono_1.sample(sid);
			freqMono_2.sample(sid);
			volumeMono.sample(sid).advance();
			resonanceMono.sample(sid);
			filterMono.sample(sid);
			if (isLowerResolution) {
				envMono_0.advance();
				envMono_1.advance();
				envMono_2.advance();
				freqMono_0.advance();
				freqMono_1.advance();
				freqMono_2.advance();
				resonanceMono.advance();
				filterMono.advance();
			}
			break;

		case 1:
			waveStereo_0.sample(sid).advance();
			waveStereo_1.sample(sid).advance();
			waveStereo_2.sample(sid).advance();
			envStereo_0.sample(sid);
			envStereo_1.sample(sid);
			envStereo_2.sample(sid);
			freqStereo_0.sample(sid);
			freqStereo_1.sample(sid);
			freqStereo_2.sample(sid);
			volumeStereo.sample(sid).advance();
			resonanceStereo.sample(sid);
			filterStereo.sample(sid);
			if (isLowerResolution) {
				envStereo_0.advance();
				envStereo_1.advance();
				envStereo_2.advance();
				freqStereo_0.advance();
				freqStereo_1.advance();
				freqStereo_2.advance();
				resonanceStereo.advance();
				filterStereo.advance();
			}
			break;
		case 2:
			wave3Sid_0.sample(sid).advance();
			wave3Sid_1.sample(sid).advance();
			wave3Sid_2.sample(sid).advance();
			env3Sid_0.sample(sid);
			env3Sid_1.sample(sid);
			env3Sid_2.sample(sid);
			freq3Sid_0.sample(sid);
			freq3Sid_1.sample(sid);
			freq3Sid_2.sample(sid);
			volume3Sid.sample(sid).advance();
			resonance3Sid.sample(sid);
			filter3Sid.sample(sid);
			if (isLowerResolution) {
				env3Sid_0.advance();
				env3Sid_1.advance();
				env3Sid_2.advance();
				freq3Sid_0.advance();
				freq3Sid_1.advance();
				freq3Sid_2.advance();
				resonance3Sid.advance();
				filter3Sid.advance();
			}
			break;

		default:
			break;
		}
	}

	/**
	 * Update gauges using provided consumer.
	 * 
	 * @param chipNum SID chip number
	 * @param updater update method
	 */
	private void updateGauges(Integer chipNum, Consumer<Gauge> updater) {
		switch (chipNum) {
		case 0:
			updater.accept(waveMono_0);
			updater.accept(waveMono_1);
			updater.accept(waveMono_2);
			updater.accept(envMono_0);
			updater.accept(envMono_1);
			updater.accept(envMono_2);
			updater.accept(freqMono_0);
			updater.accept(freqMono_1);
			updater.accept(freqMono_2);
			updater.accept(volumeMono);
			updater.accept(resonanceMono);
			updater.accept(filterMono);
			break;

		case 1:
			updater.accept(waveStereo_0);
			updater.accept(waveStereo_1);
			updater.accept(waveStereo_2);
			updater.accept(envStereo_0);
			updater.accept(envStereo_1);
			updater.accept(envStereo_2);
			updater.accept(freqStereo_0);
			updater.accept(freqStereo_1);
			updater.accept(freqStereo_2);
			updater.accept(volumeStereo);
			updater.accept(resonanceStereo);
			updater.accept(filterStereo);
			break;
		case 2:
			updater.accept(wave3Sid_0);
			updater.accept(wave3Sid_1);
			updater.accept(wave3Sid_2);
			updater.accept(env3Sid_0);
			updater.accept(env3Sid_1);
			updater.accept(env3Sid_2);
			updater.accept(freq3Sid_0);
			updater.accept(freq3Sid_1);
			updater.accept(freq3Sid_2);
			updater.accept(volume3Sid);
			updater.accept(resonance3Sid);
			updater.accept(filter3Sid);
			break;

		default:
			break;
		}
	}

}
