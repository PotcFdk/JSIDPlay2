package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class InsertTuneServlet extends JSIDPlay2Servlet {

	private static final String DRIVER = "com.mysql.jdbc.Driver";

	private static final String INSERT_SONG_QUERY = "INSERT INTO `MusicInfo` "
			+ "(`Title`, `Artist`, `Album`, `FileDir`, `InfoDir`, audio_length) " + "VALUES ( ? , ? , ? , ? , ? , ? );";

	static {
		try {
			Class.forName(DRIVER);
		} catch (ClassNotFoundException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private ServletUtil util;

	public InsertTuneServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/insert-tune";
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		MusicInfoBean musicInfoBean = getInput(request, MusicInfoBean.class);

		try {
			String database = System.getenv().getOrDefault("YECHENG_DATABASE_NAME", "musiclibary");
			int port = Integer.parseInt(System.getenv().getOrDefault("YECHENG_DATABASE_PORT", "3306"));
			String host = System.getenv().getOrDefault("YECHENG_DATABASE_HOST", "localhost");
			String user = System.getenv().getOrDefault("YECHENG_DATABASE_USER", "newuser");
			String password = System.getenv().getOrDefault("YECHENG_DATABASE_PASS", "password");

			Connection dbConn = DriverManager.getConnection(
					"jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user, user, password);
			if (dbConn.isClosed()) {
				throw new RuntimeException("can not open Database");
			}
			Statement dbStatement = dbConn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			dbStatement.setFetchSize(Integer.MIN_VALUE);

			PreparedStatement insertMusic = dbConn.prepareStatement(INSERT_SONG_QUERY, Statement.RETURN_GENERATED_KEYS);

			insertMusic = dbConn.prepareStatement(INSERT_SONG_QUERY, Statement.RETURN_GENERATED_KEYS);

			insertMusic.setString(1, musicInfoBean.getTitle());
			insertMusic.setString(2, musicInfoBean.getArtist());
			insertMusic.setString(3, musicInfoBean.getAlbum());
			insertMusic.setString(4, musicInfoBean.getFileDir());
			insertMusic.setString(5, musicInfoBean.getInfoDir());
			insertMusic.setDouble(6, musicInfoBean.getAudioLength());
			insertMusic.executeUpdate();

			int id;
			try (ResultSet rs = insertMusic.getGeneratedKeys()) {
				rs.next();
				id = rs.getInt(1);
			}

			IdBean result = new IdBean();
			result.setId(id);

			dbConn.close();

			setOutput(request, response, result, IdBean.class);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
