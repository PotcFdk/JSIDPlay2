package ui.servlets;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import de.schlichtherle.truezip.file.TFile;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;

public class ServletUtil {

	private static final String C64_MUSIC = "/C64Music";
	private static final String CGSC = "/CGSC";

	private static final Comparator<File> COLLECTION_FILE_COMPARATOR = (file1, file2) -> {
		if (file1.isDirectory() && file2.isFile()) {
			return -1;
		} else if (file1.isFile() && file2.isDirectory()) {
			return 1;
		} else {
			return file1.getName().compareToIgnoreCase(file2.getName());
		}
	};

	private Configuration configuration;

	private Properties directoryProperties;

	public ServletUtil(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public List<String> getDirectory(String path, String filter, boolean adminRole) {
		if (path.equals("/")) {
			return getRoot(adminRole);
		} else if (path.startsWith(C64_MUSIC)) {
			String root = configuration.getSidplay2Section().getHvsc();
			File rootFile = configuration.getSidplay2Section().getHvscFile();
			return getCollectionFiles(rootFile, root, path, filter, C64_MUSIC, adminRole);
		} else if (path.startsWith(CGSC)) {
			String root = configuration.getSidplay2Section().getCgsc();
			File rootFile = configuration.getSidplay2Section().getCgscFile();
			return getCollectionFiles(rootFile, root, path, filter, CGSC, adminRole);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				File rootFile = new TFile(directoryValue);
				return getCollectionFiles(rootFile, directoryValue, path, filter, directoryLogicalName, adminRole);
			}
		}
		return getRoot(adminRole);
	}

	private List<String> getCollectionFiles(File rootFile, String root, String path, String filter,
			String virtualCollectionRoot, boolean adminRole) {
		ArrayList<String> result = new ArrayList<String>();
		if (rootFile != null) {
			File file = ZipFileUtils.newFile(root, path.substring(virtualCollectionRoot.length()));
			File[] listFiles = file.listFiles(pathname -> {
				if (pathname.isDirectory() && pathname.getName().endsWith(".tmp"))
					return false;
				return pathname.isDirectory() || filter == null
						|| pathname.getName().toLowerCase(Locale.US).matches(filter);
			});
			if (listFiles != null) {
				List<File> asList = Arrays.asList(listFiles);
				Collections.sort(asList, COLLECTION_FILE_COMPARATOR);
				addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, file) + "/../", null);
				for (File f : asList) {
					addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, f), f);
				}
			}
		}
		if (result.isEmpty()) {
			return getRoot(adminRole);
		}
		return result;
	}

	private void addPath(ArrayList<String> result, String path, File f) {
		result.add(path + (f != null && f.isDirectory() ? "/" : ""));
	}

	private List<String> getRoot(boolean adminRole) {
		List<String> result = new ArrayList<>(Arrays.asList(C64_MUSIC + "/", CGSC + "/"));
		
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if (!needToBeAdmin || adminRole) {
				result.add(directoryLogicalName + "/");
			}
		}
		return result;
	}

	public File getAbsoluteFile(String path, boolean adminRole) throws FileNotFoundException {
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvscFile();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgscFile();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if (adminRole == needToBeAdmin && path.startsWith(directoryLogicalName) && directoryValue != null) {
				return PathUtils.getFile(directoryValue + path.substring(directoryLogicalName.length()), null, null);
			}
		}
		throw new FileNotFoundException(path);
	}

	public String getFavoriteFilename(HVSCEntry entry) {
		if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvscFile(), null).size() > 0) {
			return C64_MUSIC + entry.getPath();
		} else if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgscFile(), null)
				.size() > 0) {
			return CGSC + entry.getPath();
		}
		return null;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}
