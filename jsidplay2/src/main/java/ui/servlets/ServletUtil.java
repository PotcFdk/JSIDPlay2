package ui.servlets;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;

public class ServletUtil {

	private static class FileTypeComparator implements Comparator<File> {

		@Override
		public int compare(File file1, File file2) {

			if (file1.isDirectory() && file2.isFile())
				return -1;
			if (file1.isDirectory() && file2.isDirectory()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			if (file1.isFile() && file2.isFile()) {
				return file1.getName().compareToIgnoreCase(file2.getName());
			}
			return 1;
		}
	}

	private static final String C64_MUSIC = "/C64Music";
	private static final String CGSC = "/CGSC";
	private static final Comparator<File> COLLECTION_FILE_COMPARATOR = new FileTypeComparator();
	
	private Configuration configuration;

	public ServletUtil(Configuration configuration) {
		this.configuration = configuration;
	}

	public List<String> getDirectory(String path, String filter) {
		if (path.equals("/")) {
			return Arrays.asList(C64_MUSIC + "/", CGSC + "/");
		} else if (path.startsWith(C64_MUSIC)) {
			String root = configuration.getSidplay2Section().getHvsc();
			File rootFile = configuration.getSidplay2Section().getHvscFile();
			return getCollectionFiles(rootFile, root, path, filter, C64_MUSIC);
		} else if (path.startsWith(CGSC)) {
			String root = configuration.getSidplay2Section().getCgsc();
			File rootFile = configuration.getSidplay2Section().getCgscFile();
			return getCollectionFiles(rootFile, root, path, filter, CGSC);
		}
		return null;
	}

	private List<String> getCollectionFiles(File rootFile, String root, String path, String filter,
			String virtualCollectionRoot) {
		ArrayList<String> result = new ArrayList<String>();
		if (rootFile != null) {
			File file = ZipFileUtils.newFile(root, path.substring(virtualCollectionRoot.length()));
			File[] listFiles = file.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					if (pathname.isDirectory() && pathname.getName().endsWith(".tmp"))
						return false;
					return pathname.isDirectory() || filter == null || pathname.getName().matches(filter);
				}
			});
			if (listFiles != null) {
				List<File> asList = Arrays.asList(listFiles);
				Collections.sort(asList, COLLECTION_FILE_COMPARATOR);
				addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, file) + "/../", null);
				for (File f : asList) {
					addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, f), f);
				}
			}
		} else {
			return Arrays.asList(C64_MUSIC + "/", CGSC + "/");
		}
		return result;
	}

	private void addPath(ArrayList<String> result, String path, File f) {
		result.add(path + (f != null && f.isDirectory() ? "/" : ""));
	}

	public File getAbsoluteFile(String path) {
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvscFile();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgscFile();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		return null;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
