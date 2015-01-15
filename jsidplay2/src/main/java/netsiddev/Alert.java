package netsiddev;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public class Alert extends SIDDeviceStage {

	@FXML
	private Text message;
	
	public Alert() {
		resizableProperty().set(true);
		setWait(true);		
	}
	
	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) message.getScene().getWindow()).close();
	}

	public void setText(final String msg) {
		message.setText(msg);
	}
}
