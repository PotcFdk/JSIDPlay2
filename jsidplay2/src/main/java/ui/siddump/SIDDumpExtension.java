package ui.siddump;

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
import libsidplay.common.CPUClock;
import libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg;
import server.netsiddev.InvalidCommandException;
import sidplay.Player;
import sidplay.audio.siddump.SidDumpOutput;
import ui.entities.config.Configuration;

public abstract class SIDDumpExtension extends sidplay.audio.siddump.SIDDumpExtension {

	private Player player;

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
	private double fSeconds;

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

	public SIDDumpExtension(Player pl, Configuration cfg) {
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

	public void setRecordLength(final double seconds) {
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
	@Override
	public void init(CPUClock cpuClock) {
		super.init(cpuClock);
		clear();
	}

	@Override
	public boolean isWithinTimeWindow() {
		return fFrames >= fFirstframe && fFrames <= (long) (fFirstframe + fSeconds * cpuClock.getScreenRefresh());
	}

	/**
	 * Re-load a SID dump file
	 *
	 * @param filename file name to load
	 */
	public void load(final String filename) {
		// first clear table
		clear();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filename)))) {
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
					sc.findInLine(
							"Load address: \\$(\\p{XDigit}+) Init address: \\$(\\p{XDigit}+) Play address: \\$(\\p{XDigit}+)");
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
	 * @param filename       file name
	 * @param sidDumpOutputs
	 */
	public void save(final String filename, ObservableList<SidDumpOutput> sidDumpOutputs) {
		try (final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
			out.println(String.format("Load address: $%04X Init address: $%04X Play address: $%04X", fLoadAddress,
					fInitAddress, fPlayerAddress));
			out.println("Calling initroutine with subtune " + (fCurrentSong - 1));
			out.println("Calling playroutine for " + (int) (fSeconds * cpuClock.getScreenRefresh())
					+ " frames, starting from frame " + fFirstframe);
			out.println(String.format("Middle C frequency is $%04X", FREQ_TBL_LO_USE[48] | FREQ_TBL_HI_USE[48] << 8));
			out.println();
			out.println(
					"| Frame | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | FCut RC Typ V |");
			out.println(
					"+-------+---------------------------+---------------------------+---------------------------+---------------+");
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
		fFrames = (long) (fFirstframe + fSeconds * cpuClock.getScreenRefresh());
	}

	/**
	 * Replay a recorded SID dump
	 *
	 * @param sidDumpOutputs
	 *
	 * @throws InvalidCommandException
	 */
	public void replay(ObservableList<SidDumpOutput> sidDumpOutputs) throws InvalidCommandException {
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

	@Override
	public abstract void add(final SidDumpOutput output);

}
