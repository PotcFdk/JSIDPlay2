package ui.update;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.TextArea;
import javafx.util.Duration;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.util.DesktopUtil;
import ui.common.util.VersionUtil;
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
		PauseTransition pauseTransition = new PauseTransition(Duration.millis(1000));
		SequentialTransition sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {

			final String localVersion = VersionUtil.getVersion();
			final String remoteVersion = VersionUtil.getRemoteVersion(sidplay2Section);
			boolean updateAvailable = remoteVersion != null
					&& isUpdateAvailable(getVersionNumbers(localVersion), getVersionNumbers(remoteVersion));

			latestVersionLink.setVisible(updateAvailable);
			update.setText(util.getBundle().getString(updateAvailable ? "UPDATE_AVAILABLE" : "NO_UPDATE"));
		});
		sequentialTransition.playFromStart();
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

	private int[] getVersionNumbers(String ver) {
		Matcher m = Pattern.compile("(\\d+)\\.(\\d+)").matcher(ver);
		if (!m.matches()) {
			throw new IllegalArgumentException("Malformed FW version");
		}

		return new int[] { Integer.parseInt(m.group(1)), // major
				Integer.parseInt(m.group(2)), // minor
		};
	}

}
