package ui.toolbar;

import java.util.Optional;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import libsidplay.common.ChipModel;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.EnumToStringConverter;
import ui.common.UIPart;
import ui.entities.config.DeviceMapping;

public class SidBlasterDeviceMapping extends C64VBox implements UIPart {

	@FXML
	private CheckBox usedCheckbox;

	@FXML
	private TextField serialNumEditor;

	@FXML
	private ComboBox<ChipModel> chipModelEditor;

	private DeviceMapping deviceMapping;

	private Consumer<DeviceMapping> testSidBlasterDevice, removeSidBlasterDevice;

	public SidBlasterDeviceMapping() {
		super();
	}

	public SidBlasterDeviceMapping(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		final ResourceBundle bundle = util.getBundle();

		Pattern pattern = Pattern.compile("^$|" + bundle.getString("SIDBLASTER_SERIALNUM_FORMAT"));
		serialNumEditor.textProperty().addListener((obj, o, n) -> deviceMapping.setSerialNum(n));
		serialNumEditor.textProperty().addListener(
				(obj, o, n) -> util.checkTextField(serialNumEditor, () -> pattern.matcher(n).find(), () -> {
				}, "SIDBLASTER_SERIALNUM_TIP", "SIDBLASTER_SERIALNUM_FORMAT"));

		chipModelEditor.setItems(FXCollections.<ChipModel>observableArrayList(ChipModel.MOS6581, ChipModel.MOS8580));
		chipModelEditor.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		chipModelEditor.valueProperty().addListener((obj, o, n) -> deviceMapping.setChipModel(n));
	}

	@FXML
	private void setUsed() {
		deviceMapping.setUsed(usedCheckbox.isSelected());
	}

	@FXML
	private void remove() {
		removeSidBlasterDevice.accept(deviceMapping);
	}

	@FXML
	private void testSidBlaster() {
		testSidBlasterDevice.accept(deviceMapping);
	}

	public void init(DeviceMapping deviceMapping, Consumer<DeviceMapping> testSidBlasterDevice,
			Consumer<DeviceMapping> removeSidBlasterDevice) {
		this.deviceMapping = deviceMapping;
		this.testSidBlasterDevice = testSidBlasterDevice;
		this.removeSidBlasterDevice = removeSidBlasterDevice;

		usedCheckbox.setSelected(deviceMapping.isUsed());
		serialNumEditor.setText(deviceMapping.getSerialNum());
		chipModelEditor.setValue(Optional.ofNullable(deviceMapping.getChipModel()).orElse(ChipModel.MOS8580));
	}

}
