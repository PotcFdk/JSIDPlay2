package ui.about;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.text.Text;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.util.VersionUtil;

public class About extends C64Window {

	@FXML
	private Text credits;

	public About() {
		super();
	}

	public About(Player player) {
		super(player);
		getStage().resizableProperty().set(false);
	}

	@FXML
	@Override
	protected void initialize() {
		credits.setText(util.getPlayer().getCredits(VersionUtil.getVersion()));
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

}
