package sidplay.audio.siddump;

import static libsidplay.common.SIDChip.REG_COUNT;

import java.text.SimpleDateFormat;
import java.util.Date;

import libsidplay.common.CPUClock;
import libsidplay.common.SIDListener;
import libsidplay.components.mos6510.IMOS6510Extension;

public abstract class SIDDumpExtension implements SIDListener, IMOS6510Extension {

	private static final String FILTER_NAME[] = { "Off", "Low", "Bnd", "L+B", "Hi ", "L+H", "B+H", "LBH" };

	private static final char FREQ_TBL_LO[] = { 0x17, 0x27, 0x39, 0x4b, 0x5f, 0x74, 0x8a, 0xa1, 0xba, 0xd4, 0xf0, 0x0e,
			0x2d, 0x4e, 0x71, 0x96, 0xbe, 0xe8, 0x14, 0x43, 0x74, 0xa9, 0xe1, 0x1c, 0x5a, 0x9c, 0xe2, 0x2d, 0x7c, 0xcf,
			0x28, 0x85, 0xe8, 0x52, 0xc1, 0x37, 0xb4, 0x39, 0xc5, 0x5a, 0xf7, 0x9e, 0x4f, 0x0a, 0xd1, 0xa3, 0x82, 0x6e,
			0x68, 0x71, 0x8a, 0xb3, 0xee, 0x3c, 0x9e, 0x15, 0xa2, 0x46, 0x04, 0xdc, 0xd0, 0xe2, 0x14, 0x67, 0xdd, 0x79,
			0x3c, 0x29, 0x44, 0x8d, 0x08, 0xb8, 0xa1, 0xc5, 0x28, 0xcd, 0xba, 0xf1, 0x78, 0x53, 0x87, 0x1a, 0x10, 0x71,
			0x42, 0x89, 0x4f, 0x9b, 0x74, 0xe2, 0xf0, 0xa6, 0x0e, 0x33, 0x20, 0xff };

	private static final char FREQ_TBL_HI[] = { 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02,
			0x02, 0x02, 0x02, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05,
			0x06, 0x06, 0x06, 0x07, 0x07, 0x08, 0x08, 0x09, 0x09, 0x0a, 0x0a, 0x0b, 0x0c, 0x0d, 0x0d, 0x0e, 0x0f, 0x10,
			0x11, 0x12, 0x13, 0x14, 0x15, 0x17, 0x18, 0x1a, 0x1b, 0x1d, 0x1f, 0x20, 0x22, 0x24, 0x27, 0x29, 0x2b, 0x2e,
			0x31, 0x34, 0x37, 0x3a, 0x3e, 0x41, 0x45, 0x49, 0x4e, 0x52, 0x57, 0x5c, 0x62, 0x68, 0x6e, 0x75, 0x7c, 0x83,
			0x8b, 0x93, 0x9c, 0xa5, 0xaf, 0xb9, 0xc4, 0xd0, 0xdd, 0xea, 0xf8, 0xff };

	private char freqTableLo[] = new char[FREQ_TBL_LO.length];
	private char freqTableHi[] = new char[FREQ_TBL_HI.length];

	private long frames;

	public long getFrames() {
		return frames;
	}

	public void setFrames(long frames) {
		this.frames = frames;
	}

	private long firstframe;

	public long getFirstFrame() {
		return firstframe;
	}

	public void setFirstFrame(final long firstFrame) {
		this.firstframe = firstFrame;
	}

	private float oldNoteFactor;

	public float getOldNoteFactor() {
		return oldNoteFactor;
	}

	public void setOldNoteFactor(final float oldNoteFactor) {
		this.oldNoteFactor = oldNoteFactor;
	}

	private int baseFreq;

	public int getBaseFreq() {
		return baseFreq;
	}

	public void setBaseFreq(final int baseFreq) {
		this.baseFreq = baseFreq;
	}

	private int baseNote;

	public int getBaseNote() {
		return baseNote;
	}

	public void setBaseNote(final int baseNote) {
		this.baseNote = baseNote;
	}

	private boolean flowRes;

	public boolean getLowRes() {
		return flowRes;
	}

	public void setLowRes(final boolean lowRes) {
		this.flowRes = lowRes;
	}

