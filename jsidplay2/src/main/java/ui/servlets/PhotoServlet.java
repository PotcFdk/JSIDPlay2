package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_OCTET_STREAM;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import ui.entities.config.Configuration;

public class PhotoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH = "/photo";

	/*
	 * Contains a mapping: Author to picture resource path.
	 */
	private static final Properties SID_AUTHORS = new Properties();

	static {
		try (InputStream is = SidTune.class.getResourceAsStream("pictures.properties")) {
			SID_AUTHORS.load(is);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

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
		String filePath = request.getRequestURI()
				.substring(request.getRequestURI().indexOf(SERVLET_PATH) + SERVLET_PATH.length());

		try (ServletOutputStream out = response.getOutputStream()) {
			byte[] photo = getPhoto(filePath);
			out.write(photo);

			response.setContentType(MIME_TYPE_OCTET_STREAM);
			response.setContentLength(photo.length);
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (SidTuneError e) {
			throw new ServletException(e.getMessage());
		}
	}

	private byte[] getPhoto(String resource) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(util.getAbsoluteFile(resource));
		if (tune != null) {
			Collection<String> infos = tune.getInfo().getInfoString();
			if (infos.size() > 1) {
				Iterator<String> iterator = infos.iterator();
				/* title = */iterator.next();
				String author = iterator.next();
				String photoResource = SID_AUTHORS.getProperty(author);
				if (photoResource != null) {
					URL us = SidTune.class.getResource("Photos/" + photoResource);
					byte[] photo = new byte[us.openConnection().getContentLength()];
					try (DataInputStream is = new DataInputStream(
							SidTune.class.getResourceAsStream("Photos/" + photoResource))) {
						is.readFully(photo);
						return photo;
					}
				}
			}
		}
		return new byte[0];
	}

}