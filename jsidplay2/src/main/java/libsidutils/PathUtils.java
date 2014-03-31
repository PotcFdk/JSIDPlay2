package libsidutils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

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

	public static File getFile(String path, File hvscRoot, File cgscRoot) {
		List<File> files;
		files = PathUtils.getFiles(path, hvscRoot, null);
		if (files.size() > 0) {
			// relative path name of HVSC?
			return files.get(files.size() - 1);
		}
		files = PathUtils.getFiles(path, cgscRoot, null);
		if (files.size() > 0) {
			// relative path name of CGSC?
			return files.get(files.size() - 1);
		}
		return new File(path);
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
					if (childFile.getName().equals(pathSeg)) {
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

	public static String getExtension(final String filename) {
		int lastIndexOf = filename.lastIndexOf('.');
		final String ext;
		if (lastIndexOf != -1) {
			ext = filename.substring(lastIndexOf);
		} else {
			ext = "";
		}
		return ext;
	}

}
