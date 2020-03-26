package ui.entities;

import java.util.HashMap;

public class PersistenceProperties extends HashMap<String, String> {
	private static final long serialVersionUID = -4166092050575739736L;

	public static final String CONFIG_DS = "configuration-ds";
	public static final String HVSC_DS = "hvsc-ds";
	public static final String CGSC_DS = "cgsc-ds";
	public static final String GAMEBASE_DS = "gamebase-ds";
	public static final String WHATSSID_DS = "whatssid-ds";

	public PersistenceProperties(String url, String username, String password, Database type) {
		this(type.getJdbcDriver(), type.getJdbcUrl().replace("${name}", url), username, password, type.getSqlDialect());
	}

	public PersistenceProperties(String driver, String url, String username, String password, String dialect) {
		put("hibernate.connection.driver_class", driver);
		put("hibernate.connection.url", url);
		put("hibernate.connection.username", username);
		put("hibernate.connection.password", password);
		put("hibernate.dialect", dialect);
	}
}
