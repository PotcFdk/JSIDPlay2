package netsiddev;

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
import libsidplay.common.ChipModel;
import libsidplay.common.SIDChip;
import libsidplay.config.IFilterSection;
import netsiddev.ini.JSIDDeviceConfig;
import resid_builder.residfp.Filter6581;
import resid_builder.residfp.Filter8580;

/**
 * Documentation of the protocol is contained here: netsiddev.ad
 * 
 * @author Ken Händel
 * @author Antti S. Lankila
 * @author Wilfred Bos
 */
public class NetworkSIDDevice extends Application {
	private static final String TRAY_ICON = "jsidplay2.png";
	private static final String TRAY_TOOLTIP = "SID Network Device";
	private static final String MENU_ABOUT = "About";
	private static final String MENU_SETTINGS = "Settings...";
	private static final String MENU_EXIT = "Exit";

	private static JSIDDeviceConfig config;

	private About aboutDialog = null;
	private Settings settingsDialog = null;

	/**
	 * Gets the number of known configurations.
	 * 
	 * @return The number of known SID configurations.
	 */
	public static byte getSidCount() {
		String[] sid = config.getFilterList();
		return (byte) sid.length;
	}

	/**
	 * Return the name of the requested SID.
	 * 
	 * @param sidNum
	 *            The SID to get the name of.
	 * @return SID name string
	 */
	protected static String getSidName(int sidNum) {
		String[] sid = config.getFilterList();
		return sid[sidNum];
	}

	/**
	 * Construct the SID object suite.
	 * 
	 * @param sidNumber
	 */
	protected static SIDChip getSidConfig(int sidNumber) {
		IFilterSection iniFilter = config.getFilter(config.getFilterList()[sidNumber]);

		SIDChip sid = null;
		if (iniFilter.isReSIDFilter6581()) {
			sid = new resid_builder.resid.SID();
			((resid_builder.resid.SID) sid).setChipModel(ChipModel.MOS6581);
			((resid_builder.resid.SID) sid).getFilter6581().setFilterCurve(iniFilter.getFilter6581CurvePosition());
		} else if (iniFilter.isReSIDFilter8580()) {
			sid = new resid_builder.resid.SID();
			((resid_builder.resid.SID) sid).setChipModel(ChipModel.MOS8580);
			((resid_builder.resid.SID) sid).getFilter8580().setFilterCurve(iniFilter.getFilter8580CurvePosition());
		} else if (iniFilter.isReSIDfpFilter6581()) {
			sid = new resid_builder.residfp.SID();
			((resid_builder.residfp.SID) sid).setChipModel(ChipModel.MOS6581);
			Filter6581 filter6581 = ((resid_builder.residfp.SID) sid).getFilter6581();
			filter6581.setCurveProperties(iniFilter.getBaseresistance(), iniFilter.getOffset(),
					iniFilter.getSteepness(), iniFilter.getMinimumfetresistance());
			filter6581.setDistortionProperties(iniFilter.getAttenuation(), iniFilter.getNonlinearity(),
					iniFilter.getResonanceFactor());
			((resid_builder.residfp.SID) sid).set6581VoiceNonlinearity(iniFilter.getVoiceNonlinearity());
			filter6581.setNonLinearity(iniFilter.getVoiceNonlinearity());
		} else if (iniFilter.isReSIDfpFilter8580()) {
			sid = new resid_builder.residfp.SID();
			((resid_builder.residfp.SID) sid).setChipModel(ChipModel.MOS8580);
			Filter8580 filter8580 = ((resid_builder.residfp.SID) sid).getFilter8580();
			filter8580.setCurveProperties(iniFilter.getK(), iniFilter.getB(), 0, 0);
			filter8580.setDistortionProperties(0, 0, iniFilter.getResonanceFactor());
		}
		return sid;
	}

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
			printErrorAndExit("Sorry, System Tray is not yet supported on your platform!");
		}

		Platform.setImplicitExit(false);
		config = new JSIDDeviceConfig();
		createSystemTrayMenu();
		new Thread(() -> {
			try {
				ClientContext.listenForClients(config);
			} catch (Exception e) {
				Platform.runLater(() -> printErrorAndExit(exceptionToString(e)));
			}
		}).start();

	}

	private void createSystemTrayMenu() {
		// XXX unfortunately system tray is not directly supported by JavaFX!
		final SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().getImage(getClass().getResource(TRAY_ICON));

		final TrayIcon trayIcon = new TrayIcon(image, TRAY_TOOLTIP, popup);
		trayIcon.setImageAutoSize(true);

		MenuItem aboutItem = new MenuItem(MENU_ABOUT);
		aboutItem.addActionListener((e) -> Platform.runLater(() -> {
			try {
				if (aboutDialog == null) {
					aboutDialog = new About();
				}
				aboutDialog.requestFocus();
				if (!aboutDialog.isShowing()) {
					aboutDialog.open();
				}
			} catch (IOException e1) {
				printErrorAndExit(exceptionToString(e1));
			}
		}));
		popup.add(aboutItem);

		MenuItem settingsItem = new MenuItem(MENU_SETTINGS);
		settingsItem.addActionListener((e) -> Platform.runLater(() -> {
			try {
				if (settingsDialog == null) {
					settingsDialog = new Settings();
				}
				settingsDialog.requestFocus();
				if (!settingsDialog.isShowing()) {
					settingsDialog.open();
				}
			} catch (IOException e1) {
				printErrorAndExit(exceptionToString(e1));
			}
		}));
		popup.add(settingsItem);

		popup.addSeparator();

		MenuItem exitItem = new MenuItem(MENU_EXIT);
		exitItem.addActionListener((e) -> System.exit(0));
		popup.add(exitItem);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			printErrorAndExit(exceptionToString(e));
		}
	}

	private void printErrorAndExit(String msg) {
		Alert alert = new Alert(AlertType.ERROR,"");
		alert.getDialogPane().setHeaderText(msg);
		alert.showAndWait();
		System.exit(0);
	}

	private String exceptionToString(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}

}
