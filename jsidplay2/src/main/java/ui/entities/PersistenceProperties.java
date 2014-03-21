package ui.entities;

import java.io.File;
import java.util.HashMap;

public class PersistenceProperties extends HashMap<String, String> {
	private static final long serialVersionUID = -4166092050575739736L;

	public static final String CONFIG_DS = "configuration-ds";
	public static final String COLLECTION_DS = "collection-ds";
	public static final String GAMEBASE_DS = "gamebase-ds";

	public PersistenceProperties(File databaseFile) {
		put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		put("hibernate.connection.url",
				"jdbc:hsqldb:file:" + databaseFile.getAbsolutePath()
						+ ";shutdown=true");
	}

	public PersistenceProperties(String driver, String jdbcURL) {
		put("hibernate.connection.driver_class", driver);
		put("hibernate.connection.url", jdbcURL);
	}

}
