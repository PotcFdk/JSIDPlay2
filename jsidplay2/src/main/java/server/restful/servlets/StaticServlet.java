package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;

@SuppressWarnings("serial")
public class StaticServlet extends JSIDPlay2Servlet {

	public StaticServlet(ServletUtil servletUtil) {
		super(servletUtil);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC;
	}

	/**
	 * Get VUE web page.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/hvsc.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getPathInfo();
		try (InputStream source = getResourceAsStream(filePath)) {
			response.setContentType(getMimeType(PathUtils.getFilenameSuffix(filePath)).toString());
			response.getWriter().println(ZipFileUtils.convertStreamToString(source, "UTF-8"));
		} catch (IOException e) {
			response.setContentType(MIME_TYPE_TEXT.toString());
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
			throw new FileNotFoundException(filePath + " (No such file or directory)");
		}
		return resourceAsStream;
	}

}
