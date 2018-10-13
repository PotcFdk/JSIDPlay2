package server.netsiddev;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Documentation of the protocol is contained here: netsiddev.ad
 * 
 * @author Ken Händel
 * @author Antti S. Lankila
 * @author Wilfred Bos
 */
public class NetworkSIDDeviceMain {
	private static final String TRAY_ICON = "jsidplay2.png";
	private static final String TRAY_TOOLTIP = "SID Network Device";
	private static final String MENU_ABOUT = "About";
	private static final String MENU_SETTINGS = "Settings...";
	private static final String MENU_EXIT = "Exit";

	private NetworkSIDDevice networkSIDDeviceHeadless = new NetworkSIDDevice() {
		protected void printErrorAndExit(Exception e) {
			JOptionPane.showMessageDialog(null, exceptionToString(e), "Error", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}

		private String exceptionToString(Exception e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			return sw.toString();
		}

	};

	private About aboutDialog = null;
	private Settings settingsDialog = null;

	/**
	 * Main method. Create an application frame and start emulation.
	 * 
	 * @param args command line arguments
	 * @throws Exception
	 */
	public static final void main(final String[] args) throws Exception {
		new NetworkSIDDeviceMain().start();
	}

	public void start() throws Exception {
		if (!SystemTray.isSupported()) {
			networkSIDDeviceHeadless
					.printErrorAndExit(new Exception("Sorry, System Tray is not yet supported on your platform!"));
		}

		createSystemTrayMenu();
		networkSIDDeviceHeadless.start(false);
	}

	private void createSystemTrayMenu() {
		final SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource(TRAY_ICON));

		final TrayIcon trayIcon = new TrayIcon(image, TRAY_TOOLTIP, popup);
		trayIcon.setImageAutoSize(true);

		MenuItem aboutItem = new MenuItem(MENU_ABOUT);
		aboutItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			try {
				if (aboutDialog == null) {
					aboutDialog = new About();
				}
				aboutDialog.requestFocus();
				if (!aboutDialog.isShowing()) {
					aboutDialog.open();
				}
			} catch (IOException e1) {
				networkSIDDeviceHeadless.printErrorAndExit(e1);
			}
		}));
		popup.add(aboutItem);

		MenuItem settingsItem = new MenuItem(MENU_SETTINGS);
		settingsItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			try {
				if (settingsDialog == null) {
					settingsDialog = new Settings();
				}
				settingsDialog.requestFocus();
				if (!settingsDialog.isShowing()) {
					settingsDialog.open();
				}
			} catch (IOException e1) {
				networkSIDDeviceHeadless.printErrorAndExit(e1);
			}
		}));
		popup.add(settingsItem);

		popup.addSeparator();

		MenuItem exitItem = new MenuItem(MENU_EXIT);
		exitItem.addActionListener(e -> System.exit(0));
		popup.add(exitItem);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			networkSIDDeviceHeadless.printErrorAndExit(e);
		}
	}

}
