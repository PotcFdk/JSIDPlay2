package ui.entities;

public enum DatabaseType {

	/**
	 * File-based Java Database
	 */
	HSQL_FILE("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:file:${name};hsqldb.sqllog=0;hsqldb.applog=0;shutdown=true",
			"org.hibernate.dialect.HSQLDialect"),

	/**
	 * In-Memory Java Database
	 */
	HSQL_MEM("org.hsqldb.jdbc.JDBCDriver", "jdbc:hsqldb:mem:${name};hsqldb.sqllog=0;hsqldb.applog=0;shutdown=true",
			"org.hibernate.dialect.HSQLDialect"),

	/**
	 * MSAccess Database
	 */
	MSACCESS("net.ucanaccess.jdbc.UcanaccessDriver", "jdbc:ucanaccess://${name};showschema=true",
			"org.hibernate.dialect.HSQLDialect");

	private String jdbcDriver;
	private String jdbcUrl;
	private String sqlDialect;

	private DatabaseType(String driver, String url, String dialect) {
		jdbcDriver = driver;
		jdbcUrl = url;
		sqlDialect = dialect;
	}

	public String getJdbcDriver() {
		return jdbcDriver;
	}

	public String getJdbcUrl() {
		return jdbcUrl;
	}

	public String getSqlDialect() {
		return sqlDialect;
	}
}
