package ui.entities;

public enum Database {

	/**
	 * Java Database
	 */
	HSQL("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:${file};shutdown=true"),

	/**
	 * MSAccess Database
	 */
	MSACCESS("net.ucanaccess.jdbc.UcanaccessDriver",
			"jdbc:ucanaccess://${file};showschema=true");

	private String jdbcDriver;
	private String jdbcUrl;

	private Database(String driver, String url) {
		jdbcDriver = driver;
		jdbcUrl = url;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}
}
