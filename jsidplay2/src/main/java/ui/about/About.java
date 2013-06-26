package ui.about;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import ui.common.C64Stage;
import ui.events.UIEvent;

public class About extends C64Stage {

	@FXML
	private TextArea credits;

	@Override
	public String getBundleName() {
		return getClass().getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	protected String getStyleSheetName() {
		return "/" + getClass().getName().replace('.', '/') + ".css";
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		credits.setText(getPlayer().getCredits());
	}

	@Override
	protected void doCloseWindow() {
	}

	@FXML
	private void okPressed(ActionEvent event) {
		((Stage) credits.getScene().getWindow()).close();
	}

	@Override
	public void notify(UIEvent evt) {
	}

}
