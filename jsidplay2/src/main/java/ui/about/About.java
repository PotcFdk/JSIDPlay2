package ui.about;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import libsidplay.Player;
import ui.common.C64Window;

public class About extends C64Window {

	@FXML
	private TextArea credits;

	public About(Player player) {
		super(player);
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
