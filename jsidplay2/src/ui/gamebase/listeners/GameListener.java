package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import ui.download.ProgressListener;
import ui.entities.config.Configuration;
import ui.events.IInsertMedia;
import ui.events.Reset;

import libsidplay.Player;
import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;

public class GameListener extends ProgressListener {

	private String fileToRun;
	protected Object parent;
	protected Player player;
	protected Configuration config;

	public GameListener(Object parent, Player player, Configuration config) {
		this.parent = parent;
		this.config = config;
		this.player = player;
	}

	@Override
	public void downloaded(File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		try {
			ZipFileProxy zip = new ZipFileProxy(downloadedFile);
			for (File zipEntry : zip.listFiles()) {
				if (isTapeFile(zipEntry) || isDiskFile(zipEntry)) {
					File mediaFile = ZipEntryFileProxy.extractFromZip(
							(ZipEntryFileProxy) zipEntry, config.getSidplay2()
									.getTmpDir());
					mediaFile.deleteOnExit();
					if (fileToRun.length() == 0
							|| fileToRun.equals(zipEntry.getName())) {
						insertMedia(mediaFile);
					}
				}
			}
			downloadedFile.deleteOnExit();
			// Make it possible to choose a file from ZIP next time
			// the file chooser opens
			config.getSidplay2().setLastDirectory(
					downloadedFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void insertMedia(final File selectedFile) {
		final String command;
		if (isTapeFile(selectedFile)) {
			uiEvents.fireEvent(IInsertMedia.class, new IInsertMedia() {

				@Override
				public MediaType getMediaType() {
					return MediaType.TAPE;
				}

				@Override
				public File getSelectedMedia() {
					return selectedFile;
				}

				@Override
				public File getAutostartFile() {
					return null;
				}

				@Override
				public Object getComponent() {
					return parent;
				}
			});
			command = "LOAD\rRUN\r";
		} else if (isDiskFile(selectedFile)) {
			uiEvents.fireEvent(IInsertMedia.class, new IInsertMedia() {

				@Override
				public MediaType getMediaType() {
					return MediaType.DISK;
				}

				@Override
				public File getSelectedMedia() {
					return selectedFile;
				}

				@Override
				public File getAutostartFile() {
					return null;
				}

				@Override
				public Object getComponent() {
					return parent;
				}
			});
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			command = null;
		}
		// reset required after inserting the cartridge
		uiEvents.fireEvent(Reset.class, new Reset() {

			@Override
			public boolean switchToVideoTab() {
				return true;
			}

			@Override
			public String getCommand() {
				return command;
			}

			@Override
			public Object getComponent() {
				return parent;
			}
		});
	}

	private boolean isTapeFile(final File selectedFile) {
		return selectedFile.getName().toLowerCase().endsWith(".tap")
				|| selectedFile.getName().toLowerCase().endsWith(".t64");
	}

	private boolean isDiskFile(final File selectedFile) {
		return selectedFile.getName().toLowerCase().endsWith(".d64")
				|| selectedFile.getName().toLowerCase().endsWith(".g64");
	}

	public void setFileToRun(String valueOf) {
		fileToRun = valueOf;
	}

}