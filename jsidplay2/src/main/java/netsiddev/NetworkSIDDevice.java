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
import javafx.stage.Stage;
import netsiddev.ini.JSIDDeviceConfig;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.SID;
import sidplay.ini.intf.IFilterSection;

/**
 * JSIDPlay2 SID-emulation integration protocol. It is possible to have
 * JSIDPlay2 take over the duty of the SID playback for a C64 emulator/player.
 * Every jsidplay2 instance tries to open port 6581 where they will listen to
 * connections that describe SID activity with the following protocol.
 * <p>
 * GENERAL OVERVIEW
 * <p>
 * Version 2 of the protocol is structured as a request-response protocol:
 * <ul>
 * <li>Requests are variable length, with minimum packet size 4 bytes. There are
 *   3 fields and an amorphous data blob:
 *   <ul>
 *     <li>8-bit unsigned as command.
 *     <li>8-bit unsigned as SID number.
 *     <li>16-bit unsigned as length of data attached to header in bytes.
 *     <li>Data (if any)
 *   </ul>
 * <li>All commands are ACKed with a response packet that takes one of the following
 *   forms:
 *   <ul>
 *   <li>OK means that the commands were accepted by server and can be discarded
 *     by client. No data will be appended to response.
 *   <li>BUSY means that no part of the current command was accepted due to filled
 *     queue condition, and that client should wait and retry it later.
 *     1 millisecond could be a suitable delay before retry. Queue length is
 *     limited both by number of events and maximum time drift between playback
 *     clock and client clock. No data will be attended to response.
 *   <li>READ: successful read operation, one byte value follows that is the
 *       value read from SID.
 *   <li>VERSION: response to VERSION operation. Version number will be appended
 *       to response.
 *   <li>COUNT: number of SIDs supported by network device.
 *   <li>INFO: info packet, which contains model code and zero-padded
 *       20-byte UTF-8 encoded string representing model name.
 *   </ul>
 * </ul>
 * Maximum packet length is 64k + header length. It is suggested that only
 * short packets are transmitted, in the order of 1k and containing no more
 * than about 1 ms worth of events. Otherwise the client-server desync brings
 * jitter that may have unpleasant consequences. At the limit it's possible to
 * simply send a fixed header that describes a single write with each packet,
 * but this is probably measurably less efficient.
 * <p>
 * COMMAND OVERVIEW
 * <p>
 * Structure of data is specific to command. Some commands require data,
 * others will not use data even if such was provided. Some commands
 * require specific lengths for the data packets. If data length is not
 * correct, results are undefined.
 * <p>
 * Known commands are identified by small integers, starting from 0:
 * <ul>
 * <li>FLUSH (0): destroy queued data on all SIDs, and cease audio production.
 *   <ul>
 *     <li>sid number is ignored.
 *     <li>data packet must be 0 length.
 *     <li>should probably be followed by RESET (SID is in unpredictable state).
 *     <li>always returns OK
 *   </ul>
 *
 * <li>TRY_SET_SID_COUNT (1): set number of SID devices available for writing
 *   <ul>
 *     <li>sid number equals the count of SIDs wanted.
 *     <li>data packet must be 0 length.
 *     <li>returns BUSY until audio quiescent, otherwise OK.
 *   </ul>
 *
 * <li>MUTE (2): mute/unmute a voice on specified SID
 *   <ul>
 *     <li>data packet must contain two 8-bit unsigned bytes:
 *     <ul>
 *       <li>the voice number from 0 to 2
 *       <li>0 or 1 to disable/enable voice
 *       <li>this command bypasses buffer and takes immediate effect.
 *     </ul>
 *     <li>always returns OK
 *   </ul>
 *
 * <li>TRY_RESET (3): reset all SIDs, setting volume to provided value.
 *   <ul>
 *     <li>data packet must be a 8-bit unsigned value which is written to volume register.
 *     <li>returns BUSY until audio quiescent, otherwise OK.
 *   </ul>
 *
 * <li>TRY_DELAY (4): inform emulation that no events have occurred for a given count of cycles
 *   <ul>
 *     <li>data packet must be 16-bit unsigned value interpreted as delay in C64 clocks. 0 is not allowed.
 *     <li>allows audio generation in absence of other activity.
 *     <li>returns BUSY if there is already enough data for playback, otherwise OK.
 *   </ul>
 *
 * <li>TRY_WRITE (5): try to queue a number of write-to-sid events.
 *   <ul>
 *     <li>data packet must be 4*N bytes long, repeating this structure:
 *     <ul>
 *       <li>16-bit unsigned value interpreted as delay before the write in C64 clocks.
 *       <li>8-bit unsigned SID register number from 0x00 to 0x1f.
 *       <li>8-bit unsigned data value to write
 *     </ul>
 *     <li>returns BUSY if there is already enough data for playback, otherwise OK.
 *   </ul>
 *   
 * <li>TRY_READ (6): reads SID chip register.
 *   <ul>
 *     <li>data packet must be a 4n+3 bytes long, where n >= 0. The protocol
 *     used for the first n packets is the same as the TRY_WRITE protocol,
 *     returning potentially BUSY if the delay implied by the READ, or the WRITEs
 *     can not yet be buffered.
 *     <li>Read packet structure trails the write packet structure:
 *     <ul>
 *       <li>16-bit unsigned value interpreted as delay before the read in C64 clocks.
 *       <li>8-bit unsigned SID register number from 0x00 to 0x1f.
 *     </ul>
 *     <li>returns BUSY if there is already enough data for playback, otherwise
 *     READ and a data byte, which is the read value from SID.
 *   </ul>
 *   
 * <li>GET_VERSION (7): returns the version of the SID Network protocol.
 *   <ul>
 *     <li>sid number is ignored.
 *     <li>data packet must be 0 length.
 *     <li>returns 2 bytes: VERSION and a data byte, which is the version of the SID Network protocol.
 *   </ul>
 * 
 * <li>SET_SAMPLING (8): set the resampling method for all SID devices.
 *   <ul>
 *     <li>sid number is ignored.
 *     <li>data packet is 1 byte long and contains:
 *     <ul>
 *       <li>0 for pure decimator (low quality)
 *       <li>1 for low-pass filtered decimator (high quality).
 *     </ul>
 *     <li>returns BUSY until audio quiescent, otherwise OK.
 *   </ul>
 *   
 * <li>SET_CLOCKING (9): set the clock source speed for all SID devices.
 *   <ul>
 *     <li>sid number is ignored.
 *     <li>data packet is 1 byte long and contains:
 *     <ul>
 *       <li>0 for PAL
 *       <li>1 for NTSC
 *     </ul>
 *   </ul>
 *   <li>returns BUSY until audio quiescent, otherwise OK.
 *   
 * <li>GET_CONFIG_COUNT (10): Query number of SID configurations supported by server.
 *   <li>sid number is ignored.</li>
 *   <li>data packet is ignored and should be 0 length.
 *   <ul>
 *     <li>always returns COUNT and a 8-bit unsigned value that is 1 larger than the maximum valid configuration.
 *   </ul>
 *
 * <li>GET_CONFIG_INFO (11): query the name and model of the SID configuration.
 *   <ul>
 *     <li>data packet is ignored and should be 0 length.
 *     <li>returns INFO and 8-bit unsigned-value and a string in ISO-8859-1 encoding with a maximum of 255 characters excluding a null terminated byte
 *     <ul>
 *       <li>INFO code
 *       <li>Model: 0 = 6581, 1 = 8580
 *       <li>Model name (max. 255 chars + 1 null terminated byte)
 *     </ul>
 *   </ul>
 *
 * <li>SET_SID_POSITION (12): set sid position on the audio mix
 *   <ul>
 *     <li>data packet is 1 byte long and contains:
 *     <ul>
 *       <li> -100 to 0: audio is panned to left
 *       <li> 0 to 100: audio is panned to right
 *     </ul>
 *     <li>always returns OK.
 *   </ul>
 *   
 * <li>SET_SID_LEVEL (13): set SID level adjustment in dB
 *   <ul>
 *     <li>data packet is 1 byte long and contains:
 *     <ul>
 *       <li>8-bit signed dB adjustment in cB (centibels), 0 means no adjustment
 *     </ul>
 *     <li>always returns OK.
 *   </ul>
 *
 * <li>SET_SID_MODEL (14):
 *   <ul>
 *     <li>data packet is 1 byte long and contains:
 *     <ul>
 *       <li>8-bit unsigned value between 0 <= value <= max_config-1
 *     </ul>
 *     <li>always returns OK.
 *   </ul>
 *
 * </ul>
 * 
 * VERSION HISTORY
 * <ul>
 * <li>Version 1 contains all commands up to 7 (VERSION). There were 8 SID devices
 * where bit 0 gave 6581/8580, bit 1 PAL/NTSC and bit 2 RESAMPLE/DECIMATE mode of operation.
 * <li>Version 2 contains commands SAMPLING and CLOCKING. There are 4 different SID
 * devices, 3x 6581 and 1x 8580. The commands SAMPLING and CLOCKING can be used to set
 * particular SID kind.
 * </ul>
 * 
 * NOTES
 * <p>
 * The delay values do not contain the time taken to write the value to SID chip, and
 * a delay length of 0 between writes is impossible to achieve with a true C64 system,
 * although this emulator will accept it and execute several writes on the same clock.
 * <p>
 * At start of connection, the SID starts from RESET state with volume=0 and empty buffer.
 * <p>
 * Suitable packet size for TRY_WRITE is about 20 ms long. If TRY_WRITE returns BUSY, then
 * client should wait about 20 ms (same as the play length of one packet) before retry.
 * <p>
 * Future expansion:
 * <ul>
 * <li>stereo SID support
 * <li>select filter type in dialog
 * <li>route chips to left, right or mono.
 * <li>implement protocol via UDP
 * <li>combine read and write in one data packet
 * </ul>
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
	 * @param sidNum The SID to get the name of.
	 * @return SID name string
	 */
	protected static String getSidName(int sidNum) {
		String[] sid = config.getFilterList();
		return sid[sidNum];
	}
	
	/**
	 * Construct the SID object suite.
	 * TODO: we should read these things off configuration file.
	 * 
	 * @param sidNumber
	 */
	protected static SID getSidConfig(int sidNumber) {
		SID sid = new SID();
		IFilterSection iniFilter = config.getFilter(config.getFilterList()[sidNumber]);

		if (iniFilter.getFilter8580CurvePosition() == 0) {
			sid.setChipModel(ChipModel.MOS6581);
			sid.getFilter6581().setFilterCurve(iniFilter.getFilter6581CurvePosition());
		} else {
			sid.setChipModel(ChipModel.MOS8580);
			sid.getFilter8580().setFilterCurve(iniFilter.getFilter8580CurvePosition());
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
		if (SystemTray.isSupported()) {
			Platform.setImplicitExit(false);
			config = new JSIDDeviceConfig();
			createSystemTrayMenu();
			new Thread(() -> {
				try {
					ClientContext.listenForClients(config);
				} catch (Exception e) {
					Platform.runLater(() -> {
						StringWriter sw = new StringWriter();
						e.printStackTrace(new PrintWriter(sw));
						printErrorAndExit(sw.toString());
					});
				}
			}).start();
		} else {
			printErrorAndExit("Sorry, System Tray is not yet supported on your platform!");
		}

	}

	private void createSystemTrayMenu() {
		// XXX unfortunately system tray is not directly supported by JavaFX!
		final SystemTray tray = SystemTray.getSystemTray();

		PopupMenu popup = new PopupMenu();
		Image image = Toolkit.getDefaultToolkit().getImage(
				getClass().getResource(TRAY_ICON));

		final TrayIcon trayIcon = new TrayIcon(image, TRAY_TOOLTIP, popup);
		trayIcon.setImageAutoSize(true);

		MenuItem aboutItem = new MenuItem(MENU_ABOUT);
		aboutItem.addActionListener((e) -> {
			Platform.runLater(() -> {
				About about = new About();
				try {
					about.open();
				} catch (IOException e1) {
				}
			});
		});
		popup.add(aboutItem);

		MenuItem settingsItem = new MenuItem(MENU_SETTINGS);
		settingsItem.addActionListener((e) -> {
			Platform.runLater(() -> {
				Settings settings = new Settings();
				try {
					settings.open();
				} catch (IOException e1) {
				}
			});
		});
		popup.add(settingsItem);

		popup.addSeparator();

		MenuItem exitItem = new MenuItem(MENU_EXIT);
		exitItem.addActionListener((e) -> {
			Platform.runLater(() -> {
				System.exit(0);
			});
		});
		popup.add(exitItem);

		try {
			tray.add(trayIcon);
		} catch (AWTException e) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			printErrorAndExit(sw.toString());
		}
	}

	private void printErrorAndExit(String msg) {
		Alert alert = new Alert();
		alert.setMessage(msg);
		alert.setWait(true);
		try {
			alert.open();
		} catch (IOException e1) {
		}
		System.exit(0);
	}

}
