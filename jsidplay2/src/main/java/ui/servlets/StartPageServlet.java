package ui.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;

import libsidplay.sidtune.SidTune;
import libsidutils.ZipFileUtils;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;

public class StartPageServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_STARTPAGE = "/";

	private ServletUtil util;

	public StartPageServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
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
				response.setContentType(MimeTypes.Type.TEXT_HTML_UTF_8.asString());
				response.getWriter()
						.println(is != null ? ZipFileUtils.convertStreamToString(is, "UTF-8", replacements) : "");
			} else {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				new PrintStream(response.getOutputStream()).print("File not found: /doc/restful.html");
			}
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
