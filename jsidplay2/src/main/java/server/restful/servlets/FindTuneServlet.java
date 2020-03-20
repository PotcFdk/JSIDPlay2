package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class FindTuneServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public FindTuneServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/tune";
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		SongNoBean songNoBean = getInput(request, SongNoBean.class);

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

			MusicInfoBean musicInfoBean = new MusicInfoBean();
			try (ResultSet resultSet = dbStatement
					.executeQuery("SELECT Title, Artist, Album, audio_length FROM `MusicInfo` WHERE idMusicInfo="
							+ songNoBean.getSongNo())) {
				while (resultSet.next()) {
					musicInfoBean.setTitle(resultSet.getString(1));
					musicInfoBean.setArtist(resultSet.getString(2));
					musicInfoBean.setAlbum(resultSet.getString(3));
					musicInfoBean.setAudioLength(resultSet.getDouble(4));
					break;
				}
			}
			dbConn.close();

			setOutput(request, response, musicInfoBean, MusicInfoBean.class);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
