package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.MimeType.MIME_TYPE_HTML;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class StaticServlet extends JSIDPlay2Servlet {

	@SuppressWarnings("unused")
	private ServletUtil util;

	public StaticServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + "";
	}

	/**
	 * Get VUE web page.
	 * 
	 * E.g. http://haendel.ddns.net:8080/static/hvsc.vue
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath.substring(decodedPath.indexOf(getServletPath()) + getServletPath().length());

		try (InputStream source = JSIDPlay2Server.class.getResourceAsStream("/server/restful/webapp" + filePath)) {
			response.setContentType(MIME_TYPE_HTML.getContentType());
			response.getWriter().println(ZipFileUtils.convertStreamToString(source, "UTF-8"));
		} catch (IOException e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
