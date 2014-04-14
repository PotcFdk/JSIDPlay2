package netsiddev;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Alert extends SIDDeviceStage {

	@FXML
	private Text message;
	
	private String msg;
	
	@FXML
	private void initialize() {
		message.setText(msg);
	}

	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) message.getScene().getWindow()).close();
	}

	public void setMessage(String msg) {
		this.msg = msg;
	}
}
