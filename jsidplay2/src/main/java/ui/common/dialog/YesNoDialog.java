package ui.common.dialog;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.common.C64Stage;
import ui.entities.config.Configuration;

public class YesNoDialog extends C64Stage {

	@FXML
	private Text message;

	private BooleanProperty confirmed = new SimpleBooleanProperty();

	public YesNoDialog(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
	}

	@FXML
	private void initialize() {
		setWait(true);
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

	public void setText(String text) {
		message.setText(text);
	}

	public BooleanProperty getConfirmed() {
		return confirmed;
	}

}
