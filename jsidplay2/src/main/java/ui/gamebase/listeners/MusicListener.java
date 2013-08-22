package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.DoubleProperty;
import ui.download.ProgressListener;
import ui.events.IPlayTune;
import ui.events.UIEventFactory;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;

public class MusicListener extends ProgressListener {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	protected final Object parent;

	public MusicListener(Object parent, DoubleProperty progress) {
		super(progress);
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
			public Object getComponent() {
				return parent;
			}

			@Override
			public String getCommand() {
				return null;
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
		});
	}
}