package ui.update;

import static ui.common.util.VersionUtil.VERSION;
import static ui.common.util.VersionUtil.fetchRemoteVersion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.util.DesktopUtil;
import ui.entities.config.SidPlay2Section;

public class Update extends C64Window {

	@FXML
	private TextArea update;
	@FXML
	private Hyperlink latestVersionLink;

	public Update() {
		super();
	}

	public Update(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		update.setText(util.getBundle().getString("PLEASE_WAIT"));

		Task<Void> task = new Task<Void>() {
			@Override
			public Void call() throws Exception {
				final String remoteVersion = fetchRemoteVersion(sidplay2Section);
				boolean updateAvailable = remoteVersion != null
						&& isUpdateAvailable(getVersionNumbers(VERSION), getVersionNumbers(remoteVersion));

				latestVersionLink.setVisible(updateAvailable);
				update.setText(util.getBundle().getString(updateAvailable ? "UPDATE_AVAILABLE" : "NO_UPDATE"));
				return null;
			}
		};
		new Thread(task).start();
	}

	@FXML
	private void gotoLatestVersion() {
		DesktopUtil.browse("http://sourceforge.net/projects/jsidplay2/files/latest/download");
	}

	@FXML
	private void okPressed(ActionEvent event) {
		close();
	}

	private boolean isUpdateAvailable(int[] currentVersion, int[] latestVersion) {
		if (currentVersion == null || latestVersion == null) {
			// undetermined local or remote version? Do not flag an update
			return false;
		}
		for (int i = 0; i < currentVersion.length; i++) {
			if (currentVersion[i] != latestVersion[i]) {
				// current version is outdated
				return currentVersion[i] < latestVersion[i];
			}
		}
		// version is equal
		return false;
	}

	private int[] getVersionNumbers(String version) {
		Matcher m = Pattern.compile("(\\d+)\\.(\\d+)").matcher(version);
		if (!m.matches()) {
			System.err.println("Malformed version number: " + version);
			return null;
		}

		return new int[] { Integer.parseInt(m.group(1)), // major
				Integer.parseInt(m.group(2)), // minor
		};
	}

}
