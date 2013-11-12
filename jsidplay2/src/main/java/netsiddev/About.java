package netsiddev;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class About extends SIDDeviceStage {

	@FXML
	private TextArea credits;
	
	@Override
	public void initialize(URL location, ResourceBundle resources) {
	}

	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}
	
}
