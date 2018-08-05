package ui.servlets;

import static ui.servlets.JSIDPlay2Server.ROLE_ADMIN;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.URIUtil;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;
import ui.musiccollection.SidAuthors;

public class PhotoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_PHOTO = "/photo";

	private ServletUtil util;

	public PhotoServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	/**
	 * Get photo of composer.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/photo/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URIUtil.decodePath(request.getRequestURI());
		String filePath = decodedPath.substring(decodedPath.indexOf(SERVLET_PATH_PHOTO) + SERVLET_PATH_PHOTO.length());

		try {
			response.setContentType(ContentType.MIME_TYPE_JPG.getContentType());
			File absoluteFile = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			byte[] photo = getPhoto(SidTune.load(absoluteFile));
			if (photo == null) {
				throw new FileNotFoundException(
						absoluteFile.getAbsolutePath() + " (Datei oder Verzeichnis nicht gefunden)");
			}
			response.getOutputStream().write(photo);
			response.setContentLength(photo.length);
		} catch (Exception e) {
			response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
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