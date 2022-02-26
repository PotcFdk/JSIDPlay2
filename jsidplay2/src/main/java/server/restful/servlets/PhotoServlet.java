package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JPG;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jsidplay2.Photos;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class PhotoServlet extends JSIDPlay2Servlet {

	public static final String PHOTO_PATH = "/photo";

	public PhotoServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + PHOTO_PATH;
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
		super.doGet(request);
		try {
			String filePath = request.getPathInfo();
			response.setContentType(MIME_TYPE_JPG.toString());
			File absoluteFile = getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			byte[] photo = getPhoto(configuration.getSidplay2Section().getHvsc(), absoluteFile);
			if (photo == null) {
				throw new FileNotFoundException(filePath + " (No such file or directory)");
			}
			response.getOutputStream().write(photo);
			response.setContentLength(photo.length);
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintWriter(response.getWriter()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private byte[] getPhoto(File hvscRoot, File tuneFile) throws IOException, SidTuneError {
		String collectionName = null;
		if (hvscRoot != null && tuneFile.getParentFile() != null) {
			collectionName = PathUtils.getCollectionName(hvscRoot, tuneFile.getParentFile());
		}
		SidTuneInfo info = SidTune.load(tuneFile).getInfo();
		String author = null;
		if (info.getInfoString().size() > 1) {
			Iterator<String> iterator = info.getInfoString().iterator();
			/* title = */iterator.next();
			author = iterator.next();
		}
		return Photos.getPhoto(collectionName, author);
	}

}