package libsidutils;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

public class WebUtils {

	/**
	 * Open a browser URL (and run in a separate daemon thread).
	 * 
	 * @param link
	 *            link to open in the default browser
	 */
	public static void browse(String link) {
		Thread thread = new Thread(() -> {
			// As an application we open the default browser
				if (Desktop.isDesktopSupported()) {
					Desktop desktop = Desktop.getDesktop();
					if (desktop.isSupported(Desktop.Action.BROWSE)) {
						try {
							desktop.browse(new URL(link).toURI());
						} catch (final IOException ioe) {
							ioe.printStackTrace();
						} catch (final URISyntaxException urie) {
							urie.printStackTrace();
						}
					}
				} else {
					System.err.println("Awt Desktop is not supported!");
				}
			});
		thread.setDaemon(true);
		thread.start();
	}
}
