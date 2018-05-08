package ui.servlets;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;

import libsidplay.sidtune.SidTune;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;

public class StartPageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_STARTPAGE = "/";

	public StartPageServlet(Configuration configuration) {
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try (InputStream is = SidTune.class.getResourceAsStream("/doc/restful.html")) {
			response.setContentType(MimeTypes.Type.TEXT_HTML_UTF_8.asString());
			response.getWriter().println(ZipFileUtils.convertStreamToString(is, "UTF-8"));
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

}
