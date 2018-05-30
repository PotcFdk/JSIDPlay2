package ant.crc;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import ui.download.DownloadThread;

/**
 * This class is used by Ant to create a CRC32 checksum for internal downloads from our web-site.
 * 
 * @author Ken Händel
 */
@Parameters(resourceBundle = "ant.crc.CreateChecksum")
public class CreateChecksum {

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(descriptionKey = "FILENAME", required = true)
	private String filename;

	public static void main(String[] args) throws IOException {
		new CreateChecksum().createChecksum(args);
	}

	public void createChecksum(String[] args) throws IOException {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		long checksum = DownloadThread.calculateCRC32(new File(args[0]));
		System.out.println(String.format("%8X", checksum).replace(' ', '0'));

	}
}
