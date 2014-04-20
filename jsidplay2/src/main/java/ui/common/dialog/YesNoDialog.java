package ui.common.dialog;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.common.C64Window;
import ui.entities.config.Configuration;

public class YesNoDialog extends C64Window {

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
		close();
		confirmed.set(true);
	}

	@FXML
	private void no() {
		close();
		confirmed.set(false);
	}

	public void setText(String text) {
		message.setText(text);
	}

	public BooleanProperty getConfirmed() {
		return confirmed;
	}

}
