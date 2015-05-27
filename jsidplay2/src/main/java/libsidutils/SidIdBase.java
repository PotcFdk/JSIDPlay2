package libsidutils;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessControlException;

public class SidIdBase {

	/**
	 * Load byte array containing the text to be searched in.
	 * 
	 * @param name
	 *            the filename to load
	 * @return the byte array with the file contents
	 * @throws IOException
	 *             read error
	 */
	protected byte[] load(final String name) throws IOException {
		try (final DataInputStream in = new DataInputStream(
				new FileInputStream(name))) {
			final int length = (int) new File(name).length();
			final byte[] buffer = new byte[length];
			in.readFully(buffer);
			return buffer;
		}
	}


	/**
	 * Load configuration file.
	 * @param fname 
	 * @param pkg 
	 * 
	 * @return the configuration file entries
	 */
	protected byte[] readConfiguration(String fname, String pkg) {
		final File iniFilename = getLocation(fname);
		if (iniFilename != null) {
			try {
				return load(iniFilename.getAbsolutePath());
			} catch (final IOException e) {
				System.err.println("Read error: " + iniFilename);
				return readInternal(fname, pkg);
			}
		} else {
			return readInternal(fname, pkg);
		}
	}

	/**
	 * Read from internal SID-ID configuration file.
	 * @param fname 
	 * @param pkg 
	 * 
	 * @return the contents
	 */
	private byte[] readInternal(String fname, String pkg) {
		try (final InputStream inputStream = getClass().getClassLoader()
				.getResourceAsStream(pkg + fname)) {
			if (inputStream == null) {
				throw new RuntimeException("Internal SIDID not found: "
						+ pkg + fname);
			}
			final int length = inputStream.available();
			final byte[] data = new byte[length];
			int count, pos = 0;
			while (pos < length
					&& (count = inputStream.read(data, pos, length - pos)) >= 0) {
				pos += count;
			}
			if (pos != length) {
				System.err.println("Internal SIDID was not loaded completely!");
			}
			return data;
		} catch (final IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Internal SIDID not found: "
					+ pkg + fname);
		}
	}

	/**
	 * Search for the configuration file at various locations.
	 * <OL>
	 * <LI>current working dir
	 * <LI>user home dir
	 * </OL>
	 * @param fname 
	 * 
	 * @return the configuration file or null (not found)
	 */
	private File getLocation(String fname) {
		try {
			final String[] paths = new String[] { "user.dir", "user.home" };
			for (final String path : paths) {
				File sidIdFile;
				sidIdFile = locate(path, fname);
				if (sidIdFile.exists()) {
					return sidIdFile;
				}
			}
		} catch (AccessControlException e) {
			// access denied in the ui version
		}
		return null;
	}

	/**
	 * Locate configuration file at the given path.
	 * 
	 * @param location
	 *            the path to search in
	 * @param fname 
	 * @return the file (caller should check exists)
	 */
	private File locate(final String location, String fname) {
		final String path = System.getProperty(location);
		if (path != null) {
			return new File(path, fname);
		} else {
			return new File("", fname);
		}
	}

}
