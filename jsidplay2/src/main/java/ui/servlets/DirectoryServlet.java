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

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());
		String filter = request.getParameter("filter");
		List<String> files = util.getDirectory(filePath, filter);

		ObjectMapper mapper = new ObjectMapper();
		response.setContentType(MIME_TYPE_JSON);
		response.getWriter().println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(files));

		response.setStatus(HttpServletResponse.SC_OK);
	}

}
