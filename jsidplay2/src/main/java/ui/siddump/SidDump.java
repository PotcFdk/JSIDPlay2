package ui.siddump;

import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.MatchResult;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import libsidplay.sidtune.SidTune;
import libsidutils.siddump.SIDDumpConfiguration;
import libsidutils.siddump.SIDDumpConfiguration.SIDDumpPlayer;
import libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg;
import server.netsiddev.InvalidCommandException;
import sidplay.Player;
import sidplay.audio.siddump.SIDDumpExtension;
import sidplay.audio.siddump.SidDumpOutput;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.converter.TimeToStringConverter;

public class SidDump extends C64VBox implements UIPart {

	public static final String ID = "SIDDUMP";

	@FXML
	private Button loadDump, saveDump, stop;
	@FXML
	protected ToggleButton replayAll, startStopRecording;
	@FXML
	private CheckBox startStopPlayer, timeInSeconds, lowResolutionMode;
	@FXML
	private TextField firstFrame, noteSpacing, maxRecordLength, patternSpacing, oldNoteFactor, tableFontSize, baseFreq,
			baseNote, callsPerFrame;
	@FXML
	private ComboBox<SIDDumpPlayer> regPlayer;
	@FXML
	private TableView<SidDumpOutput> dumpTable;

	protected ObservableList<SidDumpOutput> sidDumpOutputs;

	private ObservableList<SIDDumpPlayer> sidDumpPlayers;

	protected SIDDumpExtension sidDumpExtension;

	private int loadAddress, initAddress, playerAddress, subTune;

	private double seconds;

	/**
	 * Total record duration
	 */
	private double recordLength;

	private Thread fPlayerThread;

	private Collection<SIDDumpReg> fRegOrder = null;

	private int fReplayFreq = 50;

	private float leftVolume;

	private SidDumpReplayer replayer;

	private PropertyChangeListener changeListener;

	public SidDump() {
		super();
	}

	public SidDump(final C64Window window, final Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		changeListener = event -> {
			if (event.getNewValue() == State.START) {
				Platform.runLater(() -> setTune(util.getPlayer().getTune()));
			}
			if (event.getNewValue() == State.END) {
				Platform.runLater(() -> {
					startStopRecording.setSelected(false);
					replayAll.setDisable(false);
					stopRecording();
				});
			}
		};
		sidDumpExtension = new SIDDumpExtension() {

			@Override
			public void add(final SidDumpOutput output) {
				SidDump.this.add(output);
			}

			@Override
			public boolean isAborted() {
				return SidDump.this.isAborted();
			}

		};
		util.getPlayer().stateProperty().addListener(changeListener);

		maxRecordLength.textProperty().addListener((obj, o, n) -> util.checkTextField(maxRecordLength, () -> {
			seconds = new TimeToStringConverter().fromString(maxRecordLength.getText()).doubleValue();
			return seconds != -1;
		}, () -> recordLength = (seconds), "MAX_RECORD_LENGTH_TIP", "MAX_RECORD_LENGTH_FORMAT"));

		sidDumpOutputs = FXCollections.<SidDumpOutput>observableArrayList();
		SortedList<SidDumpOutput> sortedList = new SortedList<>(sidDumpOutputs);
		sortedList.comparatorProperty().bind(dumpTable.comparatorProperty());
		dumpTable.setItems(sortedList);
		sidDumpPlayers = FXCollections.<SIDDumpPlayer>observableArrayList();
		regPlayer.setItems(sidDumpPlayers);
		SIDDumpConfiguration sidDump;
		try {
			sidDump = new SIDDumpConfiguration();
			sidDumpPlayers.addAll(sidDump.getPlayers());
			regPlayer.getSelectionModel().select(0);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
		doSetTableFontSize();
		setTune(util.getPlayer().getTune());
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(changeListener);
		util.getPlayer().removeMOS6510Extension(sidDumpExtension);
		util.getPlayer().removeSidListener(sidDumpExtension);
	}

	@FXML
	private void doLoadDump() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		final File file = fileDialog.showOpenDialog(loadDump.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			load(file.getAbsolutePath());
			noteSpacing.setText(String.valueOf(sidDumpExtension.getNoteSpacing()));
			patternSpacing.setText(String.valueOf(sidDumpExtension.getPatternSpacing()));
			firstFrame.setText(String.valueOf(sidDumpExtension.getFirstFrame()));
			lowResolutionMode.setSelected(sidDumpExtension.getLowRes());
			timeInSeconds.setSelected(sidDumpExtension.getTimeInSeconds());
		}
	}

