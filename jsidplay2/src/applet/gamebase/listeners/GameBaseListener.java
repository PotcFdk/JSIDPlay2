package applet.gamebase.listeners;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import applet.entities.config.Configuration;
import applet.gamebase.GameBase;
import applet.gamebase.GameBasePage;

public class GameBaseListener extends ProgressListener {

	private final GameBase gameBase;
	private Configuration config;

	public GameBaseListener(GameBase gameBase, Configuration config) {
		this.gameBase = gameBase;
		this.config = config;
	}

	@Override
	public void downloaded(File downloadedFile) {
		if (downloadedFile == null) {
			this.gameBase.enableGameBase.setEnabled(true);
			return;
		}
		try {
			File output = null;
			byte[] b = new byte[1024];
			ZipFile zip = new ZipFile(downloadedFile);
			@SuppressWarnings("rawtypes")
			Enumeration entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				long size = entry.getSize();
				output = new File(config.getSidplay2().getTmpDir(),
						entry.getName());
				if (output.isDirectory()) {
					output.mkdirs();
				} else {
					if (size > 0) {
						output.delete();
						output.getParentFile().mkdirs();
						InputStream is = zip.getInputStream(entry);
						OutputStream os = new FileOutputStream(output);
						int len;
						do {
							len = is.read(b);
							if (len > 0) {
								os.write(b, 0, len);
							}
						} while (len != -1);
						is.close();
						os.close();
					}
				}
			}
			zip.close();
			downloadedFile.delete();
			if (output != null) {
				String nameNoExt = output.getName();
				if (nameNoExt.lastIndexOf('.') != -1) {
					nameNoExt = output.getName().substring(0,
							nameNoExt.lastIndexOf('.'));
				}
				String dbName = output.getParent() + "/" + nameNoExt;
				this.gameBase.connect(dbName);
				this.gameBase.setLettersEnabled(true);
				GameBasePage page = this.gameBase.pages.get(0);
				page.setRows(this.gameBase.getGamesService().select(
						GameBase.ALL_LETTERS.charAt(0)));
				this.gameBase.getLetter().setSelectedIndex(0);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			this.gameBase.enableGameBase.setEnabled(true);
		}
	}
}