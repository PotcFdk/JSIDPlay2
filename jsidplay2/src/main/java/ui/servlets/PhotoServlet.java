package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_OCTET_STREAM;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;
import ui.musiccollection.SidAuthors;

public class PhotoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_PHOTO = "/photo";

	private ServletUtil util;

	public PhotoServlet(Configuration configuration) {
		this.util = new ServletUtil(configuration);
	}

	/**
	 * Get photo of composer.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/photo/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = java.net.URLDecoder.decode(request.getRequestURI(), "UTF-8")
				.substring(request.getRequestURI().indexOf(SERVLET_PATH_PHOTO) + SERVLET_PATH_PHOTO.length());

		try (ServletOutputStream out = response.getOutputStream()) {
			byte[] photo = getPhoto(SidTune.load(util.getAbsoluteFile(filePath, request.getUserPrincipal())));
			if (photo != null) {
				out.write(photo);
			}
			response.setContentType(MIME_TYPE_OCTET_STREAM);
			response.setContentLength(photo != null ? photo.length : 0);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (SidTuneError e) {
			throw new ServletException(e.getMessage());
		}
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