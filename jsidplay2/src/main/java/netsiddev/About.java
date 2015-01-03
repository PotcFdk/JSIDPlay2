package netsiddev;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

public class About extends SIDDeviceStage {

	@FXML
	private TextFlow credits;
	
	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}
	
	@FXML
	private void initialize() {
		setWait(true);
	}
}
