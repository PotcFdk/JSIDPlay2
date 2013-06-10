package ui.gamebase.createdb.db;

/**
 * Database class for the HSSQL database.
 * 
 * @author Jeff Heaton (http://www.heatonresearch.com)
 * 
 */
public class MSAccess extends Database {

	/**
	 * Abstract method to process a database type. Sometimes database types are
	 * not reported exactly as they need to be for proper syntax. This method
	 * corrects the database type and size.
	 * 
	 * @param type
	 *            The type reported
	 * @param i
	 *            The size of this column
	 * @return The properly formatted type, for this database
	 */
	public String processType(String type, int size) {
		if (type.equals("COUNTER")) {
			return "INT";
		} else if ("LONGCHAR".equals(type)) {
			return "VARCHAR";
		} else if ("BIT".equals(type)) {
			return "INT";
		} else if ("BYTE".equals(type)) {
			return "INT";
		}
		return type;
	}

	/**
	 * Check to see if the specified type is numeric.
	 * 
	 * @param type
	 *            The type to check.
	 * @return Returns true if the type is numeric.
	 */
	public boolean isNumeric(int type) {
		return false;
	}

	@Override
	public boolean isAutoIncrementSupported() {
		throw new RuntimeException("Should never be called!");
	}

	@Override
	public boolean isPrimaryKey(String columnName) {
		return columnName.endsWith("_ID");
	}

	@Override
	public String getCreateStmtLayout() {
		throw new RuntimeException("Should never be called!");
	}

}
