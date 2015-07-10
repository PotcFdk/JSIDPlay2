package ui.siddump;

import static libsidutils.SIDDumpConfiguration.SIDDumpReg.ATTACK_DECAY_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FILTERCTRL;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FILTERFREQ_HI;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FILTERFREQ_LO;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FREQ_HI_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.FREQ_LO_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.PULSE_HI_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.PULSE_LO_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.SUSTAIN_RELEASE_1;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.VOL;
import static libsidutils.SIDDumpConfiguration.SIDDumpReg.WAVEFORM_1;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.MatchResult;

import javafx.collections.ObservableList;
import libsidplay.common.SIDEmu;
import libsidplay.components.mos6510.IMOS6510Extension;
import libsidutils.SIDDumpConfiguration.SIDDumpReg;
import netsiddev.InvalidCommandException;
import sidplay.Player;
import ui.entities.config.Configuration;

public abstract class SidDumpExtension implements IMOS6510Extension {

	private static final String NOTE_NAME[] = { "C-0", "C#0", "D-0", "D#0",
			"E-0", "F-0", "F#0", "G-0", "G#0", "A-0", "A#0", "B-0", "C-1",
			"C#1", "D-1", "D#1", "E-1", "F-1", "F#1", "G-1", "G#1", "A-1",
			"A#1", "B-1", "C-2", "C#2", "D-2", "D#2", "E-2", "F-2", "F#2",
			"G-2", "G#2", "A-2", "A#2", "B-2", "C-3", "C#3", "D-3", "D#3",
			"E-3", "F-3", "F#3", "G-3", "G#3", "A-3", "A#3", "B-3", "C-4",
			"C#4", "D-4", "D#4", "E-4", "F-4", "F#4", "G-4", "G#4", "A-4",
			"A#4", "B-4", "C-5", "C#5", "D-5", "D#5", "E-5", "F-5", "F#5",
			"G-5", "G#5", "A-5", "A#5", "B-5", "C-6", "C#6", "D-6", "D#6",
			"E-6", "F-6", "F#6", "G-6", "G#6", "A-6", "A#6", "B-6", "C-7",
			"C#7", "D-7", "D#7", "E-7", "F-7", "F#7", "G-7", "G#7", "A-7",
			"A#7", "B-7" };

	private static final String FILTER_NAME[] = { "Off", "Low", "Bnd", "L+B",
			"Hi ", "L+H", "B+H", "LBH" };

	private static final char FREQ_TBL_LO[] = { 0x17, 0x27, 0x39, 0x4b, 0x5f,
			0x74, 0x8a, 0xa1, 0xba, 0xd4, 0xf0, 0x0e, 0x2d, 0x4e, 0x71, 0x96,
			0xbe, 0xe8, 0x14, 0x43, 0x74, 0xa9, 0xe1, 0x1c, 0x5a, 0x9c, 0xe2,
			0x2d, 0x7c, 0xcf, 0x28, 0x85, 0xe8, 0x52, 0xc1, 0x37, 0xb4, 0x39,
			0xc5, 0x5a, 0xf7, 0x9e, 0x4f, 0x0a, 0xd1, 0xa3, 0x82, 0x6e, 0x68,
			0x71, 0x8a, 0xb3, 0xee, 0x3c, 0x9e, 0x15, 0xa2, 0x46, 0x04, 0xdc,
			0xd0, 0xe2, 0x14, 0x67, 0xdd, 0x79, 0x3c, 0x29, 0x44, 0x8d, 0x08,
			0xb8, 0xa1, 0xc5, 0x28, 0xcd, 0xba, 0xf1, 0x78, 0x53, 0x87, 0x1a,
			0x10, 0x71, 0x42, 0x89, 0x4f, 0x9b, 0x74, 0xe2, 0xf0, 0xa6, 0x0e,
			0x33, 0x20, 0xff };

