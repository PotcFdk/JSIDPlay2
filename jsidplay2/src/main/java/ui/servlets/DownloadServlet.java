package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_MPEG;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_OCTET_STREAM;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_SID;
import static ui.servlets.JSIDPlay2Server.MIME_TYPE_JPG;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;

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
		String filePath = java.net.URLDecoder.decode(request.getRequestURI(), "UTF-8")
				.substring(request.getRequestURI().indexOf(SERVLET_PATH_DOWNLOAD) + SERVLET_PATH_DOWNLOAD.length());

		response.setContentType(filePath.endsWith(".mp3") ? MIME_TYPE_MPEG
				: filePath.endsWith(".sid") ? MIME_TYPE_SID : (filePath.endsWith(".jpg") ? MIME_TYPE_JPG : MIME_TYPE_OCTET_STREAM));

		try {
			ZipFileUtils.copy(util.getAbsoluteFile(filePath, request.getUserPrincipal()), response.getOutputStream());
			response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());
		} catch (Exception e) {
			response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
			response.getOutputStream().println(e.getMessage());
		} finally {
			response.getOutputStream().flush();
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
