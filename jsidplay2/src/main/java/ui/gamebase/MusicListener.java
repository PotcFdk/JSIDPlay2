package ui.gamebase;

import java.io.File;
import java.io.IOException;

import javafx.scene.Node;
import libsidplay.Player;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.common.UIUtil;
import ui.download.ProgressListener;

public class MusicListener extends ProgressListener {

	private Player player;

	public MusicListener(UIUtil util, Node node, Player player) {
		super(util, node);
		this.player = player;
	}

	@Override
	public void downloaded(final File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		downloadedFile.deleteOnExit();
		// play tune
		try {
			player.play(SidTune.load(downloadedFile));
		} catch (IOException | SidTuneError e) {
			e.printStackTrace();
		}
	}
}