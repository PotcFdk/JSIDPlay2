package ui.servlets;

import static ui.servlets.JSIDPlay2Server.MIME_TYPE_OCTET_STREAM;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

	public byte[] getPhoto(String resource) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(util.getAbsoluteFile(resource));
		if (tune != null) {
			Collection<String> infos = tune.getInfo().getInfoString();
			if (infos.size() > 1) {
				Iterator<String> iterator = infos.iterator();
				/* title = */iterator.next();
				String author = iterator.next();
				String photo = SID_AUTHORS.getProperty(author);
				if (photo != null) {
					ByteArrayOutputStream s = new ByteArrayOutputStream();
					try (InputStream is = SidTune.class.getResourceAsStream("Photos/" + photo)) {
						int n = 1;
						while (n > 0) {
							byte[] b = new byte[4096];
							n = is.read(b);
							if (n > 0)
								s.write(b, 0, n);
						}
					}
					return s.toByteArray();
				}
			}
		}
		return new byte[0];
	}

}