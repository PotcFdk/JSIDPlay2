package ui.download;

import java.io.File;

import javafx.application.Platform;
import javafx.scene.Node;
import ui.common.UIUtil;

public abstract class ProgressListener implements IDownloadListener {
	private UIUtil util;
	private Node node;

	public ProgressListener(UIUtil util, Node node) {
		this.util = util;
		this.node = node;
	}

	@Override
	public void downloadStep(final int pct) {
		Platform.runLater(() -> util.progressProperty(node).set(pct / 100.));
	}

	@Override
	public void downloadStop(File downloadedFile) {

		if (downloadedFile == null) {
			Platform.runLater(() -> util.progressProperty(node).set(0));
		}
		downloaded(downloadedFile);
	}

	public abstract void downloaded(File downloadedFile);
}