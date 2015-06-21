package ui.common.dialog;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import sidplay.Player;
import ui.common.C64Window;

public class AlertDialog extends C64Window {

	@FXML
	private Text message;

	public AlertDialog(Player player) {
		super(player);
		setWait(true);		
	}

	@FXML
	private void initialize() {
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
