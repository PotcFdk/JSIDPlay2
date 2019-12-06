package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_JPG;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import jsidplay2.photos.SidAuthors;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class PhotoServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public PhotoServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/photo";
	}

	/**
	 * Get photo of composer.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/photo/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath.substring(decodedPath.indexOf(getServletPath()) + getServletPath().length());

		try {
			response.setContentType(MIME_TYPE_JPG.getContentType());
			File absoluteFile = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			byte[] photo = getPhoto(SidTune.load(absoluteFile));
			if (photo == null) {
				throw new FileNotFoundException(filePath + " (No such file or directory)");
			}
			response.getOutputStream().write(photo);
			response.setContentLength(photo.length);
		} catch (Exception e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private byte[] getPhoto(SidTune tune) throws IOException, SidTuneError {
		if (tune.getInfo().getInfoString().size() > 1) {
			Iterator<String> iterator = tune.getInfo().getInfoString().iterator();
			/* title = */iterator.next();
			String author = iterator.next();
			return SidAuthors.getImageData(author);
		}
		return null;
	}

}