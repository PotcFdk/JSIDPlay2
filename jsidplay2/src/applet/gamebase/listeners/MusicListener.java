package applet.gamebase.listeners;

import java.awt.Component;
import java.io.File;

import applet.events.IPlayTune;
import applet.gamebase.GameBase;

public class MusicListener extends ProgressListener {

	/**
	 * 
	 */
	protected final GameBase gameBase;

	public MusicListener(GameBase gameBase) {
		this.gameBase = gameBase;
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
			public File getFile() {
				return downloadedFile;
			}

			@Override
			public Component getComponent() {
				return gameBase;
			}
		});
	}
}