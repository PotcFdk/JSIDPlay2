package applet.gamebase.listeners;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import applet.gamebase.GameBase;

public class ScreenShotListener extends ProgressListener {

	/**
	 * 
	 */
	private final GameBase gameBase;

	public ScreenShotListener(GameBase gameBase) {
		this.gameBase = gameBase;
	}

	@Override
	public void downloadStep(int pct) {
		gameBase.clearPicture();
		super.downloadStep(pct);
	}

	@Override
	public void downloaded(File downloadedFile) {
		if (downloadedFile == null) {
			return;
		}
		try {
			synchronized (this.gameBase.lastScreenshot) {
				for (File file : this.gameBase.lastScreenshot) {
					file.delete();
				}
				this.gameBase.lastScreenshot.add(downloadedFile);
			}
			final URL resource = downloadedFile.toURI().toURL();
			this.gameBase.picture.setComposerImage(new ImageIcon(resource)
					.getImage());
			this.gameBase.screenshot.repaint();
			this.gameBase.repaint();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
	}
}