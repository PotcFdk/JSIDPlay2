package ui.siddump;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

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
import server.netsiddev.InvalidCommandException;
import sidplay.Player;
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

	protected SidDumpExtension sidDumpExtension;

	private int loadAddress, initAddress, playerAddress, subTune;

	private double seconds;

	private Thread fPlayerThread;

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
					sidDumpExtension.stopRecording();
				});
			}
		};
		sidDumpExtension = new SidDumpExtension(util.getPlayer(), util.getConfig()) {

			@Override
			public void add(final SidDumpOutput output) {
				Platform.runLater(() -> sidDumpOutputs.add(output));
			}

			@Override
			public void clear() {
				Platform.runLater(() -> sidDumpOutputs.clear());
			}
		};
		util.getPlayer().stateProperty().addListener(changeListener);

		maxRecordLength.textProperty().addListener((obj, o, n) -> util.checkTextField(maxRecordLength, () -> {
			seconds = new TimeToStringConverter().fromString(maxRecordLength.getText()).doubleValue();
			return seconds != -1;
		}, () -> {
			sidDumpExtension.setRecordLength(seconds);
		}, "MAX_RECORD_LENGTH_TIP", "MAX_RECORD_LENGTH_FORMAT"));

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
	}

	@FXML
	private void doLoadDump() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		final File file = fileDialog.showOpenDialog(loadDump.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			sidDumpExtension.load(file.getAbsolutePath());
			noteSpacing.setText(String.valueOf(sidDumpExtension.getNoteSpacing()));
			patternSpacing.setText(String.valueOf(sidDumpExtension.getPatternSpacing()));
			firstFrame.setText(String.valueOf(sidDumpExtension.getFirstFrame()));
			lowResolutionMode.setSelected(sidDumpExtension.getLowRes());
			loadAddress = sidDumpExtension.getLoadAddress();
			initAddress = sidDumpExtension.getInitAddress();
			playerAddress = sidDumpExtension.getPlayerAddress();
			subTune = sidDumpExtension.getCurrentSong();
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
			sidDumpExtension.save(file.getAbsolutePath(), sidDumpOutputs);
		}
	}

	@FXML
	private void doReplayAll() {
		try {
			while (fPlayerThread != null && fPlayerThread.isAlive()) {
				sidDumpExtension.stopReplay();
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
					sidDumpExtension.replay(sidDumpOutputs);
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
			util.getPlayer().getC64().setPlayRoutineObserver(sidDumpExtension);
			// restart tune, before recording starts
			util.setPlayingTab(this);
			if (startStopPlayer.isSelected()) {
				util.getPlayer().play(util.getPlayer().getTune());
			}
		} else {
			util.getPlayer().getC64().setPlayRoutineObserver(null);
			sidDumpExtension.stopRecording();
			if (startStopPlayer.isSelected()) {
				util.getPlayer().stopC64();
			}
		}
	}

	@FXML
	private void doSetPlayer() {
		sidDumpExtension.setRegOrder(regPlayer.getSelectionModel().getSelectedItem().getRegs());
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
			sidDumpExtension.setReplayFrequency(speed * 50);
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
		sidDumpExtension.setLoadAddress(loadAddress);
		initAddress = tune.getInfo().getInitAddr();
		sidDumpExtension.setInitAddress(initAddress);
		playerAddress = tune.getInfo().getPlayAddr();
		sidDumpExtension.setPayerAddress(playerAddress);
		subTune = tune.getInfo().getCurrentSong();
		sidDumpExtension.setCurrentSong(subTune);
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
			sidDumpExtension.setRecordLength(length);
		} else {
			sidDumpExtension.setRecordLength(seconds);
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
		sidDumpExtension.setLeftVolume(util.getConfig().getAudioSection().getMainVolume());
		sidDumpExtension.setRegOrder(regPlayer.getSelectionModel().getSelectedItem().getRegs());
		sidDumpExtension.init();
	}

}
