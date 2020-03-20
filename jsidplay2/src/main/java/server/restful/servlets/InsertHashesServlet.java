package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class InsertHashesServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public InsertHashesServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/insert-hashes";
	}

	@Override
	protected void doPut(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		HashBeans hashes = getInput(request, HashBeans.class);
		List<HashBean> hashBeans = hashes.getHashes();

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

			StringBuilder buf = new StringBuilder("INSERT INTO `HashTable` " + "(`Hash`, `id`, `Time`) " + "VALUES");

			for (HashBean hashBean : hashBeans) {
				buf.append("(").append(hashBean.getHash()).append(",").append(hashBean.getId()).append(",")
						.append(hashBean.getTime()).append("),");
			}
			if (hashBeans.size() > 0) {
				buf.replace(buf.length() - 1, buf.length(), ";");

				dbStatement.execute(buf.toString());
			}
			dbConn.close();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
