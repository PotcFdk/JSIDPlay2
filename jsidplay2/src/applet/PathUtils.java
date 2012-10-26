package applet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.intf.IConfig;

/**
 * This class provides a function used to generate a path name. JSIDPlay uses
 * path names that are absolute but WITOUT a drive letter on Windows machines.
 * This makes it possible to use JSIDPlay2 on an USB stick without caring for
 * the actual drive letter that it is connected with.
 * 
 * @author Ken Haendel
 */
public class PathUtils {
	private static final int COPY_FILE_BUFFER_SIZE = 1 << 20;

	/**
	 * Remove windows drive letter.
	 * 
	 * @param home
	 *            base path, should be a directory, not a file, or it doesn't
	 *            make sense
	 * @param f
	 *            file to generate path for
	 * @return path from home to f as a string or null (maybe the file f is on a
	 *         different drive)
	 */
	public static String getPath(File f) {
		if (!onTheSameDriveLikeActualRootDir(f)) {
			// Use absolute path
			return f.getAbsolutePath();
		} else {
			// Remove drive letter on Windows
			return withoutWinDriveLetter(f);
		}
	}

	/**
	 * Check if Windows OS and remove drive letter.
	 * 
	 * @param f
	 *            file to get path without the drive letter.
	 * @return absolute path
	 */
	private static String withoutWinDriveLetter(File f) {
		boolean windowsOS = System.getProperty("os.name").startsWith("Win");
		String src = f.getAbsolutePath();
		if (windowsOS) {
			// Remove drive letter (e.g. "c:")
			return src.substring(2);
		} else {
			return src;
		}
	}

	/**
	 * Check if the file is located on the same drive letter on Windows machine.
	 * On other OS it returns true
	 * 
	 * @param f
	 *            file to check
	 * @return file is located on the current drive letter
	 */
	private static boolean onTheSameDriveLikeActualRootDir(File f) {
		boolean windowsOS = System.getProperty("os.name").startsWith("Win");
		// Note: To determine the current drive letter we use the root path "/"
		// and getting the absolute path name
		String src = new File(System.getProperty("file.separator"))
				.getAbsolutePath();
		String dest = f.getAbsolutePath();
		boolean sameDrive = true;
		if (windowsOS) {
			// compare drive letter
			src = src.substring(0);
			dest = dest.substring(0);
			sameDrive = Character.toLowerCase(src.charAt(0)) == Character
					.toLowerCase(dest.charAt(0));
		}
		return sameDrive;
	}

	public static String getHVSCName(final IConfig config, final File file) {
		String hvsc = config.getSidplay2().getHvsc();
		return getCollectionRelName(file, hvsc);
	}

	public static String getCGSCName(final IConfig config, final File file) {
		String cgsc = config.getSidplay2().getCgsc();
		return getCollectionRelName(file, cgsc);
	}

	public static String getCollectionRelName(final File file,
			String collectionRoot) {
		try {
			if (collectionRoot == null || collectionRoot.length() == 0) {
				return null;
			}
			if (file instanceof ZipEntryFileProxy) {
				final int indexOf = file.getPath().indexOf('/');
				if (indexOf == -1) {
					return null;
				}
				return file.getPath().substring(indexOf);
			}
			final String canonicalPath = file.getCanonicalPath();
			final String collCanonicalPath = new File(collectionRoot)
					.getCanonicalPath();
			if (canonicalPath.startsWith(collCanonicalPath)) {
				final String name = canonicalPath.substring(
						collCanonicalPath.length()).replace('\\', '/');
				if (name.startsWith("/")) {
					return name;
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void copyFile(File inputFile, File outputFile)
			throws IOException {
		final InputStream input = new FileInputStream(inputFile);
		final OutputStream output = new FileOutputStream(outputFile);
		final ReadableByteChannel inputChannel = Channels.newChannel(input);
		final WritableByteChannel outputChannel = Channels.newChannel(output);
		PathUtils.fastChannelCopy(inputChannel, outputChannel);
		inputChannel.close();
		outputChannel.close();
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
