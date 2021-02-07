package builder.sidblaster;

import static builder.sidblaster.Command.GET_SID_TYPE;
import static builder.sidblaster.Command.SET_SID_TYPE;
import static builder.sidblaster.SIDBlasterBuilder.getSerialNumbers;
import static builder.sidblaster.SIDBlasterBuilder.getSidType;
import static builder.sidblaster.SIDBlasterBuilder.setSidType;
import static builder.sidblaster.SIDType.SIDTYPE_NONE;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidutils.DebugUtil;
import sidplay.ini.IniConfig;
import ui.JSidPlay2Main;

@Parameters(resourceBundle = "builder.sidblaster.SidBuilderTool")
public class SIDBlasterTool {

	private static Properties properties = new Properties();
	static {
		properties.setProperty("version", "(beta)");
		try {
			URL resource = JSidPlay2Main.class.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			properties.load(resource.openConnection().getInputStream());
		} catch (NullPointerException | IOException e) {
		}
	}

	static {
		DebugUtil.init();
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--command", "-c" }, descriptionKey = "COMMAND", order = 1)
	private Command command;

	@Parameter(names = { "--deviceId", "-d" }, descriptionKey = "DEVICE_ID", order = 2)
	private int deviceId;

	@Parameter(names = { "--sidType", "-t" }, descriptionKey = "SID_TYPE", order = 3)
	private SIDType sidType = SIDTYPE_NONE;

	private IniConfig config = new IniConfig(false, null);

	private void create(String[] args) throws Exception {
		System.out.println(
				"===============================================================================================");
		System.out.println("SIDBlaster tool is a tool to read or write settings of your SIDBlaster USB device");
		System.out.println("Credits:");
		System.out.println(
				"SIDBlaster tool by Andreas Schumm (https://github.com/gh0stless/SIDBlaster-USB-Tic-Tac-Edition)");
		System.out.println("Java Version by Ken Händel");
		System.out.println(
				"===============================================================================================");

		JCommander commander = JCommander.newBuilder().addObject(this)
				.programName("sidblastertool-" + properties.getProperty("version") + ".exe").build();
		commander.parse(Arrays.asList(args).stream().map(arg -> arg == null ? "" : arg).toArray(String[]::new));
		if (command == null || help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		// trigger read library
		new SIDBlasterBuilder(null, config, null);

		final String[] serialNumbers = getSerialNumbers();
		if (command == GET_SID_TYPE) {
			if (deviceId < serialNumbers.length) {
				System.out.printf("GET %d: serial=%s, type= %s\n", deviceId, serialNumbers[deviceId],
						getSidType(deviceId));
			} else {
				System.out.println("No SIDBlaster devices detected!");
			}
		} else if (command == SET_SID_TYPE) {

			if (deviceId < serialNumbers.length) {
				System.out.printf("Do you really want to set SID type of device %d: serial=%s to %s (y/N)\n", deviceId,
						serialNumbers[deviceId], sidType);
				while (System.in.available() == 0) {
					Thread.sleep(500);
				}
				final int key = System.in.read();
				switch (key) {
				case 'y':
				case 'Y':
					int status = setSidType(deviceId, sidType);
					System.out.printf("SET %d: serial=%s, type=%s (rc=%d)\n", deviceId, serialNumbers[deviceId],
							sidType, status);
					System.out.println("Done! Please exit tool and reconnect SIDBlaster!!!");
					System.out.println("Press <enter> to exit!");
					System.in.read();
					System.exit(0);
					break;
				case 'n':
				case 'N':
				default:
					System.out.println("Aborted!");
					break;
				}
			} else {
				System.out.println("No SIDBlaster devices detected!");
			}
		} else {
			// DEFAULT: command == INFO

			if (serialNumbers.length > 0) {
				System.out.println("SIDBlaster devices:");
				for (byte i = 0; i < serialNumbers.length; i++) {
					System.out.printf("%s = %s\n", serialNumbers[i], getSidType(i));
				}
			} else {
				System.out.println("No SIDBlaster devices detected!");
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new SIDBlasterTool().create(args);
	}

}
