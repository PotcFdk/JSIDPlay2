package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import javafx.beans.property.DoubleProperty;
import sidplay.ConsolePlayer;
import sidplay.consoleplayer.MediaType;
import ui.download.ProgressListener;
import ui.entities.config.Configuration;
import de.schlichtherle.truezip.file.TFile;

public class GameListener extends ProgressListener {

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
			if (zipEntry.isFile()) {
				try {
					TFile dst = new TFile(config.getSidplay2().getTmpDir(),
							zipEntry.getName());
					dst.deleteOnExit();
					TFile.cp(zipEntry, dst);
					if (isTapeFile(dst) || isDiskFile(dst)) {
						if (fileToRun.length() == 0
								|| fileToRun.equals(dst.getName())) {
							insertMedia(dst);
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		downloadedFile.deleteOnExit();
		// Make it possible to choose a file from ZIP next time
		// the file chooser opens
		config.getSidplay2().setLastDirectory(downloadedFile.getParent());
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