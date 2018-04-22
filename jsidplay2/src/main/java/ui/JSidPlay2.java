package ui;

import java.io.File;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import sidplay.Player;
import ui.common.C64Window;

public class JSidPlay2 extends C64Window implements IExtendImageListener, Function<SidTune, String> {

	@FXML
	protected TabPane tabbedPane;

	public JSidPlay2() {
	}
	
	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	protected void initialize() {
		String date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Player.LAST_MODIFIED.getTime());
		if (getStage() != null) {
			getStage().setTitle(util.getBundle().getString("TITLE") + String.format(", %s: %s %s",
					util.getBundle().getString("RELEASE"), date, util.getBundle().getString("AUTHOR")));
		}

		util.getPlayer().setRecordingFilenameProvider(this);
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
			// EXTEND_ASK
			Alert alert = new Alert(AlertType.CONFIRMATION, "");
			alert.setTitle(util.getBundle().getString("EXTEND_DISK_IMAGE"));
			alert.getDialogPane().setHeaderText(util.getBundle().getString("EXTEND_DISK_IMAGE_TO_40_TRACKS"));
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

	/**
	 * Provide a filename for the tune containing some tune infos.
	 * 
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public String apply(SidTune tune) {
		String defaultName = "jsidplay2";
		if (tune == SidTune.RESET) {
			return new File(util.getConfig().getSidplay2Section().getTmpDir(), defaultName).getAbsolutePath();
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> infos = info.getInfoString().iterator();
		String name = infos.hasNext() ? infos.next().replaceAll("[:\\\\/*?|<>]", "_") : defaultName;
		String filename = new File(util.getConfig().getSidplay2Section().getTmpDir(),
				PathUtils.getFilenameWithoutSuffix(name)).getAbsolutePath();
		if (info.getSongs() > 1) {
			filename += String.format("-%02d", info.getCurrentSong());
		}
		return filename;
	}

}
