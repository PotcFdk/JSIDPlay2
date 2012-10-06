package applet.gamebase.createdb;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import applet.gamebase.createdb.db.Database;

public class GameBaseCopier {
	/**
	 * The source database.
	 */
	private Database source;

	/**
	 * The target database.
	 */
	private Database target;

	/**
	 * The list of tables, from the source database.
	 */
	private List<String> tables = new ArrayList<String>();

	public Database getSource() {
		return source;
	}

	public void setSource(Database source) {
		this.source = source;
	}

	public Database getTarget() {
		return target;
	}

	public void setTarget(Database target) {
		this.target = target;
	}

	/**
	 * Create the specified table. To do this the source database will be
	 * scanned for the table's structure. Then the table will be created in the
	 * target database.
	 * 
	 * @param table
	 *            The name of the table to create.
	 * @throws DatabaseException
	 *             If a database error occurs.
	 */
	public void createTable(String table) throws SQLException {
		String sql;

		// if the table already exists, then drop it
		try {
			sql = source.generateDrop(table);
			target.execute(sql);
		} catch (Exception e) {
			// Ignore non-existent table
		}

		// now create the table
		sql = source.generateCreate(table, target);
		target.execute(sql);
	}

	/**
	 * Create all of the tables in the database. This is done by looping over
	 * the list of tables and calling createTable for each.
	 * 
	 * @throws DatabaseException
	 *             If an error occurs.
	 */
	private void createTables() throws SQLException {
		System.out.println("Create tables.");
		Collection<String> list = source.listTables();
		for (String table : list) {
			System.out.println("Create table: " + table);
			createTable(table);
			tables.add(table);
		}
	}

	private void copyTableData() throws SQLException {
		for (String table : tables) {
			System.out.println("Begin copy: " + table);

			// now copy
			target.copy(source, table);
		}
	}

	public void exportDatabse() throws SQLException {
		createTables();
		copyTableData();
	}

}
