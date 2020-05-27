package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@SuppressWarnings("unchecked")
public class ZipFileUtils {

	private static final int COPY_BUFFER_CHUNK_SIZE = 16 * 1024;
	
	private static Constructor<? extends InputStream> INPUT_STREAM;
	private static Constructor<File> FILE;

	static {
		try {
			// standard java.io functionality
			FILE = File.class.getConstructor(String.class, String.class);
			INPUT_STREAM = FileInputStream.class.getConstructor(File.class);
			// support for files contained in a ZIP (optionally in the classpath)
			FILE = (Constructor<File>) Class.forName("de.schlichtherle.truezip.file.TFile").getConstructor(String.class,
					String.class);
			INPUT_STREAM = (Constructor<InputStream>) Class
					.forName("de.schlichtherle.truezip.file.TFileInputStream").getConstructor(File.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
		}
	}

	public static File newFile(String parent, String child) {
		try {
			return FILE.newInstance(parent, child);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return new File(parent, child);
		}
	}

	public static InputStream newFileInputStream(File file) throws FileNotFoundException {
		try {
			return INPUT_STREAM.newInstance(file);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return new FileInputStream(file);
		}
	}

	public static void copy(File file, OutputStream os) throws IOException {
		copy(newFileInputStream(file), os);
	}

	public static void copy(InputStream is, OutputStream os) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(is);
		final WritableByteChannel outputChannel = Channels.newChannel(os);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_CHUNK_SIZE);

		while (inputChannel.read(buffer) != -1) {
			((Buffer) buffer).flip();
			outputChannel.write(buffer);
			buffer.compact();
		}
		((Buffer) buffer).flip();
		while (buffer.hasRemaining()) {
			outputChannel.write(buffer);
		}
	}

	public static String convertStreamToString(java.io.InputStream is, String charsetName) {
		return convertStreamToString(is, charsetName, new HashMap<>());
	}

	public static String convertStreamToString(java.io.InputStream is, String charsetName, Map<String,String>replacements) {
		try (java.util.Scanner s = new java.util.Scanner(is, charsetName)) {
			s.useDelimiter("\\A");
			String string = s.hasNext() ? s.next() : "";
			for (Entry<String, String> replacement : replacements.entrySet()) {
				string = string.replace(replacement.getKey(), replacement.getValue());
			}
			return string;
		}
	}
}

