package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.fingerprinting.rest.beans.HashBean;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class FindHashServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public FindHashServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/hash";
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		IntArrayBean intArray = getInput(request, IntArrayBean.class);
		int[] hashes = intArray.getHash();

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

			int len = hashes.length;

			StringBuilder exec = new StringBuilder();
			exec.append("SELECT * FROM `HashTable` WHERE Hash in(");
			for (int i = 0; i < len; i++) {
				exec.append(hashes[i]).append(",");
			}

			HashBeans result = new HashBeans();
			result.setHashes(new ArrayList<>());
			if (len > 0) {
				len = exec.length();
				exec.replace(len - 1, len, ");");

				ResultSet rs = dbStatement.executeQuery(exec.toString());
				if (rs != null) {
					while (rs.next()) {
						HashBean hashBean = new HashBean();
						hashBean.setHash(rs.getInt(2));
						hashBean.setId(rs.getInt(3));
						hashBean.setTime(rs.getInt(4));
						result.getHashes().add(hashBean);
					}
				}
			}
			dbConn.close();

			setOutput(request, response, result, HashBeans.class);
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}
}
