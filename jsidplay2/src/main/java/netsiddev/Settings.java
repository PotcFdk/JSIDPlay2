package netsiddev;

import java.net.URL;
import java.util.Collections;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

public class Settings extends SIDDeviceStage {

	@FXML
	private ComboBox<AudioDevice> audioDevice;
	@FXML
	private CheckBox digiBoost;

	private ObservableList<AudioDevice> audioDevices = FXCollections
			.<AudioDevice> observableArrayList();

	private SIDDeviceSettings settings = SIDDeviceSettings.getInstance();

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		audioDevice.setItems(audioDevices);

		AudioDeviceCompare cmp = new AudioDeviceCompare();
		AudioDevice selectedAudioDeviceItem = null;
		int deviceIndex = 0;
		for (Info info : AudioSystem.getMixerInfo()) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				AudioDevice audioDeviceItem = new AudioDevice(
						deviceIndex, info);
				audioDevices.add(audioDeviceItem);
				if (deviceIndex == 0) {
					// first device name is the primary device driver which can
					// be translated on some systems
					cmp.setPrimaryDeviceName(info.getName());
				}
				if (audioDeviceItem.getIndex() == settings
						.getDeviceIndex()) {
					selectedAudioDeviceItem = audioDeviceItem;
				}
			}
			deviceIndex++;
		}
		Collections.sort(audioDevices, cmp);
		audioDevice.getSelectionModel().select(selectedAudioDeviceItem);
		digiBoost.setSelected(settings.getDigiBoostEnabled());
	}

	@FXML
	private void setAudioDevice() {
		ClientContext.changeDevice(audioDevice.getSelectionModel()
				.getSelectedItem().getInfo());
		settings.saveDeviceIndex(audioDevice.getSelectionModel()
				.getSelectedItem().getIndex());
	}

	@FXML
	private void setDigiBoost() {
		ClientContext.setDigiBoost(digiBoost.isSelected());
		settings.saveDigiBoost(digiBoost.isSelected());
	}

	@FXML
	private void okPressed(ActionEvent event) {
		settings.saveDeviceIndex(settings.getDeviceIndex());
		((Stage) audioDevice.getScene().getWindow()).close();
	}

}
