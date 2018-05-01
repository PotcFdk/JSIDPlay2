package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JSON;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import ui.entities.config.Configuration;

public class DirectoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH = "/directory";

	private ServletUtil util;

	public DirectoryServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Get directory contents containing music collections.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = java.net.URLDecoder.decode(request.getRequestURI(), "UTF-8")
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());
		String filter = request.getParameter("filter");
		
		List<String> files = util.getDirectory(filePath, filter, request.getUserPrincipal());

		response.setContentType(MIME_TYPE_JSON);
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(files));
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