	private int noteSpacing;

	public int getNoteSpacing() {
		return noteSpacing;
	}

	public void setNoteSpacing(final int noteSpacing) {
		this.noteSpacing = noteSpacing;
	}

	private int patternSpacing;

	public int getPatternSpacing() {
		return patternSpacing;
	}

	public void setPatternSpacing(final int patternSpacing) {
		this.patternSpacing = patternSpacing;
	}

	private boolean timeInSeconds;

	public boolean getTimeInSeconds() {
		return timeInSeconds;
	}

	public void setTimeInSeconds(final boolean timeInSeconds) {
		this.timeInSeconds = timeInSeconds;
	}

	private final Channel channel[] = new Channel[3];

	private final Channel prevChannel[] = new Channel[3];

	private final Channel prevChannel2[] = new Channel[3];

	private Filter filter;
	private Filter prevFilter;

	private boolean firstTime;

	private int counter, rows, patternNum, noteNum;

	private final byte[] registers = new byte[REG_COUNT];

	private CPUClock cpuClock;

	public SIDDumpExtension() {
		patternNum = 1;
		noteNum = 1;
		oldNoteFactor = 1.f;
		baseNote = 0xb0;
		timeInSeconds = true;
	}

	public void init(CPUClock cpuClock) {
		this.cpuClock = cpuClock;
		firstTime = true;
		clearChannelStructures();
		recalibrateFreqTable();
	}

	public int getMiddleCFreq() {
		return freqTableLo[48] | freqTableHi[48] << 8;
	}

	@Override
	public void write(int addr, byte data) {
		registers[addr & REG_COUNT - 1] = data;
	}

