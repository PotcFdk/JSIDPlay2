package libsidutils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * This class provides a general filename utility functions.
 * 
 * @author Ken Händel
 */
@SuppressWarnings("resource")
public class PathUtils {
	/**
	 * ZIP entries uses slash, Windows uses backslash.
	 */
	private static final Pattern separator = Pattern.compile("[/\\\\]");

	public static final String getCollectionName(final File collectionRoot,
			final String path) {
		return toPath(getFiles(path, collectionRoot, null));
	}

	private static final String toPath(List<File> files) {
		StringBuilder result = new StringBuilder();
		for (File pathSeg : files) {
			Scanner scanner = new Scanner(pathSeg.getName()).useDelimiter("/");
			result.append("/").append(scanner.next());
		}
		return result.toString();
	}

	public static final File getFile(String path, File hvscRoot, File cgscRoot) {
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

	public static final List<File> getFiles(String filePath, File rootFile,
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

	public static final String getBaseNameNoExt(final String name) {
		int lastIndexOf = name.lastIndexOf('.');
		final String basename;
		if (lastIndexOf != -1) {
			basename = name.substring(0, lastIndexOf);
		} else {
			basename = name;
		}
		return basename;
	}

	public static final String getExtension(final String filename) {
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
