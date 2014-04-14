package ui.gamebase.listeners;

import java.io.File;
import java.io.IOException;

import javafx.scene.Node;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import sidplay.ConsolePlayer;
import ui.common.UIUtil;
import ui.download.ProgressListener;

public class MusicListener extends ProgressListener {

	private ConsolePlayer cp;

	public MusicListener(UIUtil util, Node node, ConsolePlayer consolePlayer) {
		super(util, node);
		this.cp = consolePlayer;
	}

	@Override
	public void downloaded(final File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		downloadedFile.deleteOnExit();
		// play tune
		try {
			cp.playTune(SidTune.load(downloadedFile), null);
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}
}