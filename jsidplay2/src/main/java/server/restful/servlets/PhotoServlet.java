package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JPG;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jsidplay2.photos.SidAuthors;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;

@SuppressWarnings("serial")
public class PhotoServlet extends JSIDPlay2Servlet {

	public PhotoServlet(ServletUtil servletUtil) {
		super(servletUtil);
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
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getPathInfo();
		try {
			response.setContentType(MIME_TYPE_JPG.toString());
			File absoluteFile = getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			byte[] photo = getPhoto(SidTune.load(absoluteFile));
			if (photo == null) {
				throw new FileNotFoundException(filePath + " (No such file or directory)");
			}
			response.getOutputStream().write(photo);
			response.setContentLength(photo.length);
		} catch (Exception e) {
			response.setContentType(MIME_TYPE_TEXT.toString());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private byte[] getPhoto(SidTune tune) {
		if (tune.getInfo().getInfoString().size() > 1) {
			Iterator<String> iterator = tune.getInfo().getInfoString().iterator();
			/* title = */iterator.next();
			String author = iterator.next();
			return SidAuthors.getImageData(author);
		}
		return null;
	}

}