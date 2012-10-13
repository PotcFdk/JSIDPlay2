package applet.gamebase.createdb;

import java.io.File;
import java.sql.SQLException;

import applet.gamebase.createdb.db.Database;
import applet.gamebase.createdb.db.HSSQL;
import applet.gamebase.createdb.db.MSAccess;

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
		int mdbNameIdx = sourceURL.indexOf("DBQ=");
		if (mdbNameIdx != -1) {
			File mdbFile = new File(sourceURL.substring(mdbNameIdx
					+ "DBQ=".length()));
			if (!mdbFile.exists()) {
				System.err.println("MDB file does not exist: "
						+ mdbFile.getAbsolutePath());
				return;
			}
		}
		source.connect(sourceDriver, sourceURL);

		Database target = new HSSQL();
		target.connect(targetDriver, targetURL);

		mover.setSource(source);
		mover.setTarget(target);

		try {
			mover.exportDatabse();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		source.close();
		target.flush();
		target.close();
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