	private static final char FREQ_TBL_HI[] = { 0x01, 0x01, 0x01, 0x01, 0x01,
			0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x02, 0x02, 0x02, 0x02, 0x02,
			0x02, 0x02, 0x03, 0x03, 0x03, 0x03, 0x03, 0x04, 0x04, 0x04, 0x04,
			0x05, 0x05, 0x05, 0x06, 0x06, 0x06, 0x07, 0x07, 0x08, 0x08, 0x09,
			0x09, 0x0a, 0x0a, 0x0b, 0x0c, 0x0d, 0x0d, 0x0e, 0x0f, 0x10, 0x11,
			0x12, 0x13, 0x14, 0x15, 0x17, 0x18, 0x1a, 0x1b, 0x1d, 0x1f, 0x20,
			0x22, 0x24, 0x27, 0x29, 0x2b, 0x2e, 0x31, 0x34, 0x37, 0x3a, 0x3e,
			0x41, 0x45, 0x49, 0x4e, 0x52, 0x57, 0x5c, 0x62, 0x68, 0x6e, 0x75,
			0x7c, 0x83, 0x8b, 0x93, 0x9c, 0xa5, 0xaf, 0xb9, 0xc4, 0xd0, 0xdd,
			0xea, 0xf8, 0xff };

	private static final char FREQ_TBL_LO_USE[] = new char[FREQ_TBL_LO.length];
	private static final char FREQ_TBL_HI_USE[] = new char[FREQ_TBL_HI.length];

	/**
	 * Frequency of player address calls in Hz
	 */
	private static final int TUNE_SPEED = 50;

	protected static class Channel {
		private int freq, pulse, adsr, wave, note;

		public void read(SIDEmu sid, int channel) {
			freq = sid.readInternalRegister(FREQ_LO_1.getRegister() + 7
					* channel)
					& 0xff
					| (sid.readInternalRegister(FREQ_HI_1.getRegister() + 7
							* channel) & 0xff) << 8;
			pulse = (sid.readInternalRegister(PULSE_LO_1.getRegister() + 7
					* channel) & 0xff | (sid.readInternalRegister(PULSE_HI_1
					.getRegister() + 7 * channel) & 0xff) << 8) & 0xfff;
			wave = sid.readInternalRegister(WAVEFORM_1.getRegister() + 7
					* channel) & 0xff;
			adsr = sid.readInternalRegister(SUSTAIN_RELEASE_1.getRegister() + 7
					* channel)
					& 0xff
					| (sid.readInternalRegister(ATTACK_DECAY_1.getRegister()
							+ 7 * channel) & 0xff) << 8;

		}

		public void assign(Channel channel) {
			adsr = channel.adsr;
			freq = channel.freq;
			note = channel.note;
			pulse = channel.pulse;
			wave = channel.wave;

		}

		public String getFreq() {
			return String.format("%04X", freq);
		}

		public String getNote(boolean prevChannelNote) {
			if (prevChannelNote) {
				return String.format("(%s %02X)", NOTE_NAME[note], note | 0x80);
			} else {
				return String.format(" %s %02X ", NOTE_NAME[note], note | 0x80);
			}
		}

		public String getWf() {
			return String.format("%02X", wave);
		}

		public String getADSR() {
			return String.format("%04X", adsr);
		}

		public String getPul() {
			return String.format("%03X", pulse);
		}
	}

	protected static class Filter {
		private int cutoff, ctrl, type;

		public void read(SIDEmu fSid) {
			cutoff = (fSid.readInternalRegister(FILTERFREQ_LO.getRegister()) & 0xff) << 5
					| (fSid.readInternalRegister(FILTERFREQ_HI.getRegister()) & 0xff) << 8;
			ctrl = fSid.readInternalRegister(FILTERCTRL.getRegister()) & 0xff;
			type = fSid.readInternalRegister(VOL.getRegister()) & 0xff;
		}

		public void assign(Filter filter) {
			ctrl = filter.ctrl;
			cutoff = filter.cutoff;
			type = filter.type;
		}

		public String getCutoff() {
			return String.format("%04X", cutoff);
		}

		public String getCtrl() {
			return String.format("%02X", ctrl);
		}

		public String getTyp() {
			return FILTER_NAME[type >> 4 & 0x7];
		}

		public String getV() {
			return String.format("%01X", type & 0xf);
		}
	}

	private String getTime(long time, boolean timeInSeconds) {
		if (!timeInSeconds) {
			return String.format("%5d", time);
		} else {
			return String.format("%01d:%02d.%02d", time / (TUNE_SPEED * 60),
					time / TUNE_SPEED % 60, time % TUNE_SPEED);
		}
	}

	private final Channel fChannel[] = new Channel[3];

	private final Channel fPrevChannel[] = new Channel[3];

	private final Channel fPrevChannel2[] = new Channel[3];

	private Player player;
	private SIDEmu fSid;

	private Filter fFilter;
	private Filter fPrevFilter;

	/**
	 * tunes load address
	 */
	private int fLoadAddress;

