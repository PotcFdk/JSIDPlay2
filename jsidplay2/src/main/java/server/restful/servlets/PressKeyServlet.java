package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.CleanupPlayerTimerTask.pressKey;
import static server.restful.common.CleanupPlayerTimerTask.releaseKey;
import static server.restful.common.CleanupPlayerTimerTask.typeKey;
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
			info("PressKey: RTMP stream of: " + uuid);
			if (request.getParameter("type") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("type"));
				typeKey(uuid, key);
			} else if (request.getParameter("press") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("press"));
				pressKey(uuid, key);
			} else if (request.getParameter("release") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("release"));
				releaseKey(uuid, key);
			}
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
	}

}
