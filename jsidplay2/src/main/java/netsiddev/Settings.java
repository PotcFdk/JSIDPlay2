package netsiddev;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.Mixer.Info;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

public class Settings extends SIDDeviceStage {

	@FXML
	private ComboBox<String> device, digiBoost;

	private ObservableList<String> deviceItems2 = FXCollections
			.<String> observableArrayList();
	private ObservableList<String> digiBoosts = FXCollections
			.<String> observableArrayList();

	protected Map<Integer, Integer> indexMapping = new HashMap<Integer, Integer>();

	protected static Mixer.Info[] devices;

	protected int currentDeviceIndex = 0;
	private boolean digiBoostEnabled = false;
	protected static String primaryDeviceName = "Primary Sound Driver";

	protected SIDDeviceSettings settings;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		device.setItems(deviceItems2);
		digiBoost.setItems(digiBoosts);

		settings = SIDDeviceSettings.getInstance();
		currentDeviceIndex = settings.getDeviceIndex();
		digiBoostEnabled = settings.getDigiBoostEnabled();

		indexMapping.clear();
		devices = AudioSystem.getMixerInfo();

		List<DeviceItem> deviceItems = new ArrayList<DeviceItem>();

		int deviceIndex = 0;
		for (Info info : devices) {
			Mixer mixer = AudioSystem.getMixer(info);
			Line.Info lineInfo = new Line.Info(SourceDataLine.class);
			if (mixer.isLineSupported(lineInfo)) {
				DeviceItem deviceItem = new DeviceItem(deviceIndex, info);
				deviceItems.add(deviceItem);
			}
			deviceIndex++;
		}

		if (deviceItems.size() > 0) {
			// first device name is the primary device driver which can
			// be translated on some systems
			primaryDeviceName = deviceItems.get(0).getInfo().getName();
			Collections.sort(deviceItems, new DeviceItemCompare());

			digiBoosts.add("Enabled");
			digiBoosts.add("Disabled");
			digiBoost.getSelectionModel().select(digiBoostEnabled ? 0 : 1);

		}

		int comboBoxIndex = 0;
		for (DeviceItem deviceItem : deviceItems) {
			deviceIndex = deviceItem.getIndex();

			indexMapping.put(comboBoxIndex, deviceIndex);
			deviceItems2.add(deviceItem.getInfo().getName());

			if (deviceIndex == currentDeviceIndex) {
				device.getSelectionModel().select(comboBoxIndex);
			}

			comboBoxIndex++;
		}
	}

	@FXML
	private void setDevice() {
		int comboBoxIndex = device.getSelectionModel().getSelectedIndex();
		int deviceIndex = indexMapping.get(comboBoxIndex);

		ClientContext.changeDevice(devices[deviceIndex]);

		currentDeviceIndex = deviceIndex;
		settings.saveDeviceIndex(currentDeviceIndex);
	}

	@FXML
	private void setDigiBoost() {
		int comboBoxIndex = digiBoost.getSelectionModel().getSelectedIndex();
		ClientContext.setDigiBoost(comboBoxIndex == 0);

		settings.saveDigiBoost(comboBoxIndex == 0);
	}

	@FXML
	private void okPressed(ActionEvent event) {
		settings.saveDeviceIndex(currentDeviceIndex);
		((Stage) device.getScene().getWindow()).close();
	}

	protected static class DeviceItemCompare implements Comparator<DeviceItem> {
		@Override
		public int compare(DeviceItem d1, DeviceItem d2) {
			String devName1 = d1.getInfo().getName();
			String devName2 = d2.getInfo().getName();
			// Make sure the Primary Sound Driver is the first entry
			if (primaryDeviceName.equals(devName1)) {
				return -1;
			}
			if (primaryDeviceName.equals(devName2)) {
				return 1;
			} else {
				// group the device names by device type which is most of the
				// times
				// between brackets at the end of the string
				int index = devName1.lastIndexOf('(');
				if (index >= 0) {
					devName1 = devName1.substring(index) + devName1;
				}
				index = devName2.lastIndexOf('(');
				if (index >= 0) {
					devName2 = devName2.substring(index) + devName2;
				}

				return devName1.compareTo(devName2);
			}
		}
	}

	private class DeviceItem {
		private Info info;
		private Integer index;

		public Info getInfo() {
			return info;
		}

		public Integer getIndex() {
			return index;
		}

		public DeviceItem(final Integer index, final Info info) {
			this.index = index;
			this.info = info;
		}
	}

}
