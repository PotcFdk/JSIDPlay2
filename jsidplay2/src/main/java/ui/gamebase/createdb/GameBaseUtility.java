package ui.gamebase.createdb;

import java.sql.SQLException;

import ui.entities.PersistenceProperties;
import ui.gamebase.createdb.db.Database;
import ui.gamebase.createdb.db.HSSQL;
import ui.gamebase.createdb.db.MSAccess;


/**
 * A utility to copy data from two databases, as specified by command line
 * arguments. The configuration file is of the format:
 * 
 * sourceDriver=sun.jdbc.odbc.JdbcOdbcDriver
 * sourceURL=jdbc:odbc:Driver={Microsoft Access Driver (*.mdb,
 * *.accdb)};DBQ=${gb64.mdbfile} targetDriver=org.hsqldb.jdbcDriver
 * targetURL=jdbc:hsqldb:file:${gamebase_dir}/gb64.idx;shutdown=true,create=true
 * 
 * @author Jeff Heaton (http://www.heatonresearch.com)
 * 
 */
public class GameBaseUtility {
	private String sourceDriver = "";
	private String sourceURL = "";
	private String targetDriver = "";
	private String targetURL = "";

	public void getArguments(String[] str) {
		for (String arg : str) {
			int i = arg.indexOf('=');
			if (i == -1)
				continue;
			String cmd = arg.substring(0, i).trim();
			String val = arg.substring(i + 1).trim();
			if (cmd.equalsIgnoreCase("sourceDriver")) {
				sourceDriver = val;
			} else if (cmd.equalsIgnoreCase("sourceURL")) {
				sourceURL = val;
			} else if (cmd.equalsIgnoreCase("targetDriver")) {
				targetDriver = val;
			} else if (cmd.equalsIgnoreCase("targetURL")) {
				targetURL = val;
			}
		}
	}

	public void run() {
		GameBaseCopier mover = new GameBaseCopier();

		Database source = new MSAccess();
		source.connect(sourceDriver, sourceURL, PersistenceProperties.GAMEBASE_DS);

		Database target = new HSSQL();
		target.connect(targetDriver, targetURL, PersistenceProperties.GAMEBASE_DS);

		mover.setSource(source);
		mover.setTarget(target);

		try {
			mover.exportDatabse();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			source.close();
			target.flush();
			target.close();
		}
	}

	public static void main(String args[]) {
		if (args.length < 1) {
			System.out.println("Usage:\n\njava DataMoverUtility "
					+ "sourceDriver=<sourceDriver> sourceURL=<sourceURL> "
					+ "targetDriver=<targetDriver> targetURL=<targetURL>");
		} else {
			GameBaseUtility utility = new GameBaseUtility();
			utility.getArguments(args);
			utility.run();
		}

	}
}
