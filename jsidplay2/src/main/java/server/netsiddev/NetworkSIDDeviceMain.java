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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;

/**
 * Documentation of the protocol is contained here: netsiddev.ad
 * 
 * @author Ken Händel
 * @author Antti S. Lankila
 * @author Wilfred Bos
 */
public class NetworkSIDDeviceMain extends Application {
	private static final String TRAY_ICON = "jsidplay2.png";
	private static final String TRAY_TOOLTIP = "SID Network Device";
	private static final String MENU_ABOUT = "About";
	private static final String MENU_SETTINGS = "Settings...";
	private static final String MENU_EXIT = "Exit";

	private NetworkSIDDevice networkSIDDeviceHeadless = new NetworkSIDDevice() {
		protected void printErrorAndExit(Exception e) {
			Alert alert = new Alert(AlertType.ERROR, "");
			alert.getDialogPane().setHeaderText(exceptionToString(e));
			alert.showAndWait();
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
	 * @param args
	 *            command line arguments
	 */
	public static final void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		if (!SystemTray.isSupported()) {
			networkSIDDeviceHeadless
					.printErrorAndExit(new Exception("Sorry, System Tray is not yet supported on your platform!"));
		}

		Platform.setImplicitExit(false);
		createSystemTrayMenu();
		networkSIDDeviceHeadless.start(false);
	}

	private void createSystemTrayMenu() {
		// XXX unfortunately system tray is not directly supported by JavaFX!
		final SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource(TRAY_ICON));

		final TrayIcon trayIcon = new TrayIcon(image, TRAY_TOOLTIP, popup);
		trayIcon.setImageAutoSize(true);

		MenuItem aboutItem = new MenuItem(MENU_ABOUT);
		aboutItem.addActionListener(e -> Platform.runLater(() -> {
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
		settingsItem.addActionListener(e -> Platform.runLater(() -> {
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
