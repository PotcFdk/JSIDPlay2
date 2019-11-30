package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_JSON;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

public class DirectoryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_DIRECTORY = "/directory";

	private ServletUtil util;

	public DirectoryServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	/**
	 * Get directory contents containing music collections.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath
				.substring(decodedPath.indexOf(SERVLET_PATH_DIRECTORY) + SERVLET_PATH_DIRECTORY.length());
		String filter = request.getParameter("filter");
		if (filter != null) {
			filter = URLDecoder.decode(filter, UTF_8.name());
		}

		List<String> files = util.getDirectory(filePath, filter, request.isUserInRole(ROLE_ADMIN));

		response.setContentType(MIME_TYPE_JSON.getContentType());
		response.getWriter().println(new ObjectMapper().writer().writeValueAsString(files));
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
