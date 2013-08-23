package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.DoubleProperty;
import libsidutils.zip.ZipEntryFileProxy;
import libsidutils.zip.ZipFileProxy;
import sidplay.ConsolePlayer;
import ui.download.ProgressListener;
import ui.entities.config.Configuration;
import ui.events.IInsertMedia;
import ui.events.UIEventFactory;

public class GameListener extends ProgressListener {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	private String fileToRun;
	protected Configuration config;

	private ConsolePlayer cp;

	public GameListener(DoubleProperty progress, ConsolePlayer cp, Configuration config) {
		super(progress);
		this.cp = cp;
		this.config = config;
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

			});
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			command = null;
		}
		cp.playTune(null, command);
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