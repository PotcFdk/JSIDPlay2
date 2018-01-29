package ui.download;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Scene;
import ui.common.UIUtil;

public abstract class ProgressListener implements IDownloadListener {
	protected UIUtil util;
	private Scene scene;

	public ProgressListener(UIUtil util, Scene scene) {
		this.util = util;
		this.scene = scene;
	}

	@Override
	public void downloadStep(final int pct) {
		Platform.runLater(() -> util.progressProperty(scene).set(pct / 100.));
	}

	@Override
	public void downloadStop(File downloadedFile) {

		if (downloadedFile == null) {
			Platform.runLater(() -> util.progressProperty(scene).set(0));
		}
		downloaded(downloadedFile);
	}

	public abstract void downloaded(File downloadedFile);
}