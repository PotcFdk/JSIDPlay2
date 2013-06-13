package ui.common.dialog;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import ui.common.C64Stage;
import ui.events.UIEvent;

public class YesNoDialog extends C64Stage {

	@FXML
	private Text message;
	
	private String text;

	BooleanProperty confirmed = new SimpleBooleanProperty();
	
	@Override
	public URL getFxml() {
		return getClass().getResource("/ui/common/dialog/YesNoDialog.fxml");
	}

	@Override
	public String getBundleName() {
		return "ui/common/dialog/YesNoDialog";
	}

	@Override
	protected String getStyleSheetName() {
		return "/ui/common/dialog/YesNoDialog.css";
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		setWait(true);
		message.setText(text);
	}

	@FXML
	private void yes() {
		((Stage) message.getScene().getWindow()).close();
		confirmed.set(true);
	}

	@FXML
	private void no() {
		((Stage) message.getScene().getWindow()).close();
		confirmed.set(false);
	}

	public void setText(String str) {
		text = str;
	}

	public BooleanProperty getConfirmed() {
		return confirmed;
	}

	@Override
	protected void doCloseWindow() {
	}

	@Override
	public void notify(UIEvent evt) {
	}

}
