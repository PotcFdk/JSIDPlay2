package libsidutils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

import de.schlichtherle.truezip.file.TFile;
import libsidplay.config.IConfig;

public class DesktopIntegration {

	/**
	 * Open a browser URL (and run in a separate daemon thread).
	 * 
	 * @param link
	 *            link to open in the default browser
	 */
	public static void browse(String link) {
		openInSeparateThread(link, desktop -> {
			try {
				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					desktop.browse(new URL(link).toURI());
				} else {
					System.err.println("Awt Desktop action BROWSE is not supported!");
				}
			} catch (final IOException | URISyntaxException e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Open a file (and run in a separate daemon thread).
	 * 
	 * @param link
	 *            file to open
	 */
	public static void open(IConfig config, File file) {
		String tmpDir = config.getSidplay2Section().getTmpDir();
		File dst = new File(tmpDir, file.getName());
		if (!dst.exists()) {
			try {
				TFile.cp(file, dst);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		dst.deleteOnExit();

		openInSeparateThread(dst, desktop -> {
			try {
				if (desktop.isSupported(Desktop.Action.OPEN)) {
					desktop.open(dst);
				} else {
					System.err.println("Awt Desktop action OPEN is not supported!");
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private static <T> void openInSeparateThread(T toOpen, Consumer<Desktop> consumer) {
		Thread thread = new Thread(() -> {
			if (Desktop.isDesktopSupported()) {
				consumer.accept(Desktop.getDesktop());
			} else {
				System.err.println("Awt Desktop is not supported!");
			}
		});
		thread.setDaemon(true);
		thread.start();
	}
}
