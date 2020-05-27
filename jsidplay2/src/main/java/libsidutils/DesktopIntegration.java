package libsidutils;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.function.Consumer;

public class DesktopIntegration {

	/**
	 * Open a browser URL (and run in a separate daemon thread).
	 *
	 * @param link link to open in the default browser
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
	 * @param file file to open
	 */
	public static void open(File file) {
		openInSeparateThread(file, desktop -> {
			try {
				if (desktop.isSupported(Desktop.Action.OPEN)) {
					desktop.open(file);
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
