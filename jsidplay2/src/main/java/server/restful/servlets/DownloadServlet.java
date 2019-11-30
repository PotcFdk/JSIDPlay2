package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;
import static server.restful.common.MimeType.getMimeType;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DownloadServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public DownloadServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return "/download";
	}

	/**
	 * Download SID.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath.substring(decodedPath.indexOf(getServletPath()) + getServletPath().length());

		try {
			response.setContentType(getMimeType(getFilenameSuffix(filePath)).getContentType());
			copy(util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN)), response.getOutputStream());
			response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + new File(filePath).getName());
		} catch (Exception e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