	/**
	 * tunes init address
	 */
	private int fInitAddress;

	/**
	 * tunes player address
	 */
	private int fPlayerAddress;

	/**
	 * tunes song number
	 */
	private int fCurrentSong;

	/**
	 * Total record duration
	 */
	private int fSeconds;

	/**
	 * Total number of recorded frames
	 */
	private long fFrames;

	/**
	 * First frame to record
	 */
	private long fFirstframe;

	private float fOldNoteFactor = 1.f;

	private int fBaseFreq = 0;

	private int fBaseNote = 0xb0;

	private boolean fLowRes = false;

	private int fNoteSpacing = 0;

	private int fPatternSpacing = 0;

	private boolean fTimeInSeconds = true;

	/**
	 * First fetch call?
	 */
	private boolean fFirstTime;

	/**
	 * Note spacing counter
	 */
	private int fCounter;

	/**
	 * Pattern spacing counter
	 */
	private int fRows;

	/**
	 * Replay option: register write order
	 */
	private Collection<SIDDumpReg> fRegOrder = null;

	/**
	 * Replay option: Frequency of player address calls
	 */
	private int fReplayFreq = 50;

	/**
	 * Recorded row number
	 */
	private int fFetchedRow;

	/**
	 * Current pattern number
	 */
	private int fPatternNum;

	/**
	 * Current note pattern number
	 */
	private int fNoteNum;

	private float leftVolume;

	private SidDumpReplayer replayer;

	public SidDumpExtension(Player pl, Configuration cfg) {
		this.player = pl;
	}

	public int getLoadAddress() {
		return fLoadAddress;
	}

	public void setLoadAddress(final int loadAddr) {
		fLoadAddress = loadAddr;
	}

	public int getInitAddress() {
		return fInitAddress;
	}

	public void setInitAddress(final int initAddr) {
		fInitAddress = initAddr;
	}

	public int getPlayerAddress() {
		return fPlayerAddress;
	}

	public void setPayerAddress(final int playAddr) {
		fPlayerAddress = playAddr;
	}

	public int getCurrentSong() {
		return fCurrentSong;
	}

	public void setCurrentSong(final int currentSong) {
		fCurrentSong = currentSong;
	}

	public long getFirstFrame() {
		return fFirstframe;
	}

	public void setFirstFrame(final long firstFrame) {
		fFirstframe = firstFrame;
	}

	public void setRecordLength(final int seconds) {
		fSeconds = seconds;
	}

	public boolean getTimeInSeconds() {
		return fTimeInSeconds;
	}

	public void setTimeInSeconds(final boolean selected) {
		fTimeInSeconds = selected;
	}

	public void setOldNoteFactor(final float oldNoteFactor) {
		fOldNoteFactor = oldNoteFactor;
	}

	public void setBaseFreq(final int baseFreq) {
		fBaseFreq = baseFreq;
	}

	public void setBaseNote(final int baseNote) {
		fBaseNote = baseNote;
	}

	public int getPatternSpacing() {
		return fPatternSpacing;
	}

	public void setPatternSpacing(final int patternSpacing) {
		fPatternSpacing = patternSpacing;
	}

	public int getNoteSpacing() {
		return fNoteSpacing;
	}

	public void setNoteSpacing(final int noteSpacing) {
		fNoteSpacing = noteSpacing;
	}

	public void setLowRes(final boolean lowResolution) {
		fLowRes = lowResolution;
	}

