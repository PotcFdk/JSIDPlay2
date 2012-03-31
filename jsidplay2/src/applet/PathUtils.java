package applet;

import java.io.File;

/**
 * This class provides a function used to generate a path name. JSIDPlay uses
 * path names that are absolute but WITOUT a drive letter on Windows machines.
 * This makes it possible to use JSIDPlay2 on an USB stick without caring for
 * the actual drive letter that it is connected with.
 * 
 * @author Ken Haendel
 */
public class PathUtils {
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

}
