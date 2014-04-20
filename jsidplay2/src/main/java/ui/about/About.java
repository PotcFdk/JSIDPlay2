package ui.about;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.common.C64Window;
import ui.entities.config.Configuration;

public class About extends C64Window {

	@FXML
	private TextArea credits;

	public About(ConsolePlayer consolePlayer, Player player,
			Configuration config) {
		super(consolePlayer, player, config);
	}

	@FXML
	private void initialize() {
		credits.setText(util.getPlayer().getCredits());
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

}
