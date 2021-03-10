package sidplay.audio.siddump;

import static libsidplay.common.SIDChip.REG_COUNT;

import java.text.SimpleDateFormat;
import java.util.Date;

import libsidplay.common.CPUClock;
import libsidplay.common.SIDListener;
import libsidplay.components.mos6510.IMOS6510Extension;

public abstract class SIDDumpExtension implements SIDListener, IMOS6510Extension {

	public static final String FILTER_NAME[] = { "Off", "Low", "Bnd", "L+B", "Hi ", "L+H", "B+H", "LBH" };

	public static final char FREQ_TBL_LO[] = { 0x17, 0x27, 0x39, 0x4b, 0x5f, 0x74, 0x8a, 0xa1, 0xba, 0xd4, 0xf0, 0x0e,
			0x2d, 0x4e, 0x71, 0x96, 0xbe, 0xe8, 0x14, 0x43, 0x74, 0xa9, 0xe1, 0x1c, 0x5a, 0x9c, 0xe2, 0x2d, 0x7c, 0xcf,
			0x28, 0x85, 0xe8, 0x52, 0xc1, 0x37, 0xb4, 0x39, 0xc5, 0x5a, 0xf7, 0x9e, 0x4f, 0x0a, 0xd1, 0xa3, 0x82, 0x6e,
			0x68, 0x71, 0x8a, 0xb3, 0xee, 0x3c, 0x9e, 0x15, 0xa2, 0x46, 0x04, 0xdc, 0xd0, 0xe2, 0x14, 0x67, 0xdd, 0x79,
			0x3c, 0x29, 0x44, 0x8d, 0x08, 0xb8, 0xa1, 0xc5, 0x28, 0xcd, 0xba, 0xf1, 0x78, 0x53, 0x87, 0x1a, 0x10, 0x71,
			0x42, 0x89, 0x4f, 0x9b, 0x74, 0xe2, 0xf0, 0xa6, 0x0e, 0x33, 0x20, 0xff };

	public static final char FREQ_TBL_HI[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02,
			0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05,
			0x06, 0x06, 0x06, 0x07, 0x07, 0x08, 0x08, 0x09, 0x09, 0x0a, 0x0a, 0x0b, 0x0c, 0x0d, 0x0d, 0x0e, 0x0f, 0x10,
			0x11, 0x12, 0x13, 0x14, 0x15, 0x17, 0x18, 0x1a, 0x1b, 0x1d, 0x1f, 0x20, 0x22, 0x24, 0x27, 0x29, 0x2b, 0x2e,
			0x31, 0x34, 0x37, 0x3a, 0x3e, 0x41, 0x45, 0x49, 0x4e, 0x52, 0x57, 0x5c, 0x62, 0x68, 0x6e, 0x75, 0x7c, 0x83,
			0x8b, 0x93, 0x9c, 0xa5, 0xaf, 0xb9, 0xc4, 0xd0, 0xdd, 0xea, 0xf8, 0xff };

	protected static final char FREQ_TBL_LO_USE[] = new char[FREQ_TBL_LO.length];
	protected static final char FREQ_TBL_HI_USE[] = new char[FREQ_TBL_HI.length];

	private boolean fFirstTime;

	protected long fFrames;

	/** parameter: when to start **/
	protected long fFirstframe;

	protected float fOldNoteFactor = 1.f;

	protected int fBaseFreq = 0;

	protected int fBaseNote = 0xb0;

	protected boolean fLowRes = false;

	protected int fNoteSpacing = 0;

	protected int fPatternSpacing = 0;

	protected boolean fTimeInSeconds = true;

	private int fCounter;

	private int fRows;

	private final Channel fChannel[] = new Channel[3];

	private final Channel fPrevChannel[] = new Channel[3];

	private final Channel fPrevChannel2[] = new Channel[3];

	private Filter fFilter;
	private Filter fPrevFilter;

	private int fPatternNum;

	private int fNoteNum;

	private final byte[] registers = new byte[REG_COUNT];

	protected CPUClock cpuClock;

	public void init(CPUClock cpuClock) {
		this.cpuClock = cpuClock;
		clearChannelStructures();
		recalibrateFreqTable();
		fFirstTime = true;
		fPatternNum = 1;
		fNoteNum = 1;
	}

	@Override
	public void write(int addr, byte data) {
		registers[addr & REG_COUNT - 1] = data;
	}

	@Override
	public void jmpJsr() {
		// ignore first call
		if (fFirstTime) {
			fFirstTime = false;
			return;
		}

		// Get SID parameters from each channel and the filter
		for (int channel = 0; channel < 3; channel++) {
			fChannel[channel].read(channel, registers);
		}
		fFilter.read(registers);

		// Frame display if first frame to be recorded is reached
		if (isWithinTimeWindow()) {
			SidDumpOutput output = new SidDumpOutput();
			final long time = fFrames - fFirstframe;

			output.setTime(getTime(time, fTimeInSeconds));

			// Loop for each channel
			for (int c = 0; c < 3; c++) {
				int newnote = 0;

				// Keyoff-keyon sequence detection
				if (fChannel[c].getWave() >= 0x10) {
					if (0 != (fChannel[c].getWave() & 1)
							&& (0 == (fPrevChannel2[c].getWave() & 1) || fPrevChannel2[c].getWave() < 0x10)) {
						fPrevChannel[c].setNote(-1);
					}
				}

				// Frequency
				if (fFrames == fFirstframe || fPrevChannel[c].getNote() == -1
						|| fChannel[c].getFreq() != fPrevChannel[c].getFreq()) {
					int d;
					int dist = 0x7fffffff;
					final int delta = fChannel[c].getFreq() - fPrevChannel2[c].getFreq();
					output.setFreq(String.format("%04X", fChannel[c].getFreq()), c);

					if (fChannel[c].getWave() >= 0x10) {
						// Get new note number
						for (d = 0; d < 96; d++) {
							final int cmpfreq = FREQ_TBL_LO_USE[d] | FREQ_TBL_HI_USE[d] << 8;
							final int freq = fChannel[c].getFreq();

							if (Math.abs(freq - cmpfreq) < dist) {
								dist = Math.abs(freq - cmpfreq);
								// Favor the old note
								if (d == fPrevChannel[c].getNote()) {
									dist /= fOldNoteFactor;
								}
								fChannel[c].setNote(d);
							}
						}
						// Print new note
						if (fChannel[c].getNote() != fPrevChannel[c]
								.getNote() /*
											 * || changes [ FREQ_LO_1 + 7 * c] || changes [ FREQ_HI_1 + 7 * c]
											 */) {
							if (fPrevChannel[c].getNote() == -1) {
								if (fLowRes) {
									newnote = 1;
								}
								output.setNote(fChannel[c].getNote(false), c);
							} else {
								output.setNote(fChannel[c].getNote(true), c);
							}
						} else {
							// If same note, print frequency change
							// (slide/vibrato)
							if (delta != 0) {
								if (delta > 0) {
									output.setNote(String.format("(+ %04X)", delta), c);
								} else {
									output.setNote(String.format("(- %04X)", -delta), c);
								}
							} else {
								output.setNote(" ... .. ", c);
							}
						}
					} else {
						output.setNote(" ... .. ", c);
					}
				} else {
					output.setFreq("....", c);
					output.setNote(" ... .. ", c);
				}

				// Waveform
				if (fFrames == fFirstframe || newnote != 0 || fChannel[c].getWave() != fPrevChannel[c].getWave()) {
					output.setWf(String.format("%02X", fChannel[c].getWave()), c);
				} else {
					output.setWf("..", c);
				}

				// ADSR
				if (fFrames == fFirstframe || newnote != 0 || fChannel[c].getAdsr() != fPrevChannel[c].getAdsr()) {
					output.setAdsr(String.format("%04X", fChannel[c].getAdsr()), c);
				} else {
					output.setAdsr("....", c);
				}

				// Pulse
				if (fFrames == fFirstframe || newnote != 0 || fChannel[c].getPulse() != fPrevChannel[c].getPulse()) {
					output.setPul(String.format("%03X", fChannel[c].getPulse()), c);
				} else {
					output.setPul("...", c);
				}
			}
			// Filter cutoff
			if (fFrames == fFirstframe || fFilter.getCutoff() != fPrevFilter.getCutoff()) {
				output.setFcut(String.format("%04X", fFilter.getCutoff()));
			} else {
				output.setFcut("....");
			}

			// Filter control
			if (fFrames == fFirstframe || fFilter.getCtrl() != fPrevFilter.getCtrl()) {
				output.setRc(String.format("%02X", fFilter.getCtrl()));
			} else {
				output.setRc("..");
			}

			// Filter passband
			if (fFrames == fFirstframe || (fFilter.getType() & 0x70) != (fPrevFilter.getType() & 0x70)) {
				output.setTyp(FILTER_NAME[fFilter.getType() >> 4 & 0x7]);
			} else {
				output.setTyp("...");
			}

			// Mastervolume
			if (fFrames == fFirstframe || (fFilter.getType() & 0xf) != (fPrevFilter.getType() & 0xf)) {
				output.setV(String.format("%01X", fFilter.getType() & 0xf));
			} else {
				output.setV(".");
			}

			// End of frame display, print info so far and copy SID
			// registers to
			// old registers
			if (!fLowRes || 0 == (fFrames - fFirstframe) % fNoteSpacing) {
				add(output);
				for (int channel = 0; channel < 3; channel++) {
					fPrevChannel[channel].assign(fChannel[channel]);
				}
				fPrevFilter.assign(fFilter);
			}
			for (int channel = 0; channel < 3; channel++) {
				fPrevChannel2[channel].assign(fChannel[channel]);
			}

			// Print note/pattern separators
			if (fNoteSpacing != 0) {
				fCounter++;
				if (fCounter >= fNoteSpacing) {
					fCounter = 0;
					if (fPatternSpacing != 0) {
						fRows++;
						if (fRows >= fPatternSpacing) {
							fRows = 0;
							addPatternSpacing();
						} else if (!fLowRes) {
							addNoteSpacing();
						}
					} else if (!fLowRes) {
						addNoteSpacing();
					}
				}
			}
		}
		fFrames++;
	}

