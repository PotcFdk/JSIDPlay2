package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("unchecked")
public class ZipFileUtils {

	private static Constructor<? extends InputStream> INPUT_STREAM;
	private static Constructor<File> FILE;

	static {
		try {
			// standard java.io functionality
			FILE = (Constructor<File>) File.class.getConstructor(String.class, String.class);
			INPUT_STREAM = (Constructor<FileInputStream>) FileInputStream.class.getConstructor(File.class);
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
			throw new RuntimeException(e);
		}
	}

	public static InputStream newFileInputStream(File file) throws FileNotFoundException {
		try {
			return INPUT_STREAM.newInstance(file);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			throw (e.getCause() instanceof FileNotFoundException) ? (FileNotFoundException) e.getCause()
					: new FileNotFoundException(e.getMessage());
		}
	}
}
