package ui.siddump;

import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FILTERFREQ_LO;

import java.util.Collection;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;

import javafx.collections.ObservableList;
import libsidplay.config.IConfig;
import libsidutils.SIDDumpConfiguration.SIDDumpReg;
import netsiddev.AudioGeneratorThread;
import netsiddev.InvalidCommandException;
import netsiddev.SIDWrite;
import resid_builder.resid.SID;
import sidplay.audio.AudioConfig;

public class SidDumpReplayer {
	private static final int SID_WRITE_DELAY = 6;

	private static final String FILTER_NAME[] = { "Off", "Low", "Bnd", "L+B",
			"Hi ", "L+H", "B+H", "LBH" };

	private boolean fAborted;

	/**
	 * Replay option: register write order
	 */
	private Collection<SIDDumpReg> fRegOrder;

	/**
	 * Replay option: Frequency of player address calls
	 */
	private int fReplayFreq = 50;

	private float leftVolume;

	private IConfig cfg;

	private AudioGeneratorThread agt;

	public SidDumpReplayer(IConfig cfg) {
		this.cfg = cfg;
	}

	public void setRegOrder(final Collection<SIDDumpReg> collection) {
		this.fRegOrder = collection;
	}

	public void setReplayFrequency(final int freq) {
		fReplayFreq = freq;
	}

	public void setLeftVolume(float f) {
		this.leftVolume = f;
	}

	public void replay(ObservableList<SidDumpOutput> sidDumpOutputs)
			throws InvalidCommandException {

		if (fRegOrder == null) {
			// no SID write order, no playback!
			return;
		}

		/* FIXME: configure this SID. It is a 8580. */
		SID sid = new SID();

		/*
		 * FIXME: support for HardSID playback of recordings is lost. Will fix
		 * later.
		 */
		agt = new AudioGeneratorThread(AudioConfig.getInstance(cfg
				.getAudioSection()));
		agt.setSidArray(new SID[] { sid });
		agt.setLevelAdjustment(0, decibelsToCentibels());
		BlockingQueue<SIDWrite> queue = agt.getSidCommandQueue();
		agt.start();

		try {
			// reset replay queue
			byte volume = 0xf;

			// for each row do replay
			for (int rown = 0; rown < sidDumpOutputs.size(); rown++) {
				final SidDumpOutput row = sidDumpOutputs.get(rown);

				final String firstCol = row.getTime();
				if (firstCol.startsWith("=")) {
					// ignore pattern spacing
					continue;
				} else if (firstCol.startsWith("-")) {
					// ignore note spacing
					continue;
				}

				final Vector<SidDumpOutput> examineRows = new Vector<SidDumpOutput>();
				for (int i = 0; i < 20; i++) {
					examineRows.add(row);
				}

				int cmd = 0;
				long time = 0;
				for (final SIDDumpReg aFRegOrder : fRegOrder) {
					String col;
					int coln;
					byte register = aFRegOrder.getRegister();

					switch (aFRegOrder) {
					case ATTACK_DECAY_1:
					case SUSTAIN_RELEASE_1:
					case ATTACK_DECAY_2:
					case SUSTAIN_RELEASE_2:
					case ATTACK_DECAY_3:
					case SUSTAIN_RELEASE_3:
						// ADSR
						coln = register / 7 * 5 + 4;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}

						final int adsr = Integer.valueOf(col, 16);
						if ((register + 2) % 7 == 0) {
							// ATTACK/DECAY
							queue.put(new SIDWrite(0, register,
									(byte) (adsr >> 8), SID_WRITE_DELAY));
						} else {
							// SUSTAIN/RELEASE
							queue.put(new SIDWrite(0, register,
									(byte) (adsr & 0xff), SID_WRITE_DELAY));
						}
						cmd++;
						break;

					case FREQ_LO_1:
					case FREQ_HI_1:
					case FREQ_LO_2:
					case FREQ_HI_2:
					case FREQ_HI_3:
					case FREQ_LO_3:
						// FREQ
						coln = register / 7 * 5 + 1;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}
						final int freq = Integer.valueOf(col, 16);
						if (register % 7 == 0) {
							// FREQ_LO
							queue.put(new SIDWrite(0, register, (byte) freq,
									SID_WRITE_DELAY));
						} else {
							// FREQ_HI
							queue.put(new SIDWrite(0, register,
									(byte) (freq >> 8), SID_WRITE_DELAY));
						}
						cmd++;
						break;

					case PULSE_LO_1:
					case PULSE_HI_1:
					case PULSE_LO_2:
					case PULSE_HI_2:
					case PULSE_LO_3:
					case PULSE_HI_3:
						// PULSE
						coln = register / 7 * 5 + 5;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}

						final int pulse = Integer.valueOf(col.trim(), 16);
						if ((register + 5) % 7 == 0) {
							queue.put(new SIDWrite(0, register, (byte) pulse,
									SID_WRITE_DELAY));
						} else {
							queue.put(new SIDWrite(0, register,
									(byte) (pulse >> 8), SID_WRITE_DELAY));
						}
						cmd++;
						break;

					case WAVEFORM_1:
					case WAVEFORM_2:
					case WAVEFORM_3:
						// WF
						coln = register / 7 * 5 + 3;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}
						final int wf = Integer.valueOf(col.trim(), 16);
						queue.put(new SIDWrite(0, register, (byte) wf,
								SID_WRITE_DELAY));
						cmd++;
						break;

					case FILTERFREQ_LO:
					case FILTERFREQ_HI:
						// FCut
						coln = 16;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}
						final int fcut = Integer.valueOf(col.trim(), 16);
						if (aFRegOrder == FILTERFREQ_LO) {
							// FILTERFREQ_LO
							queue.put(new SIDWrite(0, register,
									(byte) (fcut >> 5 & 0x07), SID_WRITE_DELAY));
						} else {
							// FILTERFREQ_HI
							queue.put(new SIDWrite(0, register,
									(byte) (fcut >> 8), SID_WRITE_DELAY));
						}
						cmd++;
						break;

					case FILTERCTRL:
						// Ctrl
						coln = 17;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}
						final int typ = Integer.valueOf(col.trim(), 16);
						queue.put(new SIDWrite(0, register, (byte) typ,
								SID_WRITE_DELAY));
						cmd++;
						break;

					case VOL:
						// Typ und Mastervolume
						coln = 18;
						String colFilt = getColumnValue(examineRows.get(coln),
								coln);
						coln = 19;
						String colMast = getColumnValue(examineRows.get(coln),
								coln);

						if (colFilt.startsWith(".") && colMast.startsWith(".")) {
							break;
						}

						if (!colMast.startsWith(".")) {
							volume = (byte) (volume & 0xf0 | Integer.parseInt(
									colMast, 16));
						}
						if (!colFilt.startsWith(".")) {
							String cmp = colFilt.trim();
							for (int j = 0; j < FILTER_NAME.length; j++) {
								if (FILTER_NAME[j].equals(cmp)) {
									volume = (byte) (j << 4 | volume & 0xf);
									break;
								}
							}
						}
						queue.put(new SIDWrite(0, register, volume,
								SID_WRITE_DELAY));
						cmd++;
						break;

					default:
						break;
					}
				}

				/* Fill up to 1 frame delay */
				queue.put(SIDWrite.makePureDelay(0, 1000000 / fReplayFreq - cmd
						* SID_WRITE_DELAY));

				time += 1000000 / fReplayFreq;
				while (agt.getPlaybackClock() < time - 100000) {
					agt.ensureDraining();
					Thread.sleep(10);
				}

				if (fAborted) {
					throw new InterruptedException();
				}
			}

			/* Wait until queue drain. */
			queue.put(SIDWrite.makeEnd());
			do {
				agt.ensureDraining();
				agt.join(1000);
			} while (agt.isAlive());
		} catch (InterruptedException e) {
		} finally {
			agt.interrupt();
			fAborted = false;
		}
	}

	private String getColumnValue(SidDumpOutput row, int coli) {
		switch (coli) {
		case 0:
			return row.getTime();
		case 1:
			return row.getFreq(0);
		case 2:
			return row.getNote(0);
		case 3:
			return row.getWf(0);
		case 4:
			return row.getAdsr(0);
		case 5:
			return row.getPul(0);
		case 6:
			return row.getFreq(1);
		case 7:
			return row.getNote(1);
		case 8:
			return row.getWf(1);
		case 9:
			return row.getAdsr(1);
		case 10:
			return row.getPul(1);
		case 11:
			return row.getFreq(2);
		case 12:
			return row.getNote(2);
		case 13:
			return row.getWf(2);
		case 14:
			return row.getAdsr(2);
		case 15:
			return row.getPul(2);
		case 16:
			return row.getFcut();
		case 17:
			return row.getRc();
		case 18:
			return row.getTyp();
		case 19:
			return row.getV();
		default:
			return null;
		}
	}

	private int decibelsToCentibels() {
		return (int) leftVolume * 10;
	}

	public void stopReplay() {
		try {
			while (agt != null && agt.isAlive()) {
				fAborted = true;
				BlockingQueue<SIDWrite> queue = agt.getSidCommandQueue();
				queue.clear();
				/* Wait until queue drain. */
				queue.put(SIDWrite.makeEnd());
				do {
					agt.ensureDraining();
					agt.join(1000);
				} while (agt.isAlive());
			}
		} catch (InterruptedException e1) {
		}
	}

}
