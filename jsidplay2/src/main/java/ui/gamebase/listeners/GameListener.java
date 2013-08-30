package ui.gamebase.listeners;

import java.io.File;

import javafx.beans.property.DoubleProperty;
import sidplay.ConsolePlayer;
import sidplay.ConsolePlayer.MediaType;
import ui.download.ProgressListener;
import ui.entities.config.Configuration;
import ui.events.UIEventFactory;
import de.schlichtherle.truezip.file.TFile;

public class GameListener extends ProgressListener {
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();

	private String fileToRun;
	protected Configuration config;

	private ConsolePlayer cp;

	public GameListener(DoubleProperty progress, ConsolePlayer cp,
			Configuration config) {
		super(progress);
		this.cp = cp;
		this.config = config;
	}

	@Override
	public void downloaded(File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		TFile zip = new TFile(downloadedFile);
		for (TFile zipEntry : zip.listFiles()) {
			if (isTapeFile(zipEntry) || isDiskFile(zipEntry)) {
				zipEntry.deleteOnExit();
				if (fileToRun.length() == 0
						|| fileToRun.equals(zipEntry.getName())) {
					insertMedia(zipEntry);
				}
			}
		}
		downloadedFile.deleteOnExit();
		// Make it possible to choose a file from ZIP next time
		// the file chooser opens
		config.getSidplay2().setLastDirectory(downloadedFile.getAbsolutePath());
	}

	private void insertMedia(final TFile file) {
		final String command;
		if (isTapeFile(file)) {
			cp.insertMedia(file, null, MediaType.TAPE);
			command = "LOAD\rRUN\r";
		} else if (isDiskFile(file)) {
			cp.insertMedia(file, null, MediaType.DISK);
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