package server.restful.common;

import java.util.Properties;

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

}
