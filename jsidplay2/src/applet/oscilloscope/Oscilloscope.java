package applet.oscilloscope;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;

import libsidplay.Player;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import applet.TuneTab;
import applet.events.IMuteVoice;
import applet.events.UIEvent;

/**
 * @author Ken Händel
 */
public class Oscilloscope extends TuneTab {

	private SwingEngine swix;

	protected JCheckBox muteVoice1, muteVoice2, muteVoice3, muteVoice4,
			muteVoice5, muteVoice6;
	protected SIDGauge waveMono_0, waveMono_1, waveMono_2, waveStereo_0,
			waveStereo_1, waveStereo_2;
	protected SIDGauge envMono_0, envMono_1, envMono_2, envStereo_0,
			envStereo_1, envStereo_2;
	protected SIDGauge freqMono_0, freqMono_1, freqMono_2, freqStereo_0,
			freqStereo_1, freqStereo_2;
	protected SIDGauge volumeMono, volumeStereo, resonanceMono,
			resonanceStereo, filterMono, filterStereo;

	protected final SIDGauge[][][] gauges = new SIDGauge[2][4][3];

	int repaint = 0;

	protected final HighResolutionEvent highResolutionEvent = new HighResolutionEvent();

	protected class HighResolutionEvent extends Event {
		private EventScheduler ctx;

		public HighResolutionEvent() {
			super("High Resolution SID Register Sampler");
		}

		public void beginScheduling(final EventScheduler _ctx) {
			ctx = _ctx;
			ctx.schedule(this, 0, Event.Phase.PHI2);
		}

		@Override
		public void event() {
			for (int i = 0; i < 2; i++) {
				SIDEmu sidemu = player.getC64().getSID(i);
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

					if (isVisible() && (repaint & 255) == 0) {
						gauges[i][j][0].updateGauge(sidemu);
						gauges[i][j][1].updateGauge(sidemu);
						gauges[i][j][2].updateGauge(sidemu);
					}
				}
			}

			++repaint;
			ctx.schedule(this, 128);
		}
	}

	protected Player player;

	public Action doMuteVoice1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(0, 0, muteVoice1.isSelected());
		}
	};

	public Action doMuteVoice2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(0, 1, muteVoice2.isSelected());
		}
	};

	public Action doMuteVoice3 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(0, 2, muteVoice3.isSelected());
		}
	};

	public Action doMuteVoice4 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(1, 0, muteVoice4.isSelected());
		}
	};

	public Action doMuteVoice5 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(1, 1, muteVoice5.isSelected());
		}
	};

	public Action doMuteVoice6 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			setVoiceMute(1, 2, muteVoice6.isSelected());
		}
	};

	public Oscilloscope(final Player pl) {
		this.player = pl;
		createContents();
	}

	private void createContents() {
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("wavegauge", WaveGauge.class);
			swix.getTaglib().registerTag("envelopegauge", EnvelopeGauge.class);
			swix.getTaglib()
					.registerTag("frequencygauge", FrequencyGauge.class);
			swix.getTaglib().registerTag("volumegauge", VolumeGauge.class);
			swix.getTaglib()
					.registerTag("resonancegauge", ResonanceGauge.class);
			swix.getTaglib().registerTag("filtergauge", FilterGauge.class);
			swix.insert(Oscilloscope.class.getResource("Oscilloscope.xml"),
					this);

			fillComboBoxes();
			setDefaultsAndActions();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void setDefaultsAndActions() {
		waveMono_0.setLocalizer(swix.getLocalizer());
		waveMono_1.setLocalizer(swix.getLocalizer());
		waveMono_2.setLocalizer(swix.getLocalizer());
		waveStereo_0.setLocalizer(swix.getLocalizer());
		waveStereo_1.setLocalizer(swix.getLocalizer());
		waveStereo_2.setLocalizer(swix.getLocalizer());

		// Mono SID gauges
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
	}

	private void fillComboBoxes() {
		// nothing to do
	}

	protected void setVoiceMute(final int sidNum, final int voiceNo,
			final boolean mute) {
		player.mute(sidNum, voiceNo, mute);
		getUiEvents().fireEvent(IMuteVoice.class, new IMuteVoice() {

			public int getSidNum() {
				return sidNum;
			}

			public int getVoice() {
				return voiceNo;
			}

			public boolean isMute() {
				return mute;
			}

		});
	}

	@Override
	public void setTune(final Player m_engine, final SidTune m_tune) {
		final EventScheduler ctx = player.getC64().getEventScheduler();
		/* sample oscillator buffer */
		highResolutionEvent.beginScheduling(ctx);

		for (int i = 0; i < gauges.length; i++) {
			for (int j = 0; j < gauges[i].length; j++) {
				for (int k = 0; k < gauges[i][j].length; k++) {
					gauges[i][j][k].reset();
				}
			}
		}

		setVoiceMute(0, 0, muteVoice1.isSelected());
		setVoiceMute(0, 1, muteVoice2.isSelected());
		setVoiceMute(0, 2, muteVoice3.isSelected());
		setVoiceMute(1, 0, muteVoice4.isSelected());
		setVoiceMute(1, 1, muteVoice5.isSelected());
		setVoiceMute(1, 2, muteVoice6.isSelected());
	}

	public void notify(final UIEvent evt) {
		if (evt.isOfType(IMuteVoice.class)) {
			final IMuteVoice muteVoice = (IMuteVoice) evt.getUIEventImpl();
			final int sidNum = muteVoice.getSidNum();
			final int voice = muteVoice.getVoice();
			if (sidNum == 0) {
				if (voice == 0) {
					muteVoice1.setSelected(muteVoice.isMute());
				} else if (voice == 1) {
					muteVoice2.setSelected(muteVoice.isMute());
				} else if (voice == 2) {
					muteVoice3.setSelected(muteVoice.isMute());
				}
			} else {
				if (voice == 0) {
					muteVoice4.setSelected(muteVoice.isMute());
				} else if (voice == 1) {
					muteVoice5.setSelected(muteVoice.isMute());
				} else if (voice == 2) {
					muteVoice6.setSelected(muteVoice.isMute());
				}
			}
		}
	}
}
