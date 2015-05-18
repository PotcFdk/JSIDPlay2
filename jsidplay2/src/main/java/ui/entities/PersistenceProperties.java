package ui.entities;

import java.util.HashMap;

public class PersistenceProperties extends HashMap<String, String> {
	private static final long serialVersionUID = -4166092050575739736L;

	public static final String CONFIG_DS = "configuration-ds";
	public static final String COLLECTION_DS = "collection-ds";
	public static final String GAMEBASE_DS = "gamebase-ds";

	public PersistenceProperties(String name, Database type) {
		put("hibernate.connection.driver_class", type.getJdbcDriver());
		put("hibernate.connection.url",
				type.getJdbcUrl().replace("${name}",
						name));
	}

}
