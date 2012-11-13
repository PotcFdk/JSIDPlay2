package libsidutils;

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

	public static String getHVSCName(final String hvsc, final File file) {
		return getCollectionRelName(file, hvsc);
	}

	public static String getCGSCName(final String cgsc, final File file) {
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
