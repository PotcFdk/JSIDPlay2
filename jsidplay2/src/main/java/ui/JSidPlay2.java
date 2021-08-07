package ui;

import static sidplay.Player.LAST_MODIFIED;

import java.text.DateFormat;
import java.util.Optional;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import sidplay.Player;
import ui.common.C64Window;

public class JSidPlay2 extends C64Window implements IExtendImageListener {

	@FXML
	protected TabPane tabbedPane;

	public JSidPlay2() {
		super();
	}

	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	@Override
	protected void initialize() {
		Platform.runLater(() -> {
			// must be delayed, otherwise overridden by C64Window
			String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(LAST_MODIFIED.getTime());
			getStage().setTitle(util.getBundle().getString("TITLE") + String.format(", %s: %s %s",
					util.getBundle().getString("RELEASE"), date, util.getBundle().getString("AUTHOR")));
		});

		util.getPlayer().setExtendImagePolicy(this);
	}

	public TabPane getTabbedPane() {
		return tabbedPane;
	}

	@Override
	public void doClose() {
		util.getPlayer().quit();
		Platform.exit();
	}

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			String msg = util.getBundle().getString("EXTEND_DISK_IMAGE_TO_40_TRACKS");

			// EXTEND_ASK
			Alert alert = new Alert(AlertType.CONFIRMATION, msg);
			alert.setTitle(util.getBundle().getString("EXTEND_DISK_IMAGE"));
			Optional<ButtonType> result = alert.showAndWait();
			return result.isPresent() && result.get() == ButtonType.OK;
		} else if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
			// EXTEND_ACCESS
			return true;
		} else {
			// EXTEND_NEVER
			return false;
		}
	}

}
