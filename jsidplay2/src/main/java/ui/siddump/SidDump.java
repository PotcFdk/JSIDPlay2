package ui.siddump;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;

import javax.xml.parsers.ParserConfigurationException;

import libsidplay.Player;
import libsidplay.player.State;
import libsidplay.sidtune.SidTune;
import libsidutils.SIDDumpConfiguration;
import libsidutils.SIDDumpConfiguration.SIDDumpPlayer;
import netsiddev.InvalidCommandException;

import org.xml.sax.SAXException;

import sidplay.ini.IniReader;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.SidPlay2Section;

public class SidDump extends Tab implements UIPart {

	public static final String ID = "SIDDUMP";
	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private Button loadDump, saveDump, stop;
	@FXML
	protected ToggleButton replayAll, startStopRecording;
	@FXML
	private CheckBox timeInSeconds, lowResolutionMode;
	@FXML
	private TextField firstFrame, noteSpacing, maxRecordLength, patternSpacing,
			oldNoteFactor, tableFontSize, baseFreq, baseNote, callsPerFrame;
	@FXML
	private ComboBox<SIDDumpPlayer> regPlayer;
	@FXML
	private TableView<SidDumpOutput> dumpTable;

	protected ObservableList<SidDumpOutput> sidDumpOutputs;

	private ObservableList<SIDDumpPlayer> sidDumpPlayers;

	protected SidDumpExtension sidDumpExtension;

	private int loadAddress, initAddress, playerAddress, subTune, seconds;

	private Thread fPlayerThread;

	private UIUtil util;

	private ChangeListener<State> changeListener = (observable, oldValue,
			newValue) -> {
		if (newValue == State.RUNNING) {
			Platform.runLater(() -> setTune(util.getPlayer().getTune()));
		}
		if (newValue == State.EXIT) {
			Platform.runLater(() -> {
				replayAll.setDisable(false);
				sidDumpExtension.stopRecording();
			});
		}
	};

	public SidDump(final C64Window window, final Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString("SIDDUMP"));
	}

	@FXML
	private void initialize() {
		sidDumpExtension = new SidDumpExtension(util.getPlayer(),
				util.getConfig()) {

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

		sidDumpOutputs = FXCollections.<SidDumpOutput> observableArrayList();
		dumpTable.setItems(sidDumpOutputs);
		sidDumpPlayers = FXCollections.<SIDDumpPlayer> observableArrayList();
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		final File file = fileDialog.showOpenDialog(loadDump.getScene()
				.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
			sidDumpExtension.load(file.getAbsolutePath());
			noteSpacing.setText(String.valueOf(sidDumpExtension
					.getNoteSpacing()));
			patternSpacing.setText(String.valueOf(sidDumpExtension
					.getPatternSpacing()));
			firstFrame
					.setText(String.valueOf(sidDumpExtension.getFirstFrame()));
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
		fileDialog.setInitialDirectory(((SidPlay2Section) (util.getConfig()
				.getSidplay2())).getLastDirectoryFolder());
		final File file = fileDialog.showSaveDialog(saveDump.getScene()
				.getWindow());
		if (file != null) {
			util.getConfig().getSidplay2()
					.setLastDirectory(file.getParentFile().getAbsolutePath());
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
			});
			fPlayerThread.start();
		}
	}

	@FXML
	private void doStartStopRecording() {
		if (startStopRecording.isSelected()) {
			// restart tune, before recording starts
			util.getPlayer().play(util.getPlayer().getTune());
			setTune(util.getPlayer().getTune());
			util.getPlayer().getC64().setPlayRoutineObserver(sidDumpExtension);
		} else {
			util.getPlayer().pause();
			util.getPlayer().getC64().setPlayRoutineObserver(null);
			sidDumpExtension.stopRecording();
		}
	}

	@FXML
	private void doSetPlayer() {
		sidDumpExtension.setRegOrder(regPlayer.getSelectionModel()
				.getSelectedItem().getRegs());
	}

	@FXML
	private void doSetNoteSpacing() {
		final Tooltip tooltip = new Tooltip();
		try {
			noteSpacing.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			if (Integer.parseInt(noteSpacing.getText()) >= 0) {
				tooltip.setText(util.getBundle().getString("NOTE_SPACING_TIP"));
				noteSpacing.setTooltip(tooltip);
				noteSpacing.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("NOTE_SPACING_NEG"));
			noteSpacing.setTooltip(tooltip);
			noteSpacing.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetMaxRecordLength() {
		final Tooltip tooltip = new Tooltip();
		maxRecordLength.getStyleClass().removeAll(CELL_VALUE_OK,
				CELL_VALUE_ERROR);
		seconds = IniReader.parseTime(maxRecordLength.getText());
		if (seconds != -1) {
			tooltip.setText(util.getBundle().getString("MAX_RECORD_LENGTH_TIP"));
			maxRecordLength.setTooltip(tooltip);
			maxRecordLength.getStyleClass().add(CELL_VALUE_OK);
		} else {
			tooltip.setText(util.getBundle().getString(
					"MAX_RECORD_LENGTH_FORMAT"));
			maxRecordLength.setTooltip(tooltip);
			maxRecordLength.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetPatternSpacing() {
		final Tooltip tooltip = new Tooltip();
		try {
			patternSpacing.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			if (Integer.parseInt(patternSpacing.getText()) >= 0) {
				tooltip.setText(util.getBundle().getString(
						"PATTERN_SPACING_TIP"));
				patternSpacing.setTooltip(tooltip);
				patternSpacing.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("PATTERN_SPACING_NEG"));
			patternSpacing.setTooltip(tooltip);
			patternSpacing.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetOldNoteFactor() {
		final Tooltip tooltip = new Tooltip();
		try {
			oldNoteFactor.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			if (Float.parseFloat(oldNoteFactor.getText()) >= 1) {
				tooltip.setText(util.getBundle().getString(
						"OLD_NOTE_FACTOR_TIP"));
				oldNoteFactor.setTooltip(tooltip);
				oldNoteFactor.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("OLD_NOTE_FACTOR_NEG"));
			oldNoteFactor.setTooltip(tooltip);
			oldNoteFactor.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetBaseFreq() {
		final Tooltip tooltip = new Tooltip();
		try {
			baseFreq.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			if (Integer.decode(baseFreq.getText()) >= 0) {
				tooltip.setText(util.getBundle().getString("BASE_FREQ_TIP"));
				baseFreq.setTooltip(tooltip);
				baseFreq.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("BASE_FREQ_HEX"));
			baseFreq.setTooltip(tooltip);
			baseFreq.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetTableFontSize() {
		final Tooltip tooltip = new Tooltip();
		try {
			tableFontSize.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			int fontSizeVal = Integer.parseInt(tableFontSize.getText());
			if (fontSizeVal > 0 && fontSizeVal <= 24) {
				tooltip.setText(util.getBundle().getString(
						"TABLE_FONT_SIZE_TIP"));
				tableFontSize.setTooltip(tooltip);
				tableFontSize.getStyleClass().add(CELL_VALUE_OK);
				dumpTable.setStyle(String.format("-fx-font-size:%d.0px;}",
						fontSizeVal));
				for (TableColumn<SidDumpOutput, ?> column : dumpTable
						.getColumns()) {
					column.setStyle(String.format("-fx-font-size:%d.0px;",
							fontSizeVal));

				}
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("TABLE_FONT_SIZE_NEG"));
			tableFontSize.setTooltip(tooltip);
			tableFontSize.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetBaseNote() {
		final Tooltip tooltip = new Tooltip();
		try {
			baseNote.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			int baseNoteVal = Integer.decode(baseNote.getText());
			if (baseNoteVal >= 128 && baseNoteVal <= 223) {
				tooltip.setText(util.getBundle().getString("BASE_NOTE_TIP"));
				baseNote.setTooltip(tooltip);
				baseNote.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("BASE_NOTE_HEX"));
			baseNote.setTooltip(tooltip);
			baseNote.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetCallsPerFrame() {
		final Tooltip tooltip = new Tooltip();
		try {
			callsPerFrame.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			final int speed = Integer.parseInt(callsPerFrame.getText());
			if (speed >= 1) {
				tooltip.setText(util.getBundle().getString(
						"CALLS_PER_FRAME_TIP"));
				callsPerFrame.setTooltip(tooltip);
				callsPerFrame.getStyleClass().add(CELL_VALUE_OK);
				sidDumpExtension.setReplayFrequency(speed * 50);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("CALLS_PER_FRAME_NEG"));
			callsPerFrame.setTooltip(tooltip);
			callsPerFrame.getStyleClass().add(CELL_VALUE_ERROR);
		}
	}

	@FXML
	private void doSetFirstFrame() {
		final Tooltip tooltip = new Tooltip();
		try {
			firstFrame.getStyleClass().removeAll(CELL_VALUE_OK,
					CELL_VALUE_ERROR);
			if (Long.parseLong(firstFrame.getText()) >= 0) {
				tooltip.setText(util.getBundle().getString("FIRST_FRAME_TIP"));
				firstFrame.setTooltip(tooltip);
				firstFrame.getStyleClass().add(CELL_VALUE_OK);
			} else {
				throw new NumberFormatException();
			}
		} catch (final NumberFormatException e) {
			tooltip.setText(util.getBundle().getString("FIRST_FRAME_NEG"));
			firstFrame.setTooltip(tooltip);
			firstFrame.getStyleClass().add(CELL_VALUE_ERROR);
		}
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
			int length = util.getPlayer().getSidDatabaseInfo(
					db -> db.getSongLength(tune));
			if (length == 0) {
				length = util.getConfig().getSidplay2().getDefaultPlayLength();
				if (length == 0) {
					// default
					length = 60;
				}
			}
			maxRecordLength.setText(String.format("%02d:%02d",
					(length / 60 % 100), (length % 60)));
			sidDumpExtension.setRecordLength(length);
		} else {
			sidDumpExtension.setRecordLength(seconds);
		}
		sidDumpExtension.setTimeInSeconds(timeInSeconds.isSelected());
		sidDumpExtension.setOldNoteFactor(Float.parseFloat(oldNoteFactor
				.getText()));
		sidDumpExtension.setBaseFreq(Integer.decode(baseFreq.getText()));
		sidDumpExtension.setBaseNote(Integer.decode(baseNote.getText()));
		sidDumpExtension.setPatternSpacing(Integer.valueOf(patternSpacing
				.getText()));
		sidDumpExtension
				.setNoteSpacing(Integer.parseInt(noteSpacing.getText()));
		sidDumpExtension.setLowRes(lowResolutionMode.isSelected());

		if (tune.getInfo().getPlayAddr() == 0) {
			startStopRecording.setSelected(false);
			startStopRecording.setTooltip(new Tooltip(util.getBundle()
					.getString("NOT_AVAILABLE")));
		} else {
			startStopRecording.setTooltip(new Tooltip(null));
		}
		sidDumpExtension.setLeftVolume(util.getConfig().getAudio()
				.getLeftVolume());
		sidDumpExtension.init();
	}

}
