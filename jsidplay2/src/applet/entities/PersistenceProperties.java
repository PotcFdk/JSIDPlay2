package applet.entities;

import java.io.File;
import java.util.HashMap;

public class PersistenceProperties extends HashMap<String, String> {

	public static final String CONFIG_DS = "jsidplay2-ds";
	public static final String COLLECTION_DS = "collection-ds";
	public static final String GAMEBASE_DS = "gamebase-ds";

	public PersistenceProperties(File databaseFile) {
		put("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
		put("hibernate.connection.url",
				"jdbc:hsqldb:file:" + databaseFile.getAbsolutePath()
						+ ";shutdown=true");
		common();
	}

	public PersistenceProperties(String driver, String jdbcURL) {
		put("hibernate.connection.driver_class", driver);
		put("hibernate.connection.url", jdbcURL);
		common();
	}

	private void common() {
		put("hibernate.connection.username", "");
		put("hibernate.connection.password", "");
		put("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
		put("hibernate.hbm2ddl.auto", "update");
		// put("hibernate.show_sql", "true");
		// put("hibernate.format_sql", "true");
	}

}
