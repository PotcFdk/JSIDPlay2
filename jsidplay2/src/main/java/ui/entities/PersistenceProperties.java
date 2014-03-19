package ui.entities;

import java.io.File;
import java.util.HashMap;

public class PersistenceProperties extends HashMap<String, String> {
	private static final long serialVersionUID = -4166092050575739736L;

	public static final String CONFIG_DS = "jsidplay2-ds";
	public static final String COLLECTION_DS = CONFIG_DS;
	public static final String GAMEBASE_DS = CONFIG_DS;
	public static final String MDB_DS = "mdb-ds";

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
