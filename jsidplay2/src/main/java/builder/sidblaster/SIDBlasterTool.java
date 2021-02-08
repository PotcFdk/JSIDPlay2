package builder.sidblaster;

import static builder.sidblaster.SIDBlasterBuilder.getSerialNumbers;
import static builder.sidblaster.SIDBlasterBuilder.getSidType;
import static builder.sidblaster.SIDBlasterBuilder.setSidType;
import static builder.sidblaster.SIDType.SIDTYPE_NONE;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidutils.DebugUtil;
import sidplay.ini.IniConfig;
import ui.JSidPlay2Main;

@Parameters(resourceBundle = "builder.sidblaster.SidBuilderTool")
public class SIDBlasterTool {

	static {
		DebugUtil.init();
	}

	private static Properties properties = new Properties();
	static {
		properties.setProperty("version", "(beta)");
		try {
			URL resource = JSidPlay2Main.class.getResource("/META-INF/maven/jsidplay2/jsidplay2/pom.properties");
			properties.load(resource.openConnection().getInputStream());
		} catch (NullPointerException | IOException e) {
		}
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

	private String[] serialNumbers;

	private void create(String[] args) throws Exception {
		JCommander commander = JCommander.newBuilder().addObject(this)
				.programName("sidblastertool-" + properties.getProperty("version") + ".exe").build();
		commander.parse(args);

		System.out.println(credits());

		if (command == null || help) {
			commander.usage();
			exit(0);
		}

		// trigger read library
		new SIDBlasterBuilder(null, config, null);

		serialNumbers = getSerialNumbers();

		if (serialNumbers.length == 0) {
			System.out.println("No SIDBlaster devices detected!");
			exit(1);
		}
		if (deviceId >= serialNumbers.length) {
			System.out.printf("Illegal parameter value: deviceId=%d!\n", deviceId);
			System.out.printf("Possible value range: 0..%d\n", serialNumbers.length - 1);
			exit(1);
		}

		switch (command) {
		case GET_SID_TYPE:
			printCommand(deviceId, getSidType(deviceId));
			break;

		case SET_SID_TYPE:
			printCommand(deviceId, sidType);
			switch (proceed()) {
			case 'y':
			case 'Y':
				System.out.printf("RC=%d\n", setSidType(deviceId, sidType));
				System.out.println("Done! Please exit tool, re-connect SIDBlaster and restart JSIDPlay2!!!");
				break;

			default:
				System.out.println("Aborted by user!");
				break;

			}
			break;

		case INFO:
		default:
			System.out.println("Detected SIDBlaster devices:");
			for (int i = 0; i < serialNumbers.length; i++) {
				printCommand(i, getSidType(i));
			}
			break;

		}
	}

	private String credits() {
		StringBuilder result = new StringBuilder();
		result.append("=========================================================================================\n");
		result.append("SIDBlaster tool is a tool to read or write settings of your SIDBlaster USB device\n");
		result.append("Original tool by Andreas Schumm(https://github.com/gh0stless/SIDBlaster-USB-Tic-Tac-Edition)\n");
		result.append("Java Version by Ken Händel\n");
		result.append("DLL created by Stein Pedersen\n");
		result.append("=========================================================================================\n");
		return result.toString();
	}

	private void printCommand(int deviceId, SIDType sidType) {
		final String serialNumber = serialNumbers[deviceId];
		System.out.printf("command=%s deviceId=%d, serial=%s, type=%s\n", command, deviceId, serialNumber, sidType);
	}

	private int proceed() throws IOException {
		System.out.println("You are about to write settings to SIDBlaster USB device. Are you sure to proceed? (y/N)");
		return System.in.read();
	}

	private void exit(int rc) throws IOException {
		System.out.println("Press <enter> to exit!");
		System.in.read();
		System.exit(rc);
	}

	public static void main(String[] args) throws Exception {
		new SIDBlasterTool().create(args);
	}

}
