package applet.gamebase.createdb.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import applet.gamebase.GameBase;

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

	@Override
	public void commit() throws DatabaseException {
		super.commit();
		PreparedStatement prepareStatement = null;
		try {
			prepareStatement = connection.prepareStatement("CHECKPOINT");
			prepareStatement.execute();
		} catch (SQLException e) {
			throw (new DatabaseException(e));
		} finally {
			if (prepareStatement != null) {
				try {
					prepareStatement.close();
				} catch (SQLException e) {
					throw (new DatabaseException(e));
				}
			}
		}

	}

	@Override
	public void createVersionTable() {
		PreparedStatement prepareStatement = null;
		try {
			prepareStatement = connection
					.prepareStatement("CREATE CACHED TABLE version (number INTEGER)");
			prepareStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				prepareStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			prepareStatement = connection
					.prepareStatement("INSERT INTO version VALUES ( ? )");
			prepareStatement.setInt(1,
					Integer.valueOf(GameBase.EXPECTED_VERSION));
			prepareStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				prepareStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		try {
			connection.commit();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			prepareStatement = connection.prepareStatement("CHECKPOINT DEFRAG");
			prepareStatement.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				prepareStatement.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}
}