	public boolean getLowRes() {
		return fLowRes;
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

	/**
	 * Initialization routine to prepare recording SID write sequence.
	 */
	public void init() {
		clear();
		clearChannelStructures();
		recalibrateFreqTable();
		fFirstTime = true;
		fFetchedRow = 0;
		fPatternNum = 1;
		fNoteNum = 1;
		player.configureSID(0, sid -> fSid = sid);
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
				System.err
						.println("Warning: Calibration note out of range. Aborting recalibration.");
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

	/**
	 * CPU jmp/jsr instruction callback to catch calls to the player address
	 * 
	 * @param cpuTime
	 *            time in CPU
	 */
	@Override
	public void fetch(final long cpuTime) {
		// ignore first call
		if (fFirstTime) {
			fFirstTime = false;
			return;
		}

		// player is called within time window
		if (fSid == null) {
			return;
		}

		if (fFrames < fFirstframe + fSeconds * TUNE_SPEED) {

			// Get SID parameters from each channel and the filter
			for (int channel = 0; channel < 3; channel++) {
				fChannel[channel].read(fSid, channel);
			}
			fFilter.read(fSid);

			// Frame display if first frame to be recorded is reached
			if (fFrames >= fFirstframe) {
				SidDumpOutput output = new SidDumpOutput();
				final long time = fFrames - fFirstframe;

				output.setTime(getTime(time, fTimeInSeconds));

				// Loop for each channel
				for (int c = 0; c < 3; c++) {
					int newnote = 0;

					// Keyoff-keyon sequence detection
					if (fChannel[c].wave >= 0x10) {
						if (0 != (fChannel[c].wave & 1)
								&& (0 == (fPrevChannel2[c].wave & 1) || fPrevChannel2[c].wave < 0x10)) {
							fPrevChannel[c].note = -1;
						}
					}

					// Frequency
					if (fFrames == fFirstframe || fPrevChannel[c].note == -1
							|| fChannel[c].freq != fPrevChannel[c].freq) {
						int d;
						int dist = 0x7fffffff;
						final int delta = fChannel[c].freq
								- fPrevChannel2[c].freq;
						output.setFreq(fChannel[c].getFreq(), c);

						if (fChannel[c].wave >= 0x10) {
							// Get new note number
							for (d = 0; d < 96; d++) {
								final int cmpfreq = FREQ_TBL_LO_USE[d]
										| FREQ_TBL_HI_USE[d] << 8;
								final int freq = fChannel[c].freq;

								if (Math.abs(freq - cmpfreq) < dist) {
									dist = Math.abs(freq - cmpfreq);
									// Favor the old note
									if (d == fPrevChannel[c].note) {
										dist /= fOldNoteFactor;
									}
									fChannel[c].note = d;
								}
							}
							// Print new note
							if (fChannel[c].note != fPrevChannel[c].note /*
																		 * ||
																		 * changes
																		 * [
																		 * FREQ_LO_1
																		 * + 7 *
																		 * c] ||
																		 * changes
																		 * [
																		 * FREQ_HI_1
																		 * + 7 *
																		 * c]
																		 */) {
								if (fPrevChannel[c].note == -1) {
									if (fLowRes) {
										newnote = 1;
									}
									output.setNote(fChannel[c].getNote(false),
											c);
								} else {
									output.setNote(fChannel[c].getNote(true), c);
								}
							} else {
								// If same note, print frequency change
								// (slide/vibrato)
								if (delta != 0) {
									if (delta > 0) {
										output.setNote(String.format(
												"(+ %04X)", delta), c);
									} else {
										output.setNote(String.format(
												"(- %04X)", -delta), c);
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
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].wave != fPrevChannel[c].wave) {
						output.setWf(fChannel[c].getWf(), c);
					} else {
						output.setWf("..", c);
					}

					// ADSR
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].adsr != fPrevChannel[c].adsr) {
						output.setAdsr(fChannel[c].getADSR(), c);
					} else {
						output.setAdsr("....", c);
					}

					// Pulse
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].pulse != fPrevChannel[c].pulse) {
						output.setPul(fChannel[c].getPul(), c);
					} else {
						output.setPul("...", c);
					}
				}
				// Filter cutoff
				if (fFrames == fFirstframe
						|| fFilter.cutoff != fPrevFilter.cutoff) {
					output.setFcut(fFilter.getCutoff());
				} else {
					output.setFcut("....");
				}

				// Filter control
				if (fFrames == fFirstframe || fFilter.ctrl != fPrevFilter.ctrl) {
					output.setRc(fFilter.getCtrl());
				} else {
					output.setRc("..");
				}

				// Filter passband
				if (fFrames == fFirstframe
						|| (fFilter.type & 0x70) != (fPrevFilter.type & 0x70)) {
					output.setTyp(fFilter.getTyp());
				} else {
					output.setTyp("...");
				}

				// Mastervolume
				if (fFrames == fFirstframe
						|| (fFilter.type & 0xf) != (fPrevFilter.type & 0xf)) {
					output.setV(fFilter.getV());
				} else {
					output.setV(".");
				}

				// End of frame display, print info so far and copy SID
				// registers to
				// old registers
				if (!fLowRes || 0 == (fFrames - fFirstframe) % fNoteSpacing) {
					fFetchedRow++;
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
		}
		fFrames++;
	}

	/**
	 * Re-load a SID dump file
	 * 
	 * @param filename
	 *            file name to load
	 */
	public void load(final String filename) {
		// first clear table
		clear();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename)))) {
			// ignore header
			String lineContents;
			for (int i = 0; i < 7; i++) {
				lineContents = br.readLine();
				// System.out.println("Skipped: " + lineContents);
				if (lineContents == null) {
					System.err.println("unexpected end of file!");
					return;
				}
				switch (i) {
				case 0:
					Scanner sc = new Scanner(lineContents);
					sc.useDelimiter("\n");
					sc.findInLine("Load address: \\$(\\p{XDigit}+) Init address: \\$(\\p{XDigit}+) Play address: \\$(\\p{XDigit}+)");
					MatchResult result = sc.match();
					for (int j = 0; j < result.groupCount(); j++) {
						final String group = result.group(j + 1);
						switch (j) {
						case 0:
							fLoadAddress = readNumber(group, 16);
							break;

						case 1:
							fInitAddress = readNumber(group, 16);
							break;

						case 2:
							fPlayerAddress = readNumber(group, 16);
							break;

						default:
							break;
						}
					}
					break;

				case 1:
					sc = new Scanner(lineContents);
					sc.useDelimiter("\n");
					sc.findInLine("Calling initroutine with subtune (\\d+)");
					result = sc.match();
					for (int j = 0; j < result.groupCount(); j++) {
						final String group = result.group(j + 1);
						switch (j) {
						case 0:
							// Sub Tune
							fCurrentSong = readNumber(group, 10) + 1;
							break;

						default:
							break;
						}
					}
					break;

				case 2:
					sc = new Scanner(lineContents);
					sc.useDelimiter("\n");
					sc.findInLine("Calling playroutine for (\\d+) frames\\, starting from frame (\\d+)");
					result = sc.match();
					for (int j = 0; j < result.groupCount(); j++) {
						final String group = result.group(j + 1);
						switch (j) {
						case 0:
							// frames
							break;

						case 1:
							// first frame
							fFirstframe = readNumber(group, 10);
							break;

						default:
							break;
						}
					}
					break;

				case 3:
					// "Middle C frequency is $1168"
					sc = new Scanner(lineContents);
					sc.useDelimiter("\n");
					sc.findInLine("Middle C frequency is \\$(\\d+)");
					result = sc.match();
					for (int j = 0; j < result.groupCount(); j++) {
						// ??? how to restore that?

						// String group = result.group(j + 1);
						// switch (j) {
						// case 0:
						// // middle freq
						// // System.out.println("middle freq= " + group);
						// break;
						//
						// default:
						// break;
						// }
					}
					break;

				default:
					break;
				}
			}
			// read rows and columns
			try (final Scanner sc = new Scanner(br)) {
				sc.useDelimiter(" ?\\| ?");
				fNoteSpacing = 0;
				fPatternSpacing = 0;
				fLowRes = false;
				fFetchedRow = 0;
				fPatternNum = 1;
				fNoteNum = 1;

				int lastFrame = 0;
				loop: do {
					final SidDumpOutput output = new SidDumpOutput();
					int col = 0;
					while (sc.hasNext()) {
						final String next = sc.next();
						if (next.trim().length() == 0) {
							// line break
							break;
						}
						switch (col) {
						case 0:
							if (next.startsWith("-") || next.startsWith("=")) {
								output.setTime(next);
								break;
							}
							// get frame of current row
							lastFrame = Integer.parseInt(next.trim());
							if (fFetchedRow == 1) {
								// detect low-res recording
								final int lowresdist = lastFrame;
								if (lowresdist != 1) {
									fLowRes = true;
									fNoteSpacing = lowresdist;
								}
							}
							// e.g. Frame=" 0"
							output.setTime(next);
							break;

						case 1:
						case 2:
						case 3:
							output.setFreq(next.substring(0, 4), col - 1);
							output.setNote(next.substring(5, 13), col - 1);
							output.setWf(next.substring(14, 16), col - 1);
							output.setAdsr(next.substring(17, 21), col - 1);
							output.setPul(next.substring(22, 25), col - 1);
							break;

						case 4:
							// e.g. FCut RC Typ V = "3000 F2 Low F"
							output.setFcut(next.substring(0, 4));
							output.setRc(next.substring(5, 7));
							output.setTyp(next.substring(8, 11));
							output.setV(next.substring(12, 13));
							break;

						case 5:
							add(output);
							if (next.trim().startsWith("+=")) {
								if (fPatternSpacing == 0) {
									final int nextFrame = lastFrame
											+ fNoteSpacing;
									fPatternSpacing = nextFrame / fNoteSpacing;
								}
								// pattern spacing?
								addPatternSpacing();
							} else if (next.trim().startsWith("+-")) {
								if (fNoteSpacing == 0) {
									if (!fLowRes) {
										fNoteSpacing = fFetchedRow;
									}
								}
								// note spacing?
								addNoteSpacing();
							}
							continue loop;

						default:
							break;
						}
						col++;
					}
					add(output);
				} while (sc.hasNext());
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		stopRecording();
	}

	private int readNumber(String number, int radix) {
		try {
			return Integer.parseInt(number, radix);
		} catch (final NumberFormatException e) {
			System.err.println(e.getMessage());
			return 0;
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

	/**
	 * Save table into file (create SID dump file)
	 * 
	 * @param filename
	 *            file name
	 * @param sidDumpOutputs
	 */
	public void save(final String filename,
			ObservableList<SidDumpOutput> sidDumpOutputs) {
		try (final PrintStream out = new PrintStream(new BufferedOutputStream(
				new FileOutputStream(filename)))) {
			out.println(String
					.format("Load address: $%04X Init address: $%04X Play address: $%04X",
							fLoadAddress, fInitAddress, fPlayerAddress));
			out.println("Calling initroutine with subtune "
					+ (fCurrentSong - 1));
			out.println("Calling playroutine for " + fSeconds * TUNE_SPEED
					+ " frames, starting from frame " + fFirstframe);
			out.println(String.format("Middle C frequency is $%04X",
					FREQ_TBL_LO_USE[48] | FREQ_TBL_HI_USE[48] << 8));
			out.println();
			out.println("| Frame | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | FCut RC Typ V |");
			out.println("+-------+---------------------------+---------------------------+---------------------------+---------------+");
			for (final SidDumpOutput putput : sidDumpOutputs) {
				// String firstCol = row.get(0);
				// if (firstCol.startsWith("=")) {
				// out
				// .println("+=======+===========================+===========================+===========================+===============+");
				// continue;
				// } else if (firstCol.startsWith("-")) {
				// out
				// .println("+-------+---------------------------+---------------------------+---------------------------+---------------+");
				// continue;
				// }
				out.print("| ");
				out.print(putput.getTime());
				out.print(" | ");
				out.print(putput.getFreq(0));
				out.print(" ");
				out.print(putput.getNote(0));
				out.print(" ");
				out.print(putput.getWf(0));
				out.print(" ");
				out.print(putput.getAdsr(0));
				out.print(" ");
				out.print(putput.getPul(0));
				out.print(" | ");
				out.print(putput.getFreq(1));
				out.print(" ");
				out.print(putput.getNote(1));
				out.print(" ");
				out.print(putput.getWf(1));
				out.print(" ");
				out.print(putput.getAdsr(1));
				out.print(" ");
				out.print(putput.getPul(1));
				out.print(" | ");
				out.print(putput.getFreq(2));
				out.print(" ");
				out.print(putput.getNote(2));
				out.print(" ");
				out.print(putput.getWf(2));
				out.print(" ");
				out.print(putput.getAdsr(2));
				out.print(" ");
				out.print(putput.getPul(2));
				out.print(" | ");
				out.print(putput.getFcut());
				out.print(" ");
				out.print(putput.getRc());
				out.print(" ");
				out.print(putput.getTyp());
				out.print(" ");
				out.print(putput.getV());
				out.println(" |");
			}
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Stop recording
	 */
	public void stopRecording() {
		// set total recorded frames
		// update table
		fFrames = fFirstframe + fSeconds * TUNE_SPEED;
	}

	/**
	 * Replay a recorded SID dump
	 * 
	 * @param sidDumpOutputs
	 * 
	 * @throws InvalidCommandException
	 */
	public void replay(ObservableList<SidDumpOutput> sidDumpOutputs)
			throws InvalidCommandException {
		replayer = new SidDumpReplayer(player.getConfig());
		replayer.setLeftVolume(leftVolume);
		replayer.setRegOrder(fRegOrder);
		replayer.setReplayFrequency(fReplayFreq);
		replayer.replay(sidDumpOutputs);
	}

	public void stopReplay() {
		replayer.stopReplay();
	}

	public abstract void clear();

	public abstract void add(final SidDumpOutput output);

}
