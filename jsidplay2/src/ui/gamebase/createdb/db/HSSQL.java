package ui.gamebase.createdb.db;

/**
 * Database class for the HSSQL database.
 * 
 * @author Jeff Heaton (http://www.heatonresearch.com)
 * 
 */
public class HSSQL extends Database {

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
		throw new RuntimeException("Should never be called!");
	}

	@Override
	public boolean isAutoIncrementSupported() {
		return false;
	}

	@Override
	public boolean isPrimaryKey(String columnName) {
		throw new RuntimeException("Should never be called!");
	}

	@Override
	public String getCreateStmtLayout() {
		return "CREATE CACHED TABLE ";
	}

}
