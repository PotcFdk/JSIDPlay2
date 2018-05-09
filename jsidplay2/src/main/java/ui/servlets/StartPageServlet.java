package ui.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;

import libsidplay.sidtune.SidTune;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;

public class StartPageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_STARTPAGE = "/";

	private ServletUtil util;

	public StartPageServlet(Configuration configuration) {
		util = new ServletUtil(configuration);
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		EmulationSection s = util.getConfiguration().getEmulationSection();
		Map<String, String> replacements = new HashMap<>();
		replacements.put("${port}", String.valueOf(s.getAppServerPort()));
		
		try (InputStream is = SidTune.class.getResourceAsStream("/doc/restful.html")) {
			response.setContentType(MimeTypes.Type.TEXT_HTML_UTF_8.asString());
			response.getWriter().println(ZipFileUtils.convertStreamToString(is, "UTF-8", replacements));
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

}
