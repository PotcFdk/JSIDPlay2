package server.netsiddev;

import java.awt.AWTException;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * Documentation of the protocol is contained here: netsiddev.ad
 *
 * @author Ken Händel
 * @author Antti S. Lankila
 * @author Wilfred Bos
 */
public class NetworkSIDDeviceMain {
	private static final String TRAY_ICON = "jsiddevice.png";
	private static final String TRAY_TOOLTIP = "SID Network Device\nClients connected: %d";
	private static final String MENU_ABOUT = "About";
	private static final String MENU_SETTINGS = "Settings...";
	private static final String RESET_CONNECTIONS = "Reset connections";
	private static final String MENU_EXIT = "Exit";

	private NetworkSIDDevice networkSIDDeviceHeadless = new NetworkSIDDevice() {
		@Override
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
	
	private float calculateScalingRatio(int dpi) {
		final String javaVersion = System.getProperty("java.runtime.version");

		if (javaVersion.startsWith("1.8")) {
			if (dpi <= 96) {
				return 1.0f;
			} else if (dpi <= 120) {
				return 1.25f;
			} else if (dpi <= 144) {
				return 1.5f;
			} else if (dpi <= 192) {
				return 2.0f;
			} else if (dpi <= 240) {
				return 2.5f;
			} else if (dpi <= 288) {
				return 3.0f;
			} else if (dpi <= 384) {
				return 4.0f;
			} else if (dpi <= 480) {
				return 5.0f;
			}
		}
		return 1.0f;
	}
	
	private void setGlobalFont(Font f) {
		final Enumeration<Object> keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements()) {
			Object key = keys.nextElement();
			Object value = UIManager.get(key);
			if (value instanceof javax.swing.plaf.FontUIResource)
				UIManager.put(key, f);
		}
	}

	private void createSystemTrayMenu() {
		final int dpi = java.awt.Toolkit.getDefaultToolkit().getScreenResolution();
		final Font defaultFont = Font.decode(null); 
		final float adjustmentRatio = calculateScalingRatio(dpi);
		float newFontSize = defaultFont.getSize() * adjustmentRatio ; 
		final Font derivedFont = defaultFont.deriveFont(newFontSize);
	
		final SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource(TRAY_ICON));

		final TrayIcon trayIcon = new TrayIcon(image, getToolTip(), popup);
		trayIcon.setImageAutoSize(true);
		
		setGlobalFont(derivedFont);

		MenuItem aboutItem = new MenuItem(MENU_ABOUT);
		aboutItem.setFont(derivedFont);
		aboutItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			if (aboutDialog == null) {
				aboutDialog = new About();
			}
			aboutDialog.requestFocus();
			if (!aboutDialog.isShowing()) {
				aboutDialog.open();
			}
		}));
		popup.add(aboutItem);

		MenuItem settingsItem = new MenuItem(MENU_SETTINGS);
		settingsItem.setFont(derivedFont);
		settingsItem.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			if (settingsDialog == null) {
				settingsDialog = new Settings();
			}
			settingsDialog.requestFocus();
			if (!settingsDialog.isShowing()) {
				settingsDialog.open();
			}
		}));
		popup.add(settingsItem);

		popup.addSeparator();

		MenuItem resetConnections = new MenuItem(RESET_CONNECTIONS);
		resetConnections.setFont(derivedFont);
		resetConnections.addActionListener(e -> SwingUtilities.invokeLater(() -> {
			ClientContext.applyConnectionConfigChanges();
		}));
		popup.add(resetConnections);

		popup.addSeparator();

		MenuItem exitItem = new MenuItem(MENU_EXIT);
		exitItem.setFont(derivedFont);
		exitItem.addActionListener(e -> System.exit(0));
		popup.add(exitItem);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			networkSIDDeviceHeadless.printErrorAndExit(e);
		}

		new Timer(1000, event -> {
			trayIcon.setToolTip(getToolTip());
			String recognizedTunes = ClientContext.getRecognizedTunes();
			if (!recognizedTunes.trim().isEmpty()) {
				System.out.println(recognizedTunes);
				trayIcon.displayMessage("WhatsSID?", recognizedTunes, TrayIcon.MessageType.NONE);
			}
		}).start();
	}

	private String getToolTip() {
		String toolTipText = "";
		final int clientsConnected = ClientContext.getClientsConnectedCount();

		if (clientsConnected > 0) {
			toolTipText = ClientContext.getTuneHeaders().stream().filter(Objects::nonNull)
					.map(header -> Arrays.asList(header.getName(), header.getAuthor(), header.getReleased()).stream()
							.collect(Collectors.joining(", ")))
					.collect(Collectors.joining("\n"));
		}
		return toolTipText.isEmpty() ? String.format(TRAY_TOOLTIP, clientsConnected) : toolTipText;
	}
}
