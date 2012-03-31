/**
 * 
 */
package applet.collection.search;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import libsidutils.STIL;
import libsidutils.STIL.Info;
import libsidutils.STIL.STILEntry;
import libsidutils.STIL.TuneEntry;
import libsidutils.zip.ZipEntryFileProxy;
import sidplay.ini.IniConfig;
import applet.sidtuneinfo.SidTuneInfoCache;

public final class SearchIndexCreator implements ISearchListener {

	static {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			throw new ExceptionInInitializerError(e);
		}
	}
	
	/* bump this each time incompatible change in database occurs. */
	private static final int correctVersion = 4;

	private final SidTuneInfoCache infoCache;

	private final Connection conn;

	private PreparedStatement collectionInsertStatement, stilInsertStatement;

	protected IniConfig config;

	private int count;

	private File root;

	public SearchIndexCreator(File root, final IniConfig cfg,
			final Connection conn) {
		this.root = root;
		this.config = cfg;
		this.infoCache = new SidTuneInfoCache(config);
		this.conn = conn;

		prepareStatements();
	}

	private void prepareStatements() {
		try {
			StringBuilder insert = new StringBuilder(
					"INSERT INTO collection VALUES (?, ?"); // Full Path, File
															// Name
			for (@SuppressWarnings("unused") final
					String field : SidTuneInfoCache.SIDTUNE_INFOS) {
				insert.append(", ?");
			}
			insert.append(")");
			collectionInsertStatement = conn.prepareStatement(insert.toString());

			insert = new StringBuilder("INSERT INTO stil VALUES (?");// Full
																		// Path
			for (@SuppressWarnings("unused")
			String id : STIL.STIL_INFOS) {
				insert.append(", ?");
			}
			insert.append(")");
			stilInsertStatement = conn.prepareStatement(insert.toString());

		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public static final Connection getConnection(final String root,
			final String dbName, final boolean create) {
		final File idxFile = new File(new File(root).getParentFile(), dbName);
		// System.err.println("Connect to: " + idxFile.getAbsolutePath());
		try {
			final Connection connection = DriverManager.getConnection("jdbc:hsqldb:file:" + idxFile.getAbsolutePath() + ";shutdown=true");
			connection.setAutoCommit(false);
			initDatabase(connection, create);
			return connection;
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}

	private static void initDatabase(final Connection conn, final boolean forceDrop) throws SQLException {
		if (!forceDrop) {
			try {
				final ResultSet versionSet = conn.prepareStatement("SELECT number FROM version").executeQuery();
				if (versionSet.next() && versionSet.getInt(1) == correctVersion) {
					return;
				}
			} catch (final SQLException e) {
				System.err.println(e.getMessage());
			}
			/* invalid version. We probably have to make a new database */
		}

		final ResultSet tables = conn.getMetaData().getTables(null, null, null, new String[] { "TABLE", "VIEW" });
		while (tables.next()) {
			try {
				String schema = tables.getString("TABLE_SCHEM");
				if (schema == null) {
					schema = "";
				}
				final String name = tables.getString("TABLE_NAME");
				conn.prepareStatement("DROP TABLE " + ("".equals(schema) ? "" : schema + ".") + name).execute();
			} catch (final SQLException e) {
				throw new RuntimeException(e);
			}
		}
		
		conn.prepareStatement("SET PROPERTY \"hsqldb.cache_scale\" 9").execute();
		
		/* store current version */
		conn.prepareStatement("CREATE CACHED TABLE version (number INTEGER)").execute();

		StringBuilder create = new StringBuilder(
				"CREATE CACHED TABLE collection (\"FULL_PATH\" varchar(256), \"FILE_NAME\" varchar(256)");
		for (final String s : SidTuneInfoCache.SIDTUNE_INFOS) {
			create.append(",  \"" + s + "\" varchar(256)");
		}
		create.append(", PRIMARY KEY (\"FULL_PATH\"))");
		conn.prepareStatement(create.toString()).execute();

		create = new StringBuilder(
				"CREATE CACHED TABLE stil (\"FULL_PATH\" varchar(256)");
		for (final String s : STIL.STIL_INFOS) {
			create.append(", \"" + s + "\" varchar(2048)");
		}
		create.append(")");
		conn.prepareStatement(create.toString()).execute();
		
		conn.commit();
	}

	public void searchStart() {
	}

	public void searchHit(final File matchFile) {
		count ++;
		try {
			final String relativePath = makeRelative(matchFile);
			collectionInsertStatement.setString(1, relativePath);
			collectionInsertStatement.setString(2, matchFile.getName());
			final Object[] infos = matchFile.isFile() ? infoCache.getInfo(matchFile) : null;
			for (int i = 0; i < SidTuneInfoCache.SIDTUNE_INFOS.length; i++) {
				collectionInsertStatement.setString(i + 3, infos != null && infos[i] != null ? infos[i].toString() : null);
			}

			final STILEntry stilEntry = getSTIL(matchFile);
			/* get STIL Global Comment */
			String glbComment = stilEntry != null ? stilEntry.globalComment
					: null;
			if (stilEntry != null) {
				/* add tune infos */
				addSTILInfo(relativePath, glbComment, stilEntry.infos);

				/* go through subsongs & add them as well */
				for (final TuneEntry entry : stilEntry.subtunes) {
					addSTILInfo(relativePath, glbComment, entry.infos);
				}
			}

			collectionInsertStatement.execute();
		
			if ((count % 2000) == 0) {
				conn.commit();
				PreparedStatement prepareStatement = conn.prepareStatement("CHECKPOINT");
				try {
					prepareStatement.execute();
				} catch (SQLException e) {
					throw e;
				} finally {
					prepareStatement.close();
				}
			}
		} catch (final Exception e) {
			System.err.println("Indexing failure on: " + matchFile.getAbsolutePath()
					+ ": " + e.getMessage());
		}
	}

	private STILEntry getSTIL(final File file) {
		final String name = config.getHVSCName(file);
		if (null != name) {
			STIL stil = STIL.getInstance(config.sidplay2().getHvsc());
			if (stil != null) {
				return stil.getSTIL(name);
			}
		}
		return null;
	}

	private void addSTILInfo(final String relativePath,
			final String glbComment, final ArrayList<Info> infos)
	throws SQLException {
		stilInsertStatement.setString(1, relativePath);
		for (final Info info : infos) {
			stilInsertStatement.setString(2, glbComment);
			stilInsertStatement.setString(3, info.name);
			stilInsertStatement.setString(4, info.author);
			stilInsertStatement.setString(5, info.title);
			stilInsertStatement.setString(6, info.artist);
			stilInsertStatement.setString(7, info.comment);
		}
		stilInsertStatement.execute();
	}

	private String makeRelative(final File matchFile) throws Exception {
		if (matchFile instanceof ZipEntryFileProxy) {
			final String path = ((ZipEntryFileProxy) matchFile).getPath();
			return path.substring(path.indexOf('/') + 1);
		}
		final String canonicalPath = matchFile.getCanonicalPath();
		final String rootCanonicalPath = root.getCanonicalPath();
		if (canonicalPath.startsWith(rootCanonicalPath)) {
			final String name = canonicalPath.substring(rootCanonicalPath.length()).replace(File.separatorChar, '/');
			if (name.startsWith("/")) {
				return name.substring(1);
			}
		}
		return matchFile.getCanonicalPath();
	}

	public void searchStop(final boolean canceled) {
		try {
			if (!canceled) {
				PreparedStatement prepareStatement = conn.prepareStatement("INSERT INTO version VALUES (" + correctVersion + ")");
				prepareStatement.execute();
				prepareStatement.close();
				conn.commit();
				prepareStatement = conn.prepareStatement("CHECKPOINT DEFRAG");
				prepareStatement.execute();
				prepareStatement.close();
			} else {
				conn.rollback();
			}
		} catch (final SQLException e) {
			throw new RuntimeException(e);
		}
	}
}