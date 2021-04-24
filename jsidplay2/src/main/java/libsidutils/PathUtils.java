package libsidutils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * This class provides general filename utility functions.
 *
 * @author Ken Händel
 */
public class PathUtils {
	/**
	 * Linux, OSX and ZIP entries use slash, Windows uses backslash.
	 */
	private static final Pattern SEPARATOR = Pattern.compile("[/\\\\]");

	/**
	 * Create a filename of the given path relative to the collection root dir. <BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt;
	 * "/MUSICIANS/D/Daglish_Ben/Bombo.sid"
	 *
	 * @param collectionRoot root file of the path
	 * @param file           file to get the relative path for
	 * @return relative path to the collection root file (empty string, if the path
	 *         is not relative to the collection root file)
	 */
	public static final String getCollectionName(final File collectionRoot, final File file) {
		return createFilename(getFiles(file.getPath(), collectionRoot, null));
	}

	/**
	 * Reverse function of {@link #getFiles(String, File, FileFilter)}.<BR>
	 * e.g. [File(D), File(Daglish_Ben), File(Bombo.sid)] -&gt;
	 * "/MUSICIANS/D/Daglish_Ben/Bombo.sid"
	 *
	 * @param files file list to create a filename for
	 * @return filename filename where each path segment is delimited by a slash.
	 */
	private static final String createFilename(List<File> files) {
		StringBuilder result = new StringBuilder();
		for (File file : files) {
			result.append("/").append(file.getName());
		}
		return result.toString();
	}

	/**
	 * Get file for a given path. The path can be relative to HVSC or CGSC or even
	 * absolute.<BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt;
	 * File(/MUSICIANS/D/Daglish_Ben/Bombo.sid)
	 *
	 * @param path     path to get a file for, possible root directory can be either
	 *                 hvscRoot or cgscRoot or none, if absolute
	 * @param hvscRoot root of HVSC
	 * @param cgscRoot root of CGSC
	 * @return file of the path
	 */
	public static final File getFile(String path, File hvscRoot, File cgscRoot) {
		List<File> files = getFiles(path, hvscRoot, null);
		if (files.size() > 0) {
			// relative path name of HVSC?
			return files.get(files.size() - 1);
		}
		files = getFiles(path, cgscRoot, null);
		if (files.size() > 0) {
			// relative path name of CGSC?
			return files.get(files.size() - 1);
		}
		// absolute path name
		return new File(path);
	}

	/**
	 * Get the file list of the given file path. Each entry corresponds to a path
	 * segment. It is sorted from parent to child.<BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt; [File(MUSICIANS),
	 * File(D), File(Daglish_Ben), File(Bombo.sid)]
	 *
	 * @param path       file path to get the file list for. Each path segment is
	 *                   delimited by slash or backslash.
	 * @param rootFile   Root file to start. The first path segment must match a
	 *                   direct child of rootPath and so on.
	 * @param fileFilter Files contained in the file filter are visible as child
	 *                   files (null means filter disabled)
	 * @return a file list sorted from the parent file to the child file (empty
	 *         list, if the path is wrong or incomplete)
	 */
	public static final List<File> getFiles(String path, File rootFile, FileFilter fileFilter) {
		if (rootFile == null) {
			return Collections.emptyList();
		}
		String rootPath = rootFile.getPath();
		if (path.startsWith(rootPath)) {
			// remove root folder and separator (not for ZIP file entries)
			path = path.substring(rootPath.length());
			if (path.length() > 0) {
				path = path.substring(1);
			}
		}
		final List<File> pathSegs = new ArrayList<>();
		try (Scanner scanner = new Scanner(path)) {
			scanner.useDelimiter(SEPARATOR);
			nextPathSeg: while (scanner.hasNext()) {
				final String pathSeg = scanner.next();
				File[] childFiles = rootFile.listFiles(fileFilter);
				if (childFiles != null) {
					for (File childFile : childFiles) {
						if (childFile.getName().equalsIgnoreCase(pathSeg)) {
							pathSegs.add(rootFile = childFile);
							continue nextPathSeg;
						}
					}
				}
				return Collections.emptyList();
			}
			return pathSegs;
		}
	}

	/**
	 * Strip suffix of a filename.
	 *
	 * @param filename filename to get the suffix for
	 * @return filename without suffix (e.g. "Bombo.sid" -&gt; "Bombo")
	 */
	public static final String getFilenameWithoutSuffix(final String filename) {
		return filename.substring(0, filename.length() - getFilenameSuffix(filename).length());
	}

	/**
	 * Get suffix of a filename.
	 *
	 * @param filename filename to get the suffix for
	 * @return suffix of a filename (e.g. "Bombo.sid" -&gt; ".sid")
	 */
	public static final String getFilenameSuffix(final String filename) {
		int lastIndexOf = filename.lastIndexOf('.');
		return lastIndexOf != -1 ? filename.substring(lastIndexOf) : "";
	}

}