	@FXML
	private void doSaveDump() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		final File file = fileDialog.showSaveDialog(saveDump.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			save(file.getAbsolutePath(), sidDumpOutputs);
		}
	}

	@FXML
	private void doReplayAll() {
		try {
			while (fPlayerThread != null && fPlayerThread.isAlive()) {
				stopReplay();
				fPlayerThread.join(1000);
				// This is only the last option, if the player can not be
				// stopped clean
				fPlayerThread.interrupt();
			}
		} catch (InterruptedException e1) {
		}
		if (replayAll.isSelected()) {
			fPlayerThread = new Thread(() -> {
				try {
					replay(sidDumpOutputs);
				} catch (InvalidCommandException e) {
					e.printStackTrace();
				}
				replayAll.setSelected(false);
			});
			fPlayerThread.start();
		}
	}

	@FXML
	private void doStartStopRecording() {
		if (startStopRecording.isSelected()) {
			setTune(util.getPlayer().getTune());
			util.getPlayer().addSidListener(sidDumpExtension);
			util.getPlayer().addMOS6510Extension(sidDumpExtension);
			// restart tune, before recording starts
			util.setPlayingTab(this);
			if (startStopPlayer.isSelected()) {
				util.getPlayer().play(util.getPlayer().getTune());
			}
		} else {
			util.getPlayer().removeSidListener(sidDumpExtension);
			util.getPlayer().removeMOS6510Extension(sidDumpExtension);
			if (startStopPlayer.isSelected()) {
				util.getPlayer().stopC64();
			}
		}
	}

	@FXML
	private void doSetPlayer() {
		fRegOrder = (regPlayer.getSelectionModel().getSelectedItem().getRegs());
	}

	@FXML
	private void doSetNoteSpacing() {
		util.checkTextField(noteSpacing, () -> Integer.parseInt(noteSpacing.getText()) >= 0, () -> {
		}, "NOTE_SPACING_TIP", "NOTE_SPACING_NEG");
	}

	@FXML
	private void doSetPatternSpacing() {
		util.checkTextField(patternSpacing, () -> Integer.parseInt(patternSpacing.getText()) >= 0, () -> {
		}, "PATTERN_SPACING_TIP", "PATTERN_SPACING_NEG");
	}

	@FXML
	private void doSetOldNoteFactor() {
		util.checkTextField(oldNoteFactor, () -> Float.parseFloat(oldNoteFactor.getText()) >= 1, () -> {
		}, "OLD_NOTE_FACTOR_TIP", "OLD_NOTE_FACTOR_NEG");
	}

	@FXML
	private void doSetBaseFreq() {
		util.checkTextField(baseFreq, () -> Integer.decode(baseFreq.getText()) >= 0, () -> {
		}, "BASE_FREQ_TIP", "BASE_FREQ_HEX");
	}

	@FXML
	private void doSetTableFontSize() {
		util.checkTextField(tableFontSize, () -> {
			int fontSizeVal = Integer.parseInt(tableFontSize.getText());
			return fontSizeVal > 0 && fontSizeVal <= 24;
		}, () -> {
			int fontSizeVal = Integer.parseInt(tableFontSize.getText());
			dumpTable.setStyle(String.format("-fx-font-size:%d.0px;}", fontSizeVal));
			for (TableColumn<SidDumpOutput, ?> column : dumpTable.getColumns()) {
				column.setStyle(String.format("-fx-font-size:%d.0px;", fontSizeVal));

			}
		}, "TABLE_FONT_SIZE_TIP", "TABLE_FONT_SIZE_NEG");
	}

	@FXML
	private void doSetBaseNote() {
		util.checkTextField(baseNote, () -> {
			int baseNoteVal = Integer.decode(baseNote.getText());
			return baseNoteVal >= 128 && baseNoteVal <= 223;
		}, () -> {
		}, "BASE_NOTE_TIP", "BASE_NOTE_HEX");
	}

	@FXML
	private void doSetCallsPerFrame() {
		util.checkTextField(callsPerFrame, () -> {
			final int speed = Integer.parseInt(callsPerFrame.getText());
			return speed >= 1;
		}, () -> {
			final int speed = Integer.parseInt(callsPerFrame.getText());
			fReplayFreq = (speed * 50);
		}, "CALLS_PER_FRAME_TIP", "CALLS_PER_FRAME_NEG");
	}

	@FXML
	private void doSetFirstFrame() {
		util.checkTextField(firstFrame, () -> Long.parseLong(firstFrame.getText()) >= 0, () -> {
		}, "FIRST_FRAME_TIP", "FIRST_FRAME_NEG");
	}

	private void setTune(SidTune tune) {
		if (tune == null) {
			startStopRecording.setDisable(true);
			return;
		}
		startStopRecording.setDisable(tune.getInfo().getPlayAddr() == 0);

		loadAddress = tune.getInfo().getLoadAddr();
		initAddress = tune.getInfo().getInitAddr();
		playerAddress = tune.getInfo().getPlayAddr();
		subTune = tune.getInfo().getCurrentSong();
		sidDumpExtension.setFirstFrame(Long.valueOf(firstFrame.getText()));
		if (seconds == 0) {
			double length = util.getPlayer().getSidDatabaseInfo(db -> db.getSongLength(tune), 0.);
			if (length == 0) {
				length = util.getConfig().getSidplay2Section().getDefaultPlayLength();
				if (length <= 0) {
					// default
					length = 60;
				}
			}
			maxRecordLength.setText(new TimeToStringConverter().toString(length));
			recordLength = (length);
		} else {
			recordLength = (seconds);
		}
		sidDumpExtension.setTimeInSeconds(timeInSeconds.isSelected());
		sidDumpExtension.setOldNoteFactor(Float.parseFloat(oldNoteFactor.getText()));
		sidDumpExtension.setBaseFreq(Integer.decode(baseFreq.getText()));
		sidDumpExtension.setBaseNote(Integer.decode(baseNote.getText()));
		sidDumpExtension.setPatternSpacing(Integer.valueOf(patternSpacing.getText()));
		sidDumpExtension.setNoteSpacing(Integer.parseInt(noteSpacing.getText()));
		sidDumpExtension.setLowRes(lowResolutionMode.isSelected());

		if (tune.getInfo().getPlayAddr() == 0) {
			startStopRecording.setSelected(false);
			startStopRecording.setTooltip(new Tooltip(util.getBundle().getString("NOT_AVAILABLE")));
		} else {
			startStopRecording.setTooltip(new Tooltip(null));
		}
		leftVolume = (util.getConfig().getAudioSection().getMainVolume());
		fRegOrder = (regPlayer.getSelectionModel().getSelectedItem().getRegs());
		sidDumpExtension.init(util.getPlayer().getC64().getClock());
		Platform.runLater(() -> sidDumpOutputs.clear());

	}

	/**
	 * Stop recording
	 */
	public void stopRecording() {
		// set total recorded frames
		// update table
		sidDumpExtension.setFrames((long) (sidDumpExtension.getFirstFrame()
				+ seconds * util.getPlayer().getC64().getClock().getScreenRefresh()));
	}

	/**
	 * Re-load a SID dump file
	 *
	 * @param filename file name to load
	 */
	public void load(final String filename) {
		// first clear table
		Platform.runLater(() -> sidDumpOutputs.clear());
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
							loadAddress = readNumber(group, 16);
							break;

						case 1:
							initAddress = readNumber(group, 16);
							break;

						case 2:
							playerAddress = readNumber(group, 16);
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
							subTune = readNumber(group, 10) + 1;
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
							sidDumpExtension.setFirstFrame(readNumber(group, 10));
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
				sidDumpExtension.setNoteSpacing(0);
				sidDumpExtension.setPatternSpacing(0);
				sidDumpExtension.setLowRes(false);

				int fFetchedRow = 0;
				int fPatternNum = 1;
				int fNoteNum = 1;

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
									sidDumpExtension.setLowRes(true);
									sidDumpExtension.setNoteSpacing(lowresdist);
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
								if (sidDumpExtension.getPatternSpacing() == 0) {
									final int nextFrame = lastFrame + sidDumpExtension.getNoteSpacing();
									sidDumpExtension.setPatternSpacing(nextFrame / sidDumpExtension.getNoteSpacing());
								}
								// pattern spacing?
								fNoteNum = 1;
								fPatternNum = addPatternSpacing(fPatternNum);
							} else if (next.trim().startsWith("+-")) {
								if (sidDumpExtension.getNoteSpacing() == 0) {
									if (!sidDumpExtension.getLowRes()) {
										sidDumpExtension.setNoteSpacing(fFetchedRow);
									}
								}
								// note spacing?
								fNoteNum = addNoteSpacing(fNoteNum);
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
	private int addNoteSpacing(int noteNum) {
		final SidDumpOutput noteSep = new SidDumpOutput();
		noteSep.setTime(String.format("-N%03X", noteNum++));
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
		return noteNum;
	}

	/**
	 * Put a pattern spacing row into the table
	 */
	private int addPatternSpacing(int fPatternNum) {
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
		return fPatternNum;
	}

	/**
	 * Save table into file (create SID dump file)
	 *
	 * @param filename       file name
	 * @param sidDumpOutputs
	 */
	public void save(final String filename, ObservableList<SidDumpOutput> sidDumpOutputs) {
		try (final PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(filename)))) {
			out.println(String.format("Load address: $%04X Init address: $%04X Play address: $%04X", loadAddress,
					initAddress, playerAddress));
			out.println("Calling initroutine with subtune " + (subTune - 1));
			out.println("Calling playroutine for "
					+ (int) (seconds * util.getPlayer().getC64().getClock().getScreenRefresh())
					+ " frames, starting from frame " + sidDumpExtension.getFirstFrame());
			out.println(String.format("Middle C frequency is $%04X", sidDumpExtension.getMiddleCFreq()));
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

	public void add(final SidDumpOutput output) {
		Platform.runLater(() -> sidDumpOutputs.add(output));
	}

	public boolean isAborted() {
		return sidDumpExtension.getFrames() > (long) (sidDumpExtension.getFirstFrame()
				+ recordLength * util.getPlayer().getC64().getClock().getScreenRefresh());
	}

	/**
	 * Replay a recorded SID dump
	 *
	 * @param sidDumpOutputs
	 *
	 * @throws InvalidCommandException
	 */
	public void replay(ObservableList<SidDumpOutput> sidDumpOutputs) throws InvalidCommandException {
		replayer = new SidDumpReplayer(util.getPlayer().getConfig());
		replayer.setLeftVolume(leftVolume);
		replayer.setRegOrder(fRegOrder);
		replayer.setReplayFrequency(fReplayFreq);
		replayer.replay(sidDumpOutputs);
	}

	public void stopReplay() {
		replayer.stopReplay();
	}

}
