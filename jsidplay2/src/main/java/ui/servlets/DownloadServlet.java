package ui.servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.URIUtil;

import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;

public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_DOWNLOAD = "/download";

	private ServletUtil util;

	public DownloadServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Download SID.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URIUtil.decodePath(request.getRequestURI());
		String filePath = decodedPath
				.substring(decodedPath.indexOf(SERVLET_PATH_DOWNLOAD) + SERVLET_PATH_DOWNLOAD.length());

		try {
			response.setContentType(ContentType.getContentType(PathUtils.getFilenameSuffix(filePath)).getContentType());
			ZipFileUtils.copy(util.getAbsoluteFile(filePath, request.getUserPrincipal()), response.getOutputStream());
			response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());
		} catch (Exception e) {
			response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		} finally {
			response.getOutputStream().flush();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
