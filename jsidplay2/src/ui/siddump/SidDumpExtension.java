package ui.siddump;

import static libsidutils.SIDDump.ATTACK_DECAY_1;
import static libsidutils.SIDDump.ATTACK_DECAY_2;
import static libsidutils.SIDDump.ATTACK_DECAY_3;
import static libsidutils.SIDDump.FILTERCTRL;
import static libsidutils.SIDDump.FILTERFREQ_HI;
import static libsidutils.SIDDump.FILTERFREQ_LO;
import static libsidutils.SIDDump.FREQ_HI_1;
import static libsidutils.SIDDump.FREQ_HI_2;
import static libsidutils.SIDDump.FREQ_HI_3;
import static libsidutils.SIDDump.FREQ_LO_1;
import static libsidutils.SIDDump.FREQ_LO_2;
import static libsidutils.SIDDump.FREQ_LO_3;
import static libsidutils.SIDDump.PULSE_HI_1;
import static libsidutils.SIDDump.PULSE_HI_2;
import static libsidutils.SIDDump.PULSE_HI_3;
import static libsidutils.SIDDump.PULSE_LO_1;
import static libsidutils.SIDDump.PULSE_LO_2;
import static libsidutils.SIDDump.PULSE_LO_3;
import static libsidutils.SIDDump.SUSTAIN_RELEASE_1;
import static libsidutils.SIDDump.SUSTAIN_RELEASE_2;
import static libsidutils.SIDDump.SUSTAIN_RELEASE_3;
import static libsidutils.SIDDump.VOL;
import static libsidutils.SIDDump.WAVEFORM_1;
import static libsidutils.SIDDump.WAVEFORM_2;
import static libsidutils.SIDDump.WAVEFORM_3;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.regex.MatchResult;

import javafx.collections.ObservableList;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import libsidplay.components.mos6510.IMOS6510Extension;
import netsiddev.AudioGeneratorThread;
import netsiddev.InvalidCommandException;
import netsiddev.SIDWrite;
import resid_builder.resid.SID;
import sidplay.audio.AudioConfig;
import ui.entities.config.Configuration;

public abstract class SidDumpExtension implements IMOS6510Extension {

	protected static class Channel {
		int freq;
		int pulse;
		int adsr;
		int wave;
		int note;
	}

	protected static class Filter {
		int cutoff;
		int ctrl;
		int type;
	}

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

	private static final int SID_WRITE_DELAY = 6;

	/**
	 * Frequency of player address calls in Hz
	 */
	private static final int TUNE_SPEED = 50;

	private final Channel fChannel[] = new Channel[3];

	private final Channel fPrevChannel[] = new Channel[3];

	private final Channel fPrevChannel2[] = new Channel[3];

	private Player player;
	private AudioConfig config;
	private AudioGeneratorThread agt;

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

	private boolean fAborted;

	/**
	 * Replay option: register write order
	 */
	private Byte fRegOrder[] = null;

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

