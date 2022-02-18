package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.CleanupPlayerTimerTask.update;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Properties;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.components.keyboard.KeyTableEntry;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class PressKeyServlet extends JSIDPlay2Servlet {

	public static final String PRESS_KEY_PATH = "/press_key";

	public PressKeyServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + PRESS_KEY_PATH;
	}

	/**
	 * Press key.
	 * 
	 * Press key for Player running as a RTMP live video stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&type=KEY
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&press=KEY
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&release=KEY
	 * }
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			UUID uuid = UUID.fromString(request.getParameter("name"));

			if (request.getParameter("type") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("type"));
				info("typeKey: RTMP stream of: " + uuid + ", " + key.name());
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.typeKey(key));
			} else if (request.getParameter("press") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("press"));
				info("pressKey: RTMP stream of: " + uuid + ", " + key.name());
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.pressKey(key));
			} else if (request.getParameter("release") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("release"));
				info("releaseKey: RTMP stream of: " + uuid + ", " + key.name());
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.releaseKey(key));
			}
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
	}

}
