package applet.gamebase.listeners;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import libsidplay.Player;
import sidplay.ini.IniConfig;
import applet.events.IInsertMedia;
import applet.events.Reset;

public class GameListener extends ProgressListener {

	private String fileToRun;
	protected Component parent;
	protected Player player;
	protected IniConfig config;

	/**
	 * Last downloaded game file.
	 */
	public List<File> lastMedia = new ArrayList<File>();

	public GameListener(Component parent, Player player, IniConfig config) {
		this.parent = parent;
		this.config = config;
		this.player = player;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void downloaded(File downloadedFile) {
		// Make it possible to choose a file from ZIP next time
		// the file chooser opens
		config.sidplay2().setLastDirectory(downloadedFile.getAbsolutePath());
		try {
			byte[] b = new byte[1024];
			ZipFile zip = new ZipFile(downloadedFile);
			Enumeration entries = zip.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) entries.nextElement();
				long size = entry.getSize();
				if (size > 0
						&& (entry.getName().toLowerCase().endsWith(".t64") || entry
								.getName().toLowerCase().endsWith(".d64"))) {
					InputStream is = zip.getInputStream(entry);
					File mediaFile = new File(
							System.getProperty("jsidplay2.tmpdir"),
							entry.getName());
					mediaFile.deleteOnExit();
					OutputStream os = new FileOutputStream(mediaFile);
					while (is.available() > 0) {
						int len = is.read(b);
						if (len > 0) {
							os.write(b, 0, len);
						}
					}
					is.close();
					os.close();
					if (fileToRun.length() == 0
							|| fileToRun.equals(entry.getName())) {
						insertMedia(mediaFile);
					}
				}
			}
			zip.close();
			downloadedFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void insertMedia(final File selectedFile) {
		final String command;
		if (selectedFile.getName().toLowerCase().endsWith(".tap")
				|| selectedFile.getName().toLowerCase().endsWith(".t64")) {
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
				public Component getComponent() {
					return parent;
				}
			});
			command = "LOAD\rRUN\r";
		} else if (selectedFile.getName().toLowerCase().endsWith(".d64")
				|| selectedFile.getName().toLowerCase().endsWith(".g64")) {
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
				public Component getComponent() {
					return parent;
				}
			});
			command = "LOAD\"*\",8,1\rRUN\r";
		} else {
			command = null;
		}
		synchronized (lastMedia) {
			for (File file : lastMedia) {
				file.delete();
			}
			lastMedia.add(selectedFile);
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
			public Component getComponent() {
				return parent;
			}
		});
	}

	public void setFileToRun(String valueOf) {
		fileToRun = valueOf;
	}

}