	private String getTime(long timeInFrames, boolean timeInSeconds) {
		if (!timeInSeconds) {
			return String.format("%5d", timeInFrames);
		} else {
			return new SimpleDateFormat("m:ss.SSS")
					.format(new Date((long) (timeInFrames * 1000 / cpuClock.getScreenRefresh())));
		}
	}

	/**
	 * Put a note spacing row into the table
	 */
	private void addNoteSpacing() {
		final SidDumpOutput noteSep = new SidDumpOutput();
		noteSep.setTime(String.format("-N%03X", fNoteNum++));
		for (int c = 0; c < 3; c++) {
			noteSep.setFreq("----", c);
			noteSep.setNote("--------", c);
			noteSep.setWf("--", c);
			noteSep.setAdsr("----", c);
			noteSep.setPul("---", c);
		}
		noteSep.setFcut("----");
		noteSep.setRc("--");
		noteSep.setTyp("---");
		noteSep.setV("-");
		add(noteSep);
	}

	/**
	 * Put a pattern spacing row into the table
	 */
	private void addPatternSpacing() {
		fNoteNum = 1;
		final SidDumpOutput patternSep = new SidDumpOutput();
		patternSep.setTime(String.format("=P%03X", fPatternNum++));
		for (int c = 0; c < 3; c++) {
			patternSep.setFreq("====", c);
			patternSep.setNote("========", c);
			patternSep.setWf("==", c);
			patternSep.setAdsr("====", c);
			patternSep.setPul("===", c);
		}
		patternSep.setFcut("====");
		patternSep.setRc("==");
		patternSep.setTyp("===");
		patternSep.setV("=");
		add(patternSep);
	}

	private void clearChannelStructures() {
		// Clear channel structures in preparation & print first time info
		for (int ch = 0; ch < 3; ch++) {
			fChannel[ch] = new Channel();
			fPrevChannel[ch] = new Channel();
			fPrevChannel2[ch] = new Channel();
		}
		fFilter = new Filter();
		fPrevFilter = new Filter();
		fFrames = 0;
		fCounter = 0;
		fRows = 0;

		// Check other parameters for correctness
		if (fLowRes && 0 == fNoteSpacing) {
			fLowRes = false;
		}
	}

	private void recalibrateFreqTable() {
		System.arraycopy(FREQ_TBL_LO, 0, FREQ_TBL_LO_USE, 0, FREQ_TBL_LO.length);
		System.arraycopy(FREQ_TBL_HI, 0, FREQ_TBL_HI_USE, 0, FREQ_TBL_HI.length);
		// Re-calibrate frequency table
		if (fBaseFreq != 0) {
			fBaseNote &= 0x7f;
			if (fBaseNote < 0 || fBaseNote > 96) {
				System.err.println("Warning: Calibration note out of range. Aborting recalibration.");
			} else {
				for (int c = 0; c < 96; c++) {
					final double note = c - fBaseNote;
					double freq = fBaseFreq * Math.pow(2.0, note / 12.0);
					final int f = (int) freq;
					if (freq > 0xffff) {
						freq = 0xffff;
					}
					FREQ_TBL_LO_USE[c] = (char) (f & 0xff);
					FREQ_TBL_HI_USE[c] = (char) (f >> 8);
				}
			}
		}
	}

	public abstract void add(final SidDumpOutput output);

	public abstract boolean isWithinTimeWindow();

}