	@Override
	public void jmpJsr() {
		// ignore first call
		if (firstTime) {
			firstTime = false;
			return;
		}

		// Get SID parameters from each channel and the filter
		for (int ch = 0; ch < 3; ch++) {
			channel[ch].read(ch, registers);
		}
		filter.read(registers);

		// Frame display if first frame to be recorded is reached
		if (frames >= firstframe && !isAborted()) {
			SidDumpOutput output = new SidDumpOutput();
			final long time = frames - firstframe;

			output.setTime(getTime(time, timeInSeconds));

			// Loop for each channel
			for (int c = 0; c < 3; c++) {
				int newnote = 0;

				// Keyoff-keyon sequence detection
				if (channel[c].getWave() >= 0x10) {
					if (0 != (channel[c].getWave() & 1)
							&& (0 == (prevChannel2[c].getWave() & 1) || prevChannel2[c].getWave() < 0x10)) {
						prevChannel[c].setNote(-1);
					}
				}

				// Frequency
				if (frames == firstframe || prevChannel[c].getNote() == -1
						|| channel[c].getFreq() != prevChannel[c].getFreq()) {
					int d;
					int dist = 0x7fffffff;
					final int delta = channel[c].getFreq() - prevChannel2[c].getFreq();
					output.setFreq(String.format("%04X", channel[c].getFreq()), c);

					if (channel[c].getWave() >= 0x10) {
						// Get new note number
						for (d = 0; d < 96; d++) {
							final int cmpfreq = freqTableLo[d] | freqTableHi[d] << 8;
							final int freq = channel[c].getFreq();

							if (Math.abs(freq - cmpfreq) < dist) {
								dist = Math.abs(freq - cmpfreq);
								// Favor the old note
								if (d == prevChannel[c].getNote()) {
									dist /= oldNoteFactor;
								}
								channel[c].setNote(d);
							}
						}
						// Print new note
						if (channel[c].getNote() != prevChannel[c]
								.getNote() /*
											 * || changes [ FREQ_LO_1 + 7 * c] || changes [ FREQ_HI_1 + 7 * c]
											 */) {
							if (prevChannel[c].getNote() == -1) {
								if (flowRes) {
									newnote = 1;
								}
								output.setNote(channel[c].getNote(false), c);
							} else {
								output.setNote(channel[c].getNote(true), c);
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
				if (frames == firstframe || newnote != 0 || channel[c].getWave() != prevChannel[c].getWave()) {
					output.setWf(String.format("%02X", channel[c].getWave()), c);
				} else {
					output.setWf("..", c);
				}

				// ADSR
				if (frames == firstframe || newnote != 0 || channel[c].getAdsr() != prevChannel[c].getAdsr()) {
					output.setAdsr(String.format("%04X", channel[c].getAdsr()), c);
				} else {
					output.setAdsr("....", c);
				}

				// Pulse
				if (frames == firstframe || newnote != 0 || channel[c].getPulse() != prevChannel[c].getPulse()) {
					output.setPul(String.format("%03X", channel[c].getPulse()), c);
				} else {
					output.setPul("...", c);
				}
			}
			// Filter cutoff
			if (frames == firstframe || filter.getCutoff() != prevFilter.getCutoff()) {
				output.setFcut(String.format("%04X", filter.getCutoff()));
			} else {
				output.setFcut("....");
			}

			// Filter control
			if (frames == firstframe || filter.getCtrl() != prevFilter.getCtrl()) {
				output.setRc(String.format("%02X", filter.getCtrl()));
			} else {
				output.setRc("..");
			}

			// Filter passband
			if (frames == firstframe || (filter.getType() & 0x70) != (prevFilter.getType() & 0x70)) {
				output.setTyp(FILTER_NAME[filter.getType() >> 4 & 0x7]);
			} else {
				output.setTyp("...");
			}

			// Mastervolume
			if (frames == firstframe || (filter.getType() & 0xf) != (prevFilter.getType() & 0xf)) {
				output.setV(String.format("%01X", filter.getType() & 0xf));
			} else {
				output.setV(".");
			}

			// End of frame display, print info so far and copy SID
			// registers to
			// old registers
			if (!flowRes || 0 == (frames - firstframe) % noteSpacing) {
				add(output);
				for (int ch = 0; ch < 3; ch++) {
					prevChannel[ch].assign(channel[ch]);
				}
				prevFilter.assign(filter);
			}
			for (int ch = 0; ch < 3; ch++) {
				prevChannel2[ch].assign(channel[ch]);
			}

			// Print note/pattern separators
			if (noteSpacing != 0) {
				counter++;
				if (counter >= noteSpacing) {
					counter = 0;
					if (patternSpacing != 0) {
						rows++;
						if (rows >= patternSpacing) {
							rows = 0;
							noteNum = 1;
							addPatternSpacing(patternNum++);
						} else if (!flowRes) {
							addNoteSpacing(noteNum++);
						}
					} else if (!flowRes) {
						addNoteSpacing(noteNum++);
					}
				}
			}
		}
		frames++;
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
	private void addNoteSpacing(int noteNum) {
		final SidDumpOutput noteSep = new SidDumpOutput();
		noteSep.setTime(String.format("-N%03X", noteNum));
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
	private void addPatternSpacing(int patternNum) {
		final SidDumpOutput patternSep = new SidDumpOutput();
		patternSep.setTime(String.format("=P%03X", patternNum));
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
			channel[ch] = new Channel();
			prevChannel[ch] = new Channel();
			prevChannel2[ch] = new Channel();
		}
		filter = new Filter();
		prevFilter = new Filter();
		frames = 0;
		counter = 0;
		rows = 0;

		// Check other parameters for correctness
		if (flowRes && 0 == noteSpacing) {
			flowRes = false;
		}
	}

	private void recalibrateFreqTable() {
		System.arraycopy(FREQ_TBL_LO, 0, freqTableLo, 0, FREQ_TBL_LO.length);
		System.arraycopy(FREQ_TBL_HI, 0, freqTableHi, 0, FREQ_TBL_HI.length);
		// Re-calibrate frequency table
		if (baseFreq != 0) {
			baseNote &= 0x7f;
			if (baseNote < 0 || baseNote > 96) {
				System.err.println("Warning: Calibration note out of range. Aborting recalibration.");
			} else {
				for (int c = 0; c < 96; c++) {
					final double note = c - baseNote;
					double freq = baseFreq * Math.pow(2.0, note / 12.0);
					final int f = (int) freq;
					if (freq > 0xffff) {
						freq = 0xffff;
					}
					freqTableLo[c] = (char) (f & 0xff);
					freqTableHi[c] = (char) (f >> 8);
				}
			}
		}
	}

	public abstract boolean isAborted();

	public abstract void add(final SidDumpOutput output);

}
