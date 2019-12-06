package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.MimeType;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class StaticServlet extends JSIDPlay2Servlet {

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

		try (InputStream source = getResourceAsStream(filePath)) {
			response.setContentType(MimeType.getMimeType(PathUtils.getFilenameSuffix(filePath)).getContentType());
			response.getWriter().println(ZipFileUtils.convertStreamToString(source, "UTF-8"));
		} catch (IOException e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private InputStream getResourceAsStream(String filePath) throws FileNotFoundException {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		File localFile = new File(util.getConfiguration().getSidplay2Section().getTmpDir(), filePath);
		if (localFile.exists() && localFile.canRead()) {
			return new FileInputStream(localFile);
		}
		InputStream resourceAsStream = JSIDPlay2Server.class.getResourceAsStream("/server/restful/webapp/" + filePath);
		if (resourceAsStream == null) {
			throw new FileNotFoundException("Resource not found: " + filePath);
		}
		return resourceAsStream;
	}

}
