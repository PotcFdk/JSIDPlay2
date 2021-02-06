package builder.sidblaster;

import static builder.sidblaster.Command.GET_SID_TYPE;
import static builder.sidblaster.Command.SET_SID_TYPE;
import static builder.sidblaster.SidBlasterBuilder.getSerialNumbers;
import static builder.sidblaster.SidBlasterBuilder.getSidType;
import static builder.sidblaster.SidBlasterBuilder.setSidType;
import static builder.sidblaster.SidType.SIDTYPE_NONE;

import java.util.Arrays;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidutils.DebugUtil;
import sidplay.ini.IniConfig;

@Parameters(resourceBundle = "builder.sidblaster.SidBuilderTool")
public class SidBlasterTool {

	static {
		DebugUtil.init();
	}

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--command", "-c" }, descriptionKey = "COMMAND", required = true, order = 1)
	private Command command;

	@Parameter(names = { "--deviceId", "-d" }, descriptionKey = "DEVICE_ID", order = 2)
	private int deviceId;

	@Parameter(names = { "--sidType", "-t" }, descriptionKey = "SID_TYPE", order = 3)
	private SidType sidType = SIDTYPE_NONE;

	private IniConfig config = new IniConfig(true);

	private void create(String[] args) throws Exception {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(Arrays.asList(args).stream().map(arg -> arg == null ? "" : arg).toArray(String[]::new));
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		// trigger read library
		new SidBlasterBuilder(null, config, null);

		final String[] serialNumbers = getSerialNumbers();
		if (command == GET_SID_TYPE) {
			if (deviceId < serialNumbers.length) {
				System.out.printf("GET %s = %s\n", serialNumbers[deviceId], getSidType(deviceId));
			}
		} else if (command == SET_SID_TYPE) {

			if (deviceId < serialNumbers.length) {
				System.out.printf("Do you really want to set SID type of device %d (%s) to %s\n", deviceId,
						serialNumbers[deviceId], sidType);
				while (System.in.available() == 0) {
					Thread.sleep(1000);
				}
				final int key = System.in.read();
				switch (key) {
				case 'y':
					int status = setSidType(deviceId, sidType);
					System.out.printf("SET %s = %s (rc=%d)\n", serialNumbers[deviceId], sidType, status);
					System.out.println("Done! exit tool and reconnect SIDBlaster!!!!!!");
					System.out.println("Press <enter> to exit!");
					System.in.read();
					System.exit(0);
					break;
				case 'n':
				default:
					System.out.println("Aborted!");
					break;
				}
			}
		} else {
			// DEFAULT: command == INFO

			for (byte i = 0; i < serialNumbers.length; i++) {
				System.out.printf("%s = %s\n", serialNumbers[i], getSidType(i));
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new SidBlasterTool().create(args);
	}

}
