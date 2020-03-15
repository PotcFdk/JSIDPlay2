package sidplay.audio.whatssid.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import sidplay.audio.whatssid.FingerprintedSampleData;
import sidplay.audio.whatssid.fingerprint.Fingerprint;
import sidplay.audio.whatssid.fingerprint.Link;

/**
 * Created by hsyecheng on 2015/6/12.
 */
public class MysqlDB {

	private final String DRIVER = "com.mysql.jdbc.Driver";
	private final String CREATE_MUSIC_INFO_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `MusicInfo` ("
			+ "`id` INT NOT NULL AUTO_INCREMENT," + "`Title` VARCHAR(200)," + "`Artist` VARCHAR(200),"
			+ "`Album` VARCHAR(200)," + "`FileDir` VARCHAR(400) NULL," + "`InfoDir` VARCHAR(400) NULL,"
			+ "`audio_length` double," + "PRIMARY KEY (`id`)," + "UNIQUE INDEX `id_UNIQUE` (`id` ASC))"
			+ "ENGINE = InnoDB;";
	private final String CREATE_HASH_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `HashTable` ("
			+ "`idHashTable` INT NOT NULL AUTO_INCREMENT," + "`Hash` INT NOT NULL," + "`MusicInfo_id` INT NOT NULL,"
			+ "`Time` INT NOT NULL," + "PRIMARY KEY (`idHashTable`),"
			+ "FOREIGN KEY  (`MusicInfo_id`) REFERENCES `MusicInfo` (`id`) ON DELETE CASCADE,"
			+ "INDEX `Hash` (`Hash` ASC)  " + "KEY_BLOCK_SIZE=1)" + "ENGINE = InnoDB;";
	private final String INSERT_SONG_QUERY = "INSERT INTO `MusicInfo` "
			+ "(`Title`, `Artist`, `Album`, `FileDir`, `InfoDir`, audio_length) " + "VALUES ( ? , ? , ? , ? , ? , ? );";

	private Connection dbConn;
	private Statement dbStatement;
	private PreparedStatement insertMusic;

	public MysqlDB() {
		try {
			Class.forName(DRIVER);

			String database = System.getenv().getOrDefault("YECHENG_DATABASE_NAME", "musiclibary");
			int port = Integer.parseInt(System.getenv().getOrDefault("YECHENG_DATABASE_PORT", "3306"));
			String host = System.getenv().getOrDefault("YECHENG_DATABASE_HOST", "localhost");
			String user = System.getenv().getOrDefault("YECHENG_DATABASE_USER", "newuser");
			String password = System.getenv().getOrDefault("YECHENG_DATABASE_PASS", "password");

			dbConn = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user,
					user, password);
			if (dbConn.isClosed()) {
				throw new RuntimeException("can not open Database");
			}
			dbStatement = dbConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			dbStatement.setFetchSize(Integer.MIN_VALUE);

			insertMusic = dbConn.prepareStatement(INSERT_SONG_QUERY, Statement.RETURN_GENERATED_KEYS);
			dbConn.createStatement().executeUpdate(CREATE_MUSIC_INFO_TABLE_QUERY);
			dbConn.createStatement().executeUpdate(CREATE_HASH_TABLE_QUERY);
		} catch (SQLException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized void insert(FingerprintedSampleData fingerprintedSampleData, String fileName) {
		try {
			insertMusic.setString(1, fingerprintedSampleData.getTitle());
			insertMusic.setString(2, fingerprintedSampleData.getArtist());
			insertMusic.setString(3, fingerprintedSampleData.getAlbum());
			insertMusic.setString(4, fileName);
			insertMusic.setString(5, "");
			insertMusic.setDouble(6, fingerprintedSampleData.getAudioLength());
			insertMusic.executeUpdate();

			int id;
			try (ResultSet rs = insertMusic.getGeneratedKeys()) {
				rs.next();
				id = rs.getInt(1);
			}

			StringBuilder buf = new StringBuilder("INSERT INTO `HashTable` " + "(`Hash`, `id`, `Time`) " + "VALUES");

			Fingerprint fp = fingerprintedSampleData.getFingerprint();
			for (Link link : fp.getLinkList()) {
				Info info = new Info(id, link);
				buf.append("(").append(info.hash).append(",").append(info.id).append(",").append(info.time)
						.append("),");
			}
			if (fp.getLinkList().size() > 0) {
				buf.replace(buf.length() - 1, buf.length(), ";");

				dbStatement.execute(buf.toString());
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ResultSet search(int hash) {
		try {
			return dbStatement.executeQuery("SELECT * from `HashTable` WHERE Hash=" + hash + ";");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ResultSet searchAll(int[] hash) {
		try {
			int len = hash.length;

			StringBuilder exec = new StringBuilder();
			exec.append("SELECT * FROM `HashTable` WHERE Hash in(");
			for (int i = 0; i < len; i++) {
				exec.append(hash[i]).append(",");
			}
			if (len > 0) {
				len = exec.length();
				exec.replace(len - 1, len, ");");

				return dbStatement.executeQuery(exec.toString());
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized ResultSet listAll() {
		try {
			return dbStatement.executeQuery("SELECT * FROM `HashTable`");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized DBMatch getByID(int id) {
		try {
			ResultSet resultSet = dbStatement
					.executeQuery("SELECT Title, Artist, Album, audio_length FROM `MusicInfo` WHERE idMusicInfo=" + id);
			while (resultSet.next()) {
				DBMatch dbMatch = new DBMatch();
				dbMatch.setTitle(resultSet.getString(1));
				dbMatch.setArtist(resultSet.getString(2));
				dbMatch.setAlbum(resultSet.getString(3));
				dbMatch.setAudioLength(resultSet.getDouble(4));
				return dbMatch;
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
