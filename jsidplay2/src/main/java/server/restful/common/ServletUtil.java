package server.restful.common;

import static server.restful.common.JSIDPlay2Servlet.C64_MUSIC;
import static server.restful.common.JSIDPlay2Servlet.CGSC;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Properties;

import de.schlichtherle.truezip.file.TFile;
import libsidutils.PathUtils;
import ui.entities.config.Configuration;

public class ServletUtil {

	private Configuration configuration;

	private Properties directoryProperties;

	public ServletUtil(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Properties getDirectoryProperties() {
		return directoryProperties;
	}

	public File getAbsoluteFile(String path, boolean adminRole) throws FileNotFoundException {
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvsc();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgsc();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				return PathUtils.getFile(directoryValue + path.substring(directoryLogicalName.length()),
						new TFile(directoryValue), null);
			}
		}
		throw new FileNotFoundException(path);
	}

}
