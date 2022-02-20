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
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.RTMPPlayerWithStatus;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class SetDefaultEmulationReSidServlet extends JSIDPlay2Servlet {

	public static final String SET_DEFAULT_EMULATION_RESID_PATH = "/set_default_emulation_resid";

	public SetDefaultEmulationReSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + SET_DEFAULT_EMULATION_RESID_PATH;
	}

	/**
	 * Set default emulation to RESIDFP for Player running as a RTMP live video
	 * stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/set_default_emulation_resid?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			UUID uuid = UUID.fromString(request.getParameter("name"));

			info(String.format("setDefaultEmulationReSid: RTMP stream of: %s", uuid));
			update(uuid, RTMPPlayerWithStatus::setDefaultEmulationReSid);
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
	}

}
