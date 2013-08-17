package ui.download;

import java.io.File;

import javafx.beans.property.DoubleProperty;


public abstract class ProgressListener implements IDownloadListener {
	private DoubleProperty progress;

	public ProgressListener(DoubleProperty progress) {
		this.progress = progress;
	}
	
	@Override
	public void downloadStep(final int pct) {
		progress.set(pct);
	}

	@Override
	public void downloadStop(File downloadedFile) {

		if (downloadedFile == null) {
			progress.set(0);
		}
		downloaded(downloadedFile);
	}

	public abstract void downloaded(File downloadedFile);
}