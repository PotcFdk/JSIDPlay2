package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javafx.scene.Node;
import libsidplay.MediaType;
import libsidplay.Player;
import ui.common.UIUtil;
import ui.download.ProgressListener;
import de.schlichtherle.truezip.file.TFile;

public class GameListener extends ProgressListener {

	private String fileToRun;

	private Player player;

	public GameListener(UIUtil util, Node node, Player player) {
		super(util, node);
		this.player = player;
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
					TFile dst = new TFile(player.getConfig().getSidplay2().getTmpDir(),
							zipEntry.getName());
					dst.deleteOnExit();
					TFile.cp(zipEntry, dst);
					if (isTapeFile(dst) || isDiskFile(dst) || isCRT(dst)) {
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
		player.getConfig().getSidplay2().setLastDirectory(downloadedFile.getParent());
	}

	private void insertMedia(final TFile file) {
		final String command;
		if (isTapeFile(file)) {
			player.getC64().ejectCartridge();
			player.insertMedia(file, null, MediaType.TAPE);
			command = "LOAD\rRUN\r";
			player.playTune(null, command);
		} else if (isDiskFile(file)) {
			player.getC64().ejectCartridge();
			player.insertMedia(file, null, MediaType.DISK);
			command = "LOAD\"*\",8,1\rRUN\r";
			player.playTune(null, command);
		} else {
			player.insertMedia(file, null, MediaType.CART);
			command = null;
		}
	}

	private boolean isTapeFile(final File selectedFile) {
		return selectedFile.getName().toLowerCase(Locale.ENGLISH)
				.endsWith(".tap")
				|| selectedFile.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".t64");
	}

	private boolean isDiskFile(final File selectedFile) {
		return selectedFile.getName().toLowerCase(Locale.ENGLISH)
				.endsWith(".d64")
				|| selectedFile.getName().toLowerCase(Locale.ENGLISH)
						.endsWith(".g64");
	}

	private boolean isCRT(final File selectedFile) {
		return selectedFile.getName().toLowerCase(Locale.ENGLISH)
				.endsWith(".crt");
	}

	public void setFileToRun(String valueOf) {
		fileToRun = valueOf;
	}

}