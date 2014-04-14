package netsiddev;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class About extends SIDDeviceStage {

	@FXML
	private TextArea credits;
	
	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}
	
}
