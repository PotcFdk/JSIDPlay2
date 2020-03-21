package ui.entities;

public enum Database {

	/**
	 * File-based Java Database
	 */
	HSQL_FILE("org.hsqldb.jdbcDriver", "jdbc:hsqldb:file:${name};hsqldb.sqllog=0;hsqldb.applog=0;shutdown=true"),

	/**
	 * In-Memory Java Database
	 */
	HSQL_MEM("org.hsqldb.jdbcDriver", "jdbc:hsqldb:mem:${name};hsqldb.sqllog=0;hsqldb.applog=0;shutdown=true"),

	/**
	 * MSAccess Database
	 */
	MSACCESS("net.ucanaccess.jdbc.UcanaccessDriver", "jdbc:ucanaccess://${name};showschema=true"),

	/**
	 * MSSQL Database
	 */
	MSSQL("com.mysql.jdbc.Driver", "jdbc:mysql://${name}");

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
