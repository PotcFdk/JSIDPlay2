package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidutils.ZipFileUtils.convertStreamToString;
import static server.restful.common.MimeType.MIME_TYPE_HTML;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import libsidplay.sidtune.SidTune;
import server.restful.common.Connectors;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;

@SuppressWarnings("serial")
public class StartPageServlet extends JSIDPlay2Servlet {

	private ServletUtil util;

	public StartPageServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return "/";
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		EmulationSection emulationSection = util.getConfiguration().getEmulationSection();

		Connectors appServerConnectors = emulationSection.getAppServerConnectors();
		String preferredProtocol = appServerConnectors.getPreferredProtocol();
		String hostname = InetAddress.getLocalHost().getHostAddress();
		String portNum = String.valueOf(appServerConnectors.getPortNum());
		Map<String, String> replacements = new HashMap<>();
		replacements.put("https://haendel.ddns.net:8443", preferredProtocol + "://" + hostname + ":" + portNum);

		try (InputStream is = SidTune.class.getResourceAsStream("/doc/restful.html")) {
			if (is != null) {
				response.setContentType(MIME_TYPE_HTML.getContentType());
				response.getWriter().println(is != null ? convertStreamToString(is, UTF_8.name(), replacements) : "");
			} else {
				response.setContentType(MIME_TYPE_TEXT.getContentType());
				new PrintStream(response.getOutputStream()).print("File not found: /doc/restful.html");
			}
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
