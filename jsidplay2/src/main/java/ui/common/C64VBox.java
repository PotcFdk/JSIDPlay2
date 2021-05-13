package ui.common;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;
import sidplay.Player;

public abstract class C64VBox extends VBox implements UIPart, Initializable {

	protected UIUtil util;

	public C64VBox() {
		util = onlyForEclipseJavaFXPreviewView();
		// TODO Uncomment line ONLY for JavaFX Preview in Eclipse of JSidPlay2.fxml
		// and Oscilloscope.fxml, if you get problems in preview view
//		util.parse(this);
	}

	public C64VBox(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		util.setBundle(resources);
		initialize();
	}

	protected abstract void initialize();

}
