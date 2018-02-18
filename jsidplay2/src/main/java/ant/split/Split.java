package ant.split;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidutils.PathUtils;

/**
 * This class is used by Ant to split the HVSC in several parts to avoid the
 * maximum file size limit of the web-site provider.
 * 
 * @author Ken Händel
 */
@Parameters(resourceBundle = "libsidutils.ant.split.Split")
public class Split {

	private static final int CHUNK_SIZE = 1 << 20;

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = {"--maxFileSize", "-m"}, descriptionKey = "MAX_FILE_SIZE", required = true)
	private Integer maxFileSize;

	@Parameter(descriptionKey = "FILENAME", required = true)
	private String filename;
	
	public static void main(String[] args) throws IOException {
		new Split().doSplit(args);
	}

	private void doSplit(String[] args) throws IOException {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(args);
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}
		int partNum = 1;
		String output = createOutputFilename(filename, partNum);

		byte[] buffer = new byte[CHUNK_SIZE];
		BufferedOutputStream os = null;
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(new File(filename)), CHUNK_SIZE)) {
			int bytesRead = 0, totalBytesRead = 0;
			os = createOutputStream(output);
			int len = Math.min(buffer.length, maxFileSize - totalBytesRead);
			while ((bytesRead = is.read(buffer, 0, len)) >= 0) {
				os.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				len = Math.min(buffer.length, maxFileSize - totalBytesRead);
				if (totalBytesRead == maxFileSize) {
					os.close();
					++partNum;
					output = createOutputFilename(filename, partNum);
					os = createOutputStream(output);
					totalBytesRead = 0;
				}
			}
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String createOutputFilename(String filename, int partNum) {
		return PathUtils.getFilenameWithoutSuffix(filename) + String.format(".%03d", partNum);
	}

	private BufferedOutputStream createOutputStream(String filename) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(new File(filename)), CHUNK_SIZE);
	}

}
