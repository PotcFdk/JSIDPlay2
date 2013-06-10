package libsidutils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

import libsidutils.zip.ZipEntryFileProxy;

/**
 * This class provides a function used to generate a path name. JSIDPlay uses
 * path names that are absolute but WITOUT a drive letter on Windows machines.
 * This makes it possible to use JSIDPlay2 on an USB stick without caring for
 * the actual drive letter that it is connected with.
 * 
 * @author Ken Haendel
 */
@SuppressWarnings("resource")
public class PathUtils {
	private static final int COPY_FILE_BUFFER_SIZE = 1 << 20;

	/**
	 * ZIP entries uses slash, Windows uses backslash.
	 */
	private static Pattern separator = Pattern.compile("[/\\\\]");

	public static String getCollectionName(final File collectionRoot,
			final File file) {
		return toPath(getFiles(file.getPath(), collectionRoot, null));
	}

	private static String toPath(List<File> files) {
		StringBuilder result = new StringBuilder();
		for (File pathSeg : files) {
			Scanner scanner = new Scanner(pathSeg.getName()).useDelimiter("/");
			result.append("/").append(scanner.next());
		}
		return result.toString();
	}

	public static List<File> getFiles(String filePath, File rootFile,
			FileFilter fileFilter) {
		if (rootFile == null) {
			return Collections.emptyList();
		}
		String rootPath = rootFile.getPath();
		if (filePath.startsWith(rootPath)) {
			// remove root folder and separator
			// not for ZIP file entries
			filePath = filePath.substring(rootPath.length());
			if (filePath.length() > 0) {
				filePath = filePath.substring(1);
			}
		}
		final ArrayList<File> pathSegs = new ArrayList<File>();
		File curFile = rootFile;
		Scanner scanner = new Scanner(filePath).useDelimiter(separator);
		outer: while (scanner.hasNext()) {
			final String pathSeg = scanner.next();
			File[] childFiles = fileFilter != null ? curFile
					.listFiles(fileFilter) : curFile.listFiles();
			if (childFiles != null) {
				for (File childFile : childFiles) {
					Scanner childFileScanner = new Scanner(childFile.getName())
							.useDelimiter(separator);
					if (childFileScanner.next().equals(pathSeg)) {
						curFile = childFile;
						pathSegs.add(curFile);
						continue outer;
					}
				}
			}
			return Collections.emptyList();
		}
		return pathSegs;
	}

	public static void copyFile(File inputFile, File outputFile)
			throws IOException {
		try (InputStream input = (inputFile instanceof ZipEntryFileProxy) ? ((ZipEntryFileProxy) inputFile)
				.getInputStream() : new FileInputStream(inputFile);
				OutputStream output = new FileOutputStream(outputFile)) {

			final ReadableByteChannel inputChannel = Channels.newChannel(input);
			final WritableByteChannel outputChannel = Channels
					.newChannel(output);
			PathUtils.fastChannelCopy(inputChannel, outputChannel);
		}
	}

	private static void fastChannelCopy(final ReadableByteChannel src,
			final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer
				.allocateDirect(COPY_FILE_BUFFER_SIZE);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

	public static String getBaseNameNoExt(final File file) {
		String filename = file.getName();
		int lastIndexOf = filename.lastIndexOf('.');
		final String basename;
		if (lastIndexOf != -1) {
			basename = filename.substring(0, lastIndexOf);
		} else {
			basename = filename;
		}
		return basename;
	}

}
