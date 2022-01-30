package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.CleanupPlayerTimerTask.PLAYER_MAP;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.PrintStream;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Properties;
import java.util.UUID;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.RTMPPlayerStatus;
import sidplay.Player;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class OnPlayServlet extends JSIDPlay2Servlet {

	public static final String ON_PLAY_PATH = "/on_play";

	public OnPlayServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + ON_PLAY_PATH;
	}

	/**
	 * Stop Video streaming.
	 * 
	 * Implements RTMP directive on_play configured in nginx.conf.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/on_play
	 * } Example parameters:
	 * 
	 * <pre>
	 * app=live
	 * flashver=LNX &lt;version&gt;
	 * swfurl=
	 * tcurl=rtmp://haendel.ddns.net:1935/live
	 * pageurl=
	 * addr=&lt;client-ip-address&gt;
	 * clientid=25
	 * call=play
	 * name=&lt;UUID&gt;
	 * start=4294965296
	 * duration=0
	 * reset=0
	 * </pre>
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			UUID uuid = UUID.fromString(String.join("", request.getParameterMap().get("name")));
			SimpleImmutableEntry<Player, RTMPPlayerStatus> playerWithStatus = PLAYER_MAP.get(uuid);
			if (playerWithStatus != null) {
				info("onPlay: RTMP stream of: " + uuid);

				Player player = playerWithStatus.getKey();
				PLAYER_MAP.put(uuid, new SimpleImmutableEntry<>(player, RTMPPlayerStatus.ON_PLAY));
			}
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
	}
}