	public SidDumpExtension(Player pl, Configuration cfg) {
		this.player = pl;
		this.config = AudioConfig.getInstance(cfg.getAudio(), 2);
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

	public void setRegOrder(final Byte[] bytes) {
		this.fRegOrder = bytes;
	}

	public void setReplayFrequency(final int freq) {
		fReplayFreq = freq;
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
	 * @param register_ProgramCounter
	 *            current program counter
	 */
	@Override
	public void fetch(final long cpuTime) {
		// ignore first call
		if (fFirstTime) {
			fFirstTime = false;
			return;
		}

		// player is called within time window
		final SIDEmu fSid = player.getC64().getSID(0);
		if (fSid == null) {
			return;
		}

		if (fFrames < fFirstframe + fSeconds * TUNE_SPEED) {
			// Get SID parameters from each channel and the filter
			for (int c = 0; c < 3; c++) {
				fChannel[c].freq = fSid.readInternalRegister(FREQ_LO_1 + 7 * c)
						& 0xff
						| (fSid.readInternalRegister(FREQ_HI_1 + 7 * c) & 0xff) << 8;
				fChannel[c].pulse = (fSid.readInternalRegister(PULSE_LO_1 + 7
						* c) & 0xff | (fSid.readInternalRegister(PULSE_HI_1 + 7
						* c) & 0xff) << 8) & 0xfff;
				fChannel[c].wave = fSid
						.readInternalRegister(WAVEFORM_1 + 7 * c) & 0xff;
				fChannel[c].adsr = fSid.readInternalRegister(SUSTAIN_RELEASE_1
						+ 7 * c)
						& 0xff
						| (fSid.readInternalRegister(ATTACK_DECAY_1 + 7 * c) & 0xff) << 8;
			}
			fFilter.cutoff = (fSid.readInternalRegister(FILTERFREQ_LO) & 0xff) << 5
					| (fSid.readInternalRegister(FILTERFREQ_HI) & 0xff) << 8;
			fFilter.ctrl = fSid.readInternalRegister(FILTERCTRL) & 0xff;
			fFilter.type = fSid.readInternalRegister(VOL) & 0xff;

			// Frame display if first frame to be recorded is reached
			if (fFrames >= fFirstframe) {
				SidDumpOutput output = new SidDumpOutput();
				final long time = fFrames - fFirstframe;

				if (!fTimeInSeconds) {
					output.setTime(String.format("%5d", time));
				} else {
					output.setTime(String.format("%01d:%02d.%02d", time
							/ (TUNE_SPEED * 60), time / TUNE_SPEED % 60, time
							% TUNE_SPEED));
				}

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
						output.setFreq(String.format("%04X", fChannel[c].freq),
								c);

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
									output.setNote(String.format(" %s %02X ",
											NOTE_NAME[fChannel[c].note],
											fChannel[c].note | 0x80), c);
								} else {
									output.setNote(String.format("(%s %02X)",
											NOTE_NAME[fChannel[c].note],
											fChannel[c].note | 0x80), c);
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
						output.setWf(String.format("%02X", fChannel[c].wave), c);
					} else {
						output.setWf("..", c);
					}

					// ADSR
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].adsr != fPrevChannel[c].adsr) {
						output.setAdsr(String.format("%04X", fChannel[c].adsr),
								c);
					} else {
						output.setAdsr("....", c);
					}

					// Pulse
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].pulse != fPrevChannel[c].pulse) {
						output.setPul(String.format("%03X", fChannel[c].pulse),
								c);
					} else {
						output.setPul("...", c);
					}
				}
				// Filter cutoff
				if (fFrames == fFirstframe
						|| fFilter.cutoff != fPrevFilter.cutoff) {
					output.setFcut(String.format("%04X", fFilter.cutoff));
				} else {
					output.setFcut("....");
				}

				// Filter control
				if (fFrames == fFirstframe || fFilter.ctrl != fPrevFilter.ctrl) {
					output.setRc(String.format("%02X", fFilter.ctrl));
				} else {
					output.setRc("..");
				}

				// Filter passband
				if (fFrames == fFirstframe
						|| (fFilter.type & 0x70) != (fPrevFilter.type & 0x70)) {
					output.setTyp(FILTER_NAME[fFilter.type >> 4 & 0x7]);
				} else {
					output.setTyp("...");
				}

				// Mastervolume
				if (fFrames == fFirstframe
						|| (fFilter.type & 0xf) != (fPrevFilter.type & 0xf)) {
					output.setV(String.format("%01X", fFilter.type & 0xf));
				} else {
					output.setV(".");
				}

				// End of frame display, print info so far and copy SID
				// registers to
				// old registers
				if (!fLowRes || 0 == (fFrames - fFirstframe) % fNoteSpacing) {
					fFetchedRow++;
					add(output);
					for (int c = 0; c < 3; c++) {
						fPrevChannel[c].adsr = fChannel[c].adsr;
						fPrevChannel[c].freq = fChannel[c].freq;
						fPrevChannel[c].note = fChannel[c].note;
						fPrevChannel[c].pulse = fChannel[c].pulse;
						fPrevChannel[c].wave = fChannel[c].wave;
					}
					fPrevFilter.ctrl = fFilter.ctrl;
					fPrevFilter.cutoff = fFilter.cutoff;
					fPrevFilter.type = fFilter.type;
				}
				for (int c = 0; c < 3; c++) {
					fPrevChannel2[c].adsr = fChannel[c].adsr;
					fPrevChannel2[c].freq = fChannel[c].freq;
					fPrevChannel2[c].note = fChannel[c].note;
					fPrevChannel2[c].pulse = fChannel[c].pulse;
					fPrevChannel2[c].wave = fChannel[c].wave;
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
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(
					filename)));
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
							// Load Address
							try {
								fLoadAddress = Integer.parseInt(group, 16);
							} catch (final NumberFormatException e) {
								e.printStackTrace();
							}
							break;

						case 1:
							// Init Address
							try {
								fInitAddress = Integer.parseInt(group, 16);
							} catch (final NumberFormatException e) {
								e.printStackTrace();
							}
							break;

						case 2:
							// Player Address
							try {
								fPlayerAddress = Integer.parseInt(group, 16);
							} catch (final NumberFormatException e) {
								e.printStackTrace();
							}
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
							try {
								fCurrentSong = Integer.parseInt(group) + 1;
							} catch (final NumberFormatException e) {
								e.printStackTrace();
							}
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
							try {
								fFirstframe = Integer.parseInt(group);
							} catch (final NumberFormatException e) {
								e.printStackTrace();
							}
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
			final Scanner sc = new Scanner(br);
			sc.useDelimiter(" ?\\| ?");
			fNoteSpacing = 0;
			fPatternSpacing = 0;
			fLowRes = false;
			fFetchedRow = 0;
			fPatternNum = 1;
			fNoteNum = 1;

			int lastFrame = 0;
			loop: do {
				final SidDumpOutput sidDumpOutput = new SidDumpOutput();
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
							sidDumpOutput.setTime(next);
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
						sidDumpOutput.setTime(next);
						break;

					case 1:
					case 2:
					case 3:
						// e.g. Freq Note/Abs WF ADSR Pul = "0000 ... .. 09 00FD
						// 808"
						final String freq = next.substring(0, 4);
						sidDumpOutput.setFreq(freq, col - 1);
						final String note = next.substring(5, 13);
						sidDumpOutput.setNote(note, col - 1);
						final String wf = next.substring(14, 16);
						sidDumpOutput.setWf(wf, col - 1);
						final String adsr = next.substring(17, 21);
						sidDumpOutput.setAdsr(adsr, col - 1);
						final String pul = next.substring(22, 25);
						sidDumpOutput.setPul(pul, col - 1);
						break;

					case 4:
						// e.g. FCut RC Typ V = "3000 F2 Low F"
						final String fcut = next.substring(0, 4);
						sidDumpOutput.setFcut(fcut);
						final String rc = next.substring(5, 7);
						sidDumpOutput.setRc(rc);
						final String typ = next.substring(8, 11);
						sidDumpOutput.setTyp(typ);
						final String v = next.substring(12, 13);
						sidDumpOutput.setV(v);
						break;

					case 5:
						add(sidDumpOutput);
						if (next.trim().startsWith("+=")) {
							if (fPatternSpacing == 0) {
								final int nextFrame = lastFrame + fNoteSpacing;
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
				add(sidDumpOutput);
			} while (sc.hasNext());
			sc.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		stopRecording();
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
		try {
			final PrintStream out = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(filename)));

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
			for (final SidDumpOutput sidDumpOutput : sidDumpOutputs) {
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
				out.print(sidDumpOutput.getTime());
				out.print(" | ");
				out.print(sidDumpOutput.getFreq(0));
				out.print(" ");
				out.print(sidDumpOutput.getNote(0));
				out.print(" ");
				out.print(sidDumpOutput.getWf(0));
				out.print(" ");
				out.print(sidDumpOutput.getAdsr(0));
				out.print(" ");
				out.print(sidDumpOutput.getPul(0));
				out.print(" | ");
				out.print(sidDumpOutput.getFreq(1));
				out.print(" ");
				out.print(sidDumpOutput.getNote(1));
				out.print(" ");
				out.print(sidDumpOutput.getWf(1));
				out.print(" ");
				out.print(sidDumpOutput.getAdsr(1));
				out.print(" ");
				out.print(sidDumpOutput.getPul(1));
				out.print(" | ");
				out.print(sidDumpOutput.getFreq(2));
				out.print(" ");
				out.print(sidDumpOutput.getNote(2));
				out.print(" ");
				out.print(sidDumpOutput.getWf(2));
				out.print(" ");
				out.print(sidDumpOutput.getAdsr(2));
				out.print(" ");
				out.print(sidDumpOutput.getPul(2));
				out.print(" | ");
				out.print(sidDumpOutput.getFcut());
				out.print(" ");
				out.print(sidDumpOutput.getRc());
				out.print(" ");
				out.print(sidDumpOutput.getTyp());
				out.print(" ");
				out.print(sidDumpOutput.getV());
				out.println(" |");
			}
			out.close();
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
		agt = new AudioGeneratorThread(config);
		agt.setSidArray(new SID[] { sid });
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
				for (final Byte aFRegOrder : fRegOrder) {
					String col;
					int coln;
					byte register = aFRegOrder;

					switch (aFRegOrder) {
					case ATTACK_DECAY_1:
					case SUSTAIN_RELEASE_1:
					case ATTACK_DECAY_2:
					case SUSTAIN_RELEASE_2:
					case ATTACK_DECAY_3:
					case SUSTAIN_RELEASE_3:
						// ADSR
						coln = aFRegOrder / 7 * 5 + 4;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}

						final int adsr = Integer.valueOf(col, 16);
						if ((aFRegOrder + 2) % 7 == 0) {
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
						coln = aFRegOrder / 7 * 5 + 1;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}
						final int freq = Integer.valueOf(col, 16);
						if (aFRegOrder % 7 == 0) {
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
						coln = aFRegOrder / 7 * 5 + 5;
						col = getColumnValue(examineRows.get(coln), coln);
						if (col.startsWith(".")) {
							// ignore columns without a change
							break;
						}

						final int pulse = Integer.valueOf(col.trim(), 16);
						if ((aFRegOrder + 5) % 7 == 0) {
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
						coln = aFRegOrder / 7 * 5 + 3;
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

	public abstract void clear();

	public abstract void add(final SidDumpOutput output);

}
