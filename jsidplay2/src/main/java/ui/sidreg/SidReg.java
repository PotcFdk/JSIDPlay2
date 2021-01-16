package ui.sidreg;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import javafx.stage.FileChooser;
import libsidutils.PathUtils;
import sidplay.Player;
import sidplay.audio.SidRegDriver;
import sidplay.audio.SidRegDriver.SidRegWrite;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;

public class SidReg extends C64VBox implements UIPart {
	public static final String ID = "SIDREGISTERS";

	final SidRegExtension sidRegExtension = new SidRegExtension() {

		@Override
		public void sidWrite(final SidRegWrite output) {
			Platform.runLater(() -> {
				allSidRegWrites.add(output);
				if (allSidRegWrites.size() % REFRESH_RATE == 0) {
					doUpdateFilter();
				}
			});
		}

		@Override
		public void clear() {
			Platform.runLater(() -> allSidRegWrites.clear());
		}
	};

	private static final int REFRESH_RATE = 1000;

	@FXML
	private ToggleButton startStop;

	@FXML
	private CheckBox startStopPlayer;

	@FXML
	private CheckBox freq1, freq2, freq3, pulse1, pulse2, pulse3, ctrl1, ctrl2, ctrl3, ad1, ad2, ad3, sr1, sr2, sr3,
			filter, vol, paddles, osc3, env3;

	@FXML
	private Button selectAll, deselectAll, saveRegs;

	@FXML
	private TableView<SidRegWrite> regTable;

	private ObservableList<SidRegWrite> filteredSidRegWrites;
	private ObservableList<SidRegWrite> allSidRegWrites;
	private Set<String> filters;

	private PropertyChangeListener sidRegStop;

	public SidReg() {
		super();
	}

	public SidReg(final C64Window window, final Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		filteredSidRegWrites = FXCollections.<SidRegWrite>observableArrayList();
		SortedList<SidRegWrite> sortedList = new SortedList<>(filteredSidRegWrites);
		sortedList.comparatorProperty().bind(regTable.comparatorProperty());
		regTable.setItems(sortedList);

		allSidRegWrites = FXCollections.<SidRegWrite>observableArrayList();

		filters = new HashSet<>();

		doUpdateFilter();

		sidRegStop = event -> {
			if (event.getNewValue() == State.END) {
				Platform.runLater(() -> {
					recordSidWrites(false);
					doUpdateFilter();
				});
			}
		};
		util.getPlayer().stateProperty().addListener(sidRegStop);
	}

	@Override
	public void doClose() {
		util.getPlayer().removeSidListener(sidRegExtension);
		util.getPlayer().stateProperty().removeListener(sidRegStop);
	}

	@FXML
	private void doStartStop() {
		recordSidWrites(startStop.isSelected());
		doUpdateFilter();
		if (startStopPlayer.isSelected()) {
			if (startStop.isSelected()) {
				util.getPlayer().play(util.getPlayer().getTune());
			} else {
				util.getPlayer().stopC64();
			}
		}
	}

	@FXML
	protected void doUpdateFilter() {
		filters.clear();
		if (freq1.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_1_FREQ_L"));
			filters.add(util.getBundle().getString("VOICE_1_FREQ_H"));
		}
		if (freq2.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_2_FREQ_L"));
			filters.add(util.getBundle().getString("VOICE_2_FREQ_H"));
		}
		if (freq3.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_3_FREQ_L"));
			filters.add(util.getBundle().getString("VOICE_3_FREQ_H"));
		}
		if (pulse1.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_1_PULSE_L"));
			filters.add(util.getBundle().getString("VOICE_1_PULSE_H"));
		}
		if (pulse2.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_2_PULSE_L"));
			filters.add(util.getBundle().getString("VOICE_2_PULSE_H"));
		}
		if (pulse3.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_3_PULSE_L"));
			filters.add(util.getBundle().getString("VOICE_3_PULSE_H"));
		}
		if (ctrl1.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_1_CTRL"));
		}
		if (ctrl2.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_2_CTRL"));
		}
		if (ctrl3.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_3_CTRL"));
		}
		if (ad1.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_1_AD"));
		}
		if (ad2.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_2_AD"));
		}
		if (ad3.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_3_AD"));
		}
		if (sr1.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_1_SR"));
		}
		if (sr2.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_2_SR"));
		}
		if (sr3.isSelected()) {
			filters.add(util.getBundle().getString("VOICE_3_SR"));
		}
		if (filter.isSelected()) {
			filters.add(util.getBundle().getString("FCUT_L"));
			filters.add(util.getBundle().getString("FCUT_H"));
			filters.add(util.getBundle().getString("FRES"));
		}
		if (vol.isSelected()) {
			filters.add(util.getBundle().getString("FVOL"));
		}
		if (paddles.isSelected()) {
			filters.add(util.getBundle().getString("PADDLE1"));
			filters.add(util.getBundle().getString("PADDLE2"));
		}
		if (osc3.isSelected()) {
			filters.add(util.getBundle().getString("OSC3"));
		}
		if (env3.isSelected()) {
			filters.add(util.getBundle().getString("ENV3"));
		}
		filteredSidRegWrites.clear();
		for (SidRegWrite sidRegWrite : allSidRegWrites) {
			if (filters.contains(sidRegWrite.getDescription())) {
				filteredSidRegWrites.add(sidRegWrite);
			}
		}
	}

	@FXML
	private void doSelectAll() {
		for (CheckBox checkBox : Arrays.asList(freq1, freq2, freq3, pulse1, pulse2, pulse3, ctrl1, ctrl2, ctrl3, ad1,
				ad2, ad3, sr1, sr2, sr3, filter, vol, paddles, osc3, env3)) {
			checkBox.setSelected(true);
		}
		doUpdateFilter();
	}

	@FXML
	private void doDeselectAll() {
		for (CheckBox checkBox : Arrays.asList(freq1, freq2, freq3, pulse1, pulse2, pulse3, ctrl1, ctrl2, ctrl3, ad1,
				ad2, ad3, sr1, sr2, sr3, filter, vol, paddles, osc3, env3)) {
			checkBox.setSelected(false);
		}
		doUpdateFilter();
	}

	@FXML
	private void doSaveRegs() {
		final FileChooser fileDialog = new FileChooser();
		fileDialog.setInitialDirectory(util.getConfig().getSidplay2Section().getLastDirectory());
		final File file = fileDialog.showSaveDialog(saveRegs.getScene().getWindow());
		if (file != null) {
			util.getConfig().getSidplay2Section().setLastDirectory(file.getParentFile());
			File target = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()) + ".csv");
			try (PrintStream ps = new PrintStream(target)) {
				SidRegDriver.writeHeader(ps);
				filteredSidRegWrites.stream().forEach(sidRegWrite -> sidRegWrite.writeSidRegister(ps));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	protected void recordSidWrites(final boolean enable) {
		if (enable) {
			sidRegExtension.init();
			util.getPlayer().addSidListener(sidRegExtension);
		} else {
			util.getPlayer().removeSidListener(sidRegExtension);
		}
	}
}
