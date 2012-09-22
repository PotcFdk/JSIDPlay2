package applet.siddump;

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
import java.util.Iterator;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.regex.MatchResult;

import javax.swing.table.DefaultTableModel;

import org.swixml.Localizer;

import libsidplay.Player;
import libsidplay.common.IReSIDExtension;
import libsidplay.common.SIDEmu;
import libsidplay.components.mos6510.IMOS6510Extension;
import netsiddev.AudioGeneratorThread;
import netsiddev.InvalidCommandException;
import netsiddev.SIDWrite;
import resid_builder.resid.SID;
import sidplay.audio.AudioConfig;

public class SIDDumpModel extends DefaultTableModel implements
		IMOS6510Extension, IReSIDExtension {
	private static final int UPDATE_ON_ROW = 50;

	private Localizer localizer;

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

	private static final String COLUMN_NAMES[] = new String[] { "TIME",
			// channel 0
			"FREQ", "NOTE", "WF", "ADSR", "PUL",
			// channel 1
			"FREQ", "NOTE", "WF", "ADSR", "PUL",
			// channel 2
			"FREQ", "NOTE", "WF", "ADSR", "PUL",
			// filter
			"FCUT", "RC", "TYP",
			// volume
			"V" };

	private static final String NOTENAME[] = { "C-0", "C#0", "D-0", "D#0",
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

	private static final String FILTERNAME[] = { "Off", "Low", "Bnd", "L+B",
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

	private final Channel fChannel[] = new Channel[3];
	private final Channel fPrevChannel[] = new Channel[3];
	private final Channel fPrevChannel2[] = new Channel[3];
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
	 * Frequency of player address calls in Hz
	 */
	private static final int fTuneSpeed = 50;

	/**
	 * Replay option: register write order
	 */
	private Byte fRegOrder[] = null;

	/**
	 * Replay option: First row to replay
	 */
	private int fFrom;

	/**
	 * Replay option: Last row to replay
	 */
	private int fTo;

	/**
	 * Replay option: Frequency of player address calls
	 */
	private int fReplayFreq = 50;

	/**
	 * Recorded row number
	 */
	int fFetchedRow;

	/**
	 * Current pattern number
	 */
	int fPatternNum;

	/**
	 * Current note pattern number
	 */
	int fNoteNum;

	@Override
	public String getColumnName(final int column) {
		return localizer != null ? localizer.getString(COLUMN_NAMES[column])
				: "";
	}

	@Override
	public int getColumnCount() {
		return COLUMN_NAMES.length;
	}

	@Override
	public int getRowCount() {
		return dataVector.size();
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

	public void setAborted() {
		fAborted = true;
	}

	public void setReplayRange(final int from, final int to) {
		fFrom = from;
		fTo = to;
	}

	public void setReplayFrequency(final int freq) {
		fReplayFreq = freq;
	}

	/**
	 * Initialization routine to prepare recording SID write sequence.
	 */
	public void init() {
		clearTable();
		clearChannelStructures();
		recalibrateFreqTable();
		fFirstTime = true;
		fFetchedRow = 0;
		fPatternNum = 1;
		fNoteNum = 1;
	}

	/**
	 * Clear recorded rows
	 */
	private void clearTable() {
		final int rowCount = getRowCount();
		dataVector.removeAllElements();
		fireTableRowsDeleted(0, rowCount);
	}

	// boolean changes[]= new boolean[0x20];
	public void write(final long time, final int chipNum, final int addr,
			final byte data) {
		// changes[addr] = true;
	}

	/**
	 * CPU jmp/jsr instruction callback to catch calls to the player address
	 * 
	 * @param cpuTime
	 *            time in CPU
	 * @param register_ProgramCounter
	 *            current program counter
	 */
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

		if (fFrames < fFirstframe + fSeconds * fTuneSpeed) {
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
				final Vector<String> output = new Vector<String>();
				final long time = fFrames - fFirstframe;

				if (!fTimeInSeconds) {
					output.add(String.format("%5d", time));
				} else {
					output.add(String.format("%01d:%02d.%02d", time
							/ (fTuneSpeed * 60), time / fTuneSpeed % 60, time
							% fTuneSpeed));
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
						output.add(String.format("%04X", fChannel[c].freq));

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
									output.add(String.format(" %s %02X ",
											NOTENAME[fChannel[c].note],
											fChannel[c].note | 0x80));
								} else {
									output.add(String.format("(%s %02X)",
											NOTENAME[fChannel[c].note],
											fChannel[c].note | 0x80));
								}
							} else {
								// If same note, print frequency change
								// (slide/vibrato)
								if (delta != 0) {
									if (delta > 0) {
										output.add(String.format("(+ %04X)",
												delta));
									} else {
										output.add(String.format("(- %04X)",
												-delta));
									}
								} else {
									output.add(" ... .. ");
								}
							}
						} else {
							output.add(" ... .. ");
						}
					} else {
						output.add("....");
						output.add(" ... .. ");
					}

					// Waveform
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].wave != fPrevChannel[c].wave) {
						output.add(String.format("%02X", fChannel[c].wave));
					} else {
						output.add("..");
					}

					// ADSR
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].adsr != fPrevChannel[c].adsr) {
						output.add(String.format("%04X", fChannel[c].adsr));
					} else {
						output.add("....");
					}

					// Pulse
					if (fFrames == fFirstframe || newnote != 0
							|| fChannel[c].pulse != fPrevChannel[c].pulse) {
						output.add(String.format("%03X", fChannel[c].pulse));
					} else {
						output.add("...");
					}
				}
				// Filter cutoff
				if (fFrames == fFirstframe
						|| fFilter.cutoff != fPrevFilter.cutoff) {
					output.add(String.format("%04X", fFilter.cutoff));
				} else {
					output.add("....");
				}

				// Filter control
				if (fFrames == fFirstframe || fFilter.ctrl != fPrevFilter.ctrl) {
					output.add(String.format("%02X", fFilter.ctrl));
				} else {
					output.add("..");
				}

				// Filter passband
				if (fFrames == fFirstframe
						|| (fFilter.type & 0x70) != (fPrevFilter.type & 0x70)) {
					output.add(String.format("%s",
							FILTERNAME[fFilter.type >> 4 & 0x7]));
				} else {
					output.add("...");
				}

				// Mastervolume
				if (fFrames == fFirstframe
						|| (fFilter.type & 0xf) != (fPrevFilter.type & 0xf)) {
					output.add(String.format("%01X", fFilter.type & 0xf));
				} else {
					output.add(".");
				}

				// End of frame display, print info so far and copy SID
				// registers to
				// old registers
				if (!fLowRes || 0 == (fFrames - fFirstframe) % fNoteSpacing) {
					putInRow(output);
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
	 * Put a note spacing row into the table
	 */
	private void addNoteSpacing() {
		final Vector<String> noteSep = new Vector<String>();
		// Time
		noteSep.add(String.format("-N%03X", fNoteNum++));

		// Freq
		noteSep.add("----");
		// Note
		noteSep.add("--------");
		// WF
		noteSep.add("--");
		// ADSR
		noteSep.add("----");
		// Pul
		noteSep.add("---");

		// Freq
		noteSep.add("----");
		// Note
		noteSep.add("--------");
		// WF
		noteSep.add("--");
		// ADSR
		noteSep.add("----");
		// Pul
		noteSep.add("---");

		// Freq
		noteSep.add("----");
		// Note
		noteSep.add("--------");
		// WF
		noteSep.add("--");
		// ADSR
		noteSep.add("----");
		// Pul
		noteSep.add("---");

		// FCut
		noteSep.add("----");
		// RC
		noteSep.add("--");
		// Typ
		noteSep.add("---");
		// Volume
		noteSep.add("-");
		putInRow(noteSep);
	}

	/**
	 * Put a pattern spacing row into the table
	 */
	private void addPatternSpacing() {
		fNoteNum = 1;
		final Vector<String> patternSep = new Vector<String>();
		// Time
		patternSep.add(String.format("=P%03X", fPatternNum++));

		// Freq
		patternSep.add("====");
		// Note
		patternSep.add("========");
		// WF
		patternSep.add("==");
		// ADSR
		patternSep.add("====");
		// Pul
		patternSep.add("===");

		// Freq
		patternSep.add("====");
		// Note
		patternSep.add("========");
		// WF
		patternSep.add("==");
		// ADSR
		patternSep.add("====");
		// Pul
		patternSep.add("===");

		// Freq
		patternSep.add("====");
		// Note
		patternSep.add("========");
		// WF
		patternSep.add("==");
		// ADSR
		patternSep.add("====");
		// Pul
		patternSep.add("===");

		// FCut
		patternSep.add("====");
		// RC
		patternSep.add("==");
		// Typ
		patternSep.add("===");
		// Volume
		patternSep.add("=");
		putInRow(patternSep);
	}

	/**
	 * Put the row into the table
	 * 
	 * @param output
	 *            the row to add
	 */
	@SuppressWarnings("unchecked")
	private void putInRow(final Vector<String> output) {
		dataVector.insertElementAt(output, fFetchedRow++);
		if (fFetchedRow % UPDATE_ON_ROW == 0) {
			// do not update every time a row is added (performance)
			fireTableRowsInserted(fFetchedRow - fFetchedRow % UPDATE_ON_ROW,
					fFetchedRow);
		}
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
	 * Stop recording
	 */
	public void stop() {
		// set total recorded frames
		// update table
		fFrames = fFirstframe + fSeconds * fTuneSpeed;
		fireTableRowsInserted(dataVector.size() - fFetchedRow % UPDATE_ON_ROW,
				dataVector.size());
	}

	/**
	 * Save table into file (create SID dump file)
	 * 
	 * @param name
	 *            file name
	 */
	@SuppressWarnings("unchecked")
	public void save(final String name) {
		try {
			final PrintStream out = new PrintStream(new BufferedOutputStream(
					new FileOutputStream(name)));

			out.println(String
					.format("Load address: $%04X Init address: $%04X Play address: $%04X",
							fLoadAddress, fInitAddress, fPlayerAddress));
			out.println("Calling initroutine with subtune "
					+ (fCurrentSong - 1));
			out.println("Calling playroutine for " + fSeconds * fTuneSpeed
					+ " frames, starting from frame " + fFirstframe);
			out.println(String.format("Middle C frequency is $%04X",
					FREQ_TBL_LO_USE[48] | FREQ_TBL_HI_USE[48] << 8));
			out.println();
			out.println("| Frame | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | FCut RC Typ V |");
			out.println("+-------+---------------------------+---------------------------+---------------------------+---------------+");
			for (final Vector<String> aDataVector : (Iterable<Vector<String>>) dataVector) {
				int colNum = 0;
				final Vector<String> row = aDataVector;

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
				for (final Iterator<String> cols = row.iterator(); cols
						.hasNext();) {
					final String col = cols.next().toString();
					out.print(col);
					out.print(" ");
					if (colNum == 0 || colNum % 5 == 0) {
						out.print("|");
						if (cols.hasNext()) {
							out.print(" ");
						}
					}
					colNum++;
				}
				out.println("|");
			}
			out.close();
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Re-load a SID dump file
	 * 
	 * @param name
	 *            file name to load
	 */
	public void load(final String name) {
		// first clear table
		clearTable();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(
					new FileInputStream(name)));
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
				final Vector<String> colVector = new Vector<String>();
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
							colVector.add(next);
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
						colVector.add(next);
						break;

					case 1:
					case 2:
					case 3:
						// e.g. Freq Note/Abs WF ADSR Pul = "0000 ... .. 09 00FD
						// 808"
						final String freq = next.substring(0, 4);
						colVector.add(freq);
						final String note = next.substring(5, 13);
						colVector.add(note);
						final String wf = next.substring(14, 16);
						colVector.add(wf);
						final String adsr = next.substring(17, 21);
						colVector.add(adsr);
						final String pul = next.substring(22, 25);
						colVector.add(pul);
						break;

					case 4:
						// e.g. FCut RC Typ V = "3000 F2 Low F"
						final String fcut = next.substring(0, 4);
						colVector.add(fcut);
						final String rc = next.substring(5, 7);
						colVector.add(rc);
						final String typ = next.substring(8, 11);
						colVector.add(typ);
						final String v = next.substring(12, 13);
						colVector.add(v);
						break;

					case 5:
						putInRow(colVector);
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
				putInRow(colVector);
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
		stop();
	}

	private Player player;

	private AudioConfig config;

	/**
	 * Replay a recorded SID dump
	 * 
	 * @throws InvalidCommandException
	 */
	public void replay() throws InvalidCommandException {
		if (fRegOrder == null) {
			// no SID write order, no playback!
			return;
		}

		fAborted = false;

		/* FIXME: configure this SID. It is a 8580. */
		SID sid = new SID();

		/*
		 * FIXME: support for HardSID playback of recordings is lost. Will fix
		 * later.
		 */
		AudioGeneratorThread agt = new AudioGeneratorThread(config);
		agt.setSidArray(new SID[] { sid });
		BlockingQueue<SIDWrite> queue = agt.getSidCommandQueue();
		agt.start();

		try {
			// reset replay queue
			byte volume = 0xf;
			int timeOffset = 0;

			// for each row do replay
			for (int rown = fFrom; rown <= fTo && rown < dataVector.size(); rown++) {
				@SuppressWarnings("unchecked")
				final Vector<String> row = (Vector<String>) dataVector
						.get(rown);

				final String firstCol = row.get(0);
				if (firstCol.startsWith("=")) {
					// ignore pattern spacing
					continue;
				} else if (firstCol.startsWith("-")) {
					// ignore note spacing
					continue;
				}

				/*
				 * This impossibly screwy scheme is to support finding an
				 * initial value for the first invocation.
				 */
				final Vector<Vector<String>> examineRows = new Vector<Vector<String>>();
				for (int i = 0; i < row.size(); i++) {
					examineRows.add(row);
				}

				/*
				 * Now find the last value for every register & set those
				 * values.
				 */
				if (timeOffset == 0 && fFrom > 0) {
					for (int coli = 0; coli < row.size(); coli++) {
						// for each column find a row with a value != ...
						final String col = row.get(coli);
						if (coli == 0 || col.startsWith(".")
								|| col.startsWith("-") || col.startsWith("=")) {
							// search backwards to find initial register value
							// of
							// column i (always search for the last frame
							// (col=0))
							for (int rowi = rown - 1; rowi >= 0; rowi--) {
								@SuppressWarnings("unchecked")
								final Vector<String> findRow = (Vector<String>) dataVector
										.get(rowi);
								if (findRow.get(coli).startsWith(".")
										|| findRow.get(coli).startsWith("-")
										|| findRow.get(coli).startsWith("=")) {
									// still not found in previous row?
									continue;
								}
								// findRow contains the initial register value
								examineRows.set(coli, findRow);
								break;
							}
						}
					}
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
						col = examineRows.get(coln).get(coln);
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
						col = examineRows.get(coln).get(coln);
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
						col = examineRows.get(coln).get(coln);
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
						col = examineRows.get(coln).get(coln);
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
						col = examineRows.get(coln).get(coln);
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
						col = examineRows.get(coln).get(coln);
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
						String colFilt = examineRows.get(coln).get(coln);
						coln = 19;
						String colMast = examineRows.get(coln).get(coln);

						if (colFilt.startsWith(".") && colMast.startsWith(".")) {
							break;
						}

						if (!colMast.startsWith(".")) {
							volume = (byte) (volume & 0xf0 | Integer.parseInt(
									colMast, 16));
						}
						if (!colFilt.startsWith(".")) {
							String cmp = colFilt.trim();
							for (int j = 0; j < FILTERNAME.length; j++) {
								if (FILTERNAME[j].equals(cmp)) {
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
			agt.ensureDraining();
			agt.join();
		} catch (InterruptedException e) {
		} finally {
			agt.interrupt();
		}
	}

	public void setPlayer(final Player player, final AudioConfig config) {
		this.player = player;
		this.config = config;
	}

	public void setLocalizer(Localizer l) {
		localizer = l;
		fireTableStructureChanged();
	}
}
