package applet.gamebase.listeners;

import java.io.File;

import applet.events.IMadeProgress;
import applet.events.UIEventFactory;
import applet.soundsettings.IDownloadListener;

public abstract class ProgressListener implements IDownloadListener {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	@Override
	public void downloadStep(final int pct) {
		this.uiEvents.fireEvent(IMadeProgress.class,
				new IMadeProgress() {

					@Override
					public int getPercentage() {
						return pct;
					}
				});
	}

	@Override
	public void downloadStop(File downloadedFile) {

		if (downloadedFile == null) {
			this.uiEvents.fireEvent(IMadeProgress.class,
					new IMadeProgress() {

						@Override
						public int getPercentage() {
							return 100;
						}
					});
		} else {
			downloaded(downloadedFile);
		}
	}

	public abstract void downloaded(File downloadedFile);
}