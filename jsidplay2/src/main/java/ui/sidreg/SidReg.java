package ui.sidreg;

import static sidplay.ConsolePlayer.playerExit;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleButton;
import ui.common.C64Stage;

public class SidReg extends C64Stage {

	private static final int REFRESH_RATE = 1000;

	@FXML
	private ToggleButton startStop;

	@FXML
	private CheckBox freq1, freq2, freq3, pulse1, pulse2, pulse3, ctrl1, ctrl2,
			ctrl3, ad1, ad2, ad3, sr1, sr2, sr3, filter, vol, paddles, osc3,
			env3;

	@FXML
	private Button selectAll, deselectAll;;

	@FXML
	private TableView<SidRegWrite> regTable;

	private ObservableList<SidRegWrite> filteredSidRegWrites = FXCollections
			.<SidRegWrite> observableArrayList();
	private ObservableList<SidRegWrite> allSidRegWrites = FXCollections
			.<SidRegWrite> observableArrayList();
	private Set<String> filters = new HashSet<String>();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		getConsolePlayer().getState().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> arg0,
					Number arg1, Number arg2) {
				if (arg2.intValue() == playerExit) {
					recordSidWrites(false);
				}
			}
		});
		regTable.setItems(filteredSidRegWrites);
		doUpdateFilter();
	}

	@FXML
	private void doStartStop() {
		recordSidWrites(startStop.isSelected());
		doUpdateFilter();
	}

	@FXML
	private void doUpdateFilter() {
		filters.clear();
		if (freq1.isSelected()) {
			filters.add(getBundle().getString("VOICE_1_FREQ_L"));
			filters.add(getBundle().getString("VOICE_1_FREQ_H"));
		}
		if (freq2.isSelected()) {
			filters.add(getBundle().getString("VOICE_2_FREQ_L"));
			filters.add(getBundle().getString("VOICE_2_FREQ_H"));
		}
		if (freq3.isSelected()) {
			filters.add(getBundle().getString("VOICE_3_FREQ_L"));
			filters.add(getBundle().getString("VOICE_3_FREQ_H"));
		}
		if (pulse1.isSelected()) {
			filters.add(getBundle().getString("VOICE_1_PULSE_L"));
			filters.add(getBundle().getString("VOICE_1_PULSE_H"));
		}
		if (pulse2.isSelected()) {
			filters.add(getBundle().getString("VOICE_2_PULSE_L"));
			filters.add(getBundle().getString("VOICE_2_PULSE_H"));
		}
		if (pulse3.isSelected()) {
			filters.add(getBundle().getString("VOICE_3_PULSE_L"));
			filters.add(getBundle().getString("VOICE_3_PULSE_H"));
		}
		if (ctrl1.isSelected()) {
			filters.add(getBundle().getString("VOICE_1_CTRL"));
		}
		if (ctrl2.isSelected()) {
			filters.add(getBundle().getString("VOICE_2_CTRL"));
		}
		if (ctrl3.isSelected()) {
			filters.add(getBundle().getString("VOICE_3_CTRL"));
		}
		if (ad1.isSelected()) {
			filters.add(getBundle().getString("VOICE_1_AD"));
		}
		if (ad2.isSelected()) {
			filters.add(getBundle().getString("VOICE_2_AD"));
		}
		if (ad3.isSelected()) {
			filters.add(getBundle().getString("VOICE_3_AD"));
		}
		if (sr1.isSelected()) {
			filters.add(getBundle().getString("VOICE_1_SR"));
		}
		if (sr2.isSelected()) {
			filters.add(getBundle().getString("VOICE_2_SR"));
		}
		if (sr3.isSelected()) {
			filters.add(getBundle().getString("VOICE_3_SR"));
		}
		if (filter.isSelected()) {
			filters.add(getBundle().getString("FCUT_L"));
			filters.add(getBundle().getString("FCUT_H"));
			filters.add(getBundle().getString("FRES"));
		}
		if (vol.isSelected()) {
			filters.add(getBundle().getString("FVOL"));
		}
		if (paddles.isSelected()) {
			filters.add(getBundle().getString("PADDLE1"));
			filters.add(getBundle().getString("PADDLE2"));
		}
		if (osc3.isSelected()) {
			filters.add(getBundle().getString("OSC3"));
		}
		if (env3.isSelected()) {
			filters.add(getBundle().getString("ENV3"));
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
		for (CheckBox checkBox : Arrays.asList(freq1, freq2, freq3, pulse1,
				pulse2, pulse3, ctrl1, ctrl2, ctrl3, ad1, ad2, ad3, sr1, sr2,
				sr3, filter, vol, paddles, osc3, env3)) {
			checkBox.setSelected(true);
		}
		doUpdateFilter();
	}

	@FXML
	private void doDeselectAll() {
		for (CheckBox checkBox : Arrays.asList(freq1, freq2, freq3, pulse1,
				pulse2, pulse3, ctrl1, ctrl2, ctrl3, ad1, ad2, ad3, sr1, sr2,
				sr3, filter, vol, paddles, osc3, env3)) {
			checkBox.setSelected(false);
		}
		doUpdateFilter();
	}

	private void recordSidWrites(final boolean enable) {
		SidRegExtension sidRegExtension = null;
		if (enable) {
			sidRegExtension = new SidRegExtension() {

				@Override
				public void sidWrite(final SidRegWrite output) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							allSidRegWrites.add(output);
							if (allSidRegWrites.size() % REFRESH_RATE == 0) {
								doUpdateFilter();
							}
						}
					});
				}

				@Override
				public void clear() {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							allSidRegWrites.clear();
						}
					});
				}

			};
			sidRegExtension.setbundle(getBundle());
			sidRegExtension.init();
		}
		getPlayer().getC64().setSidWriteListener(0, sidRegExtension);
		if (getPlayer().getTune() != null
				&& getPlayer().getTune().getInfo().sidChipBase2 != 0) {
			getPlayer().getC64().setSidWriteListener(1, sidRegExtension);
		}
	}

}
