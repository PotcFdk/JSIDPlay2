package ui.download;

import java.io.File;

import ui.events.IMadeProgress;
import ui.events.UIEventFactory;


public abstract class ProgressListener implements IDownloadListener {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	@Override
	public void downloadStep(final int pct) {
		this.uiEvents.fireEvent(IMadeProgress.class, new IMadeProgress() {

			@Override
			public int getPercentage() {
				return pct;
			}
		});
	}

	@Override
	public void downloadStop(File downloadedFile) {

		if (downloadedFile == null) {
			this.uiEvents.fireEvent(IMadeProgress.class, new IMadeProgress() {

				@Override
				public int getPercentage() {
					return 100;
				}
			});
		}
		downloaded(downloadedFile);
	}

	public abstract void downloaded(File downloadedFile);
}