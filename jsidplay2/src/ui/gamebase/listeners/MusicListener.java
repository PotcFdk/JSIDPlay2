package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import ui.download.ProgressListener;
import ui.events.IPlayTune;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;

public class MusicListener extends ProgressListener {

	protected final Object parent;

	public MusicListener(Object parent) {
		this.parent = parent;
	}

	@Override
	public void downloaded(final File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		downloadedFile.deleteOnExit();
		// play tune
		uiEvents.fireEvent(IPlayTune.class, new IPlayTune() {
			@Override
			public boolean switchToVideoTab() {
				return false;
			}

			@Override
			public SidTune getSidTune() {
				try {
					return SidTune.load(downloadedFile);
				} catch (IOException | SidTuneError e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			public Object getComponent() {
				return parent;
			}
		});
	}
}