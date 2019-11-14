package server.restful.servlets;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

public class PlayerServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_PLAYER = "/player";

	private ServletUtil util;

	public PlayerServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			request.setAttribute("favorites", util.getConfiguration().getFavorites());
			request.getRequestDispatcher("/player.jsp").forward(request, response);
		} catch (Exception e) {

		}
	}
}
