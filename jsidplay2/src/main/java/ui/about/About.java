package ui.about;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import ui.common.C64Stage;

public class About extends C64Stage {

	@FXML
	private TextArea credits;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		credits.setText(getPlayer().getCredits());
	}

	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}

}
