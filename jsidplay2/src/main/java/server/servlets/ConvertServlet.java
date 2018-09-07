package server.servlets;

import static server.JSIDPlay2Server.ROLE_ADMIN;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.util.URIUtil;

import com.beust.jcommander.JCommander;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.ZipFileUtils;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.MP3Driver.MP3Stream;
import sidplay.ini.IniConfig;
import ui.entities.config.Configuration;

public class ConvertServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_CONVERT = "/convert";

	private ServletUtil util;

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	/**
	 * Stream SID as MP3.
	 * 
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URIUtil.decodePath(request.getRequestURI());
		String filePath = decodedPath
				.substring(decodedPath.indexOf(SERVLET_PATH_CONVERT) + SERVLET_PATH_CONVERT.length());

		if (filePath.toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			try {
				response.setContentType(ContentType.MIME_TYPE_MPEG.getContentType());
				ZipFileUtils.copy(util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN)),
						response.getOutputStream());
				response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());
			} catch (Exception e) {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			try {
				response.setContentType(ContentType.MIME_TYPE_MPEG.getContentType());
				IConfig config = new IniConfig(false, null);
				MP3Stream driver = new MP3Stream(response.getOutputStream());
				JCommander commander = JCommander.newBuilder().addObject(config).addObject(driver)
						.programName(getClass().getName()).build();
				String[] args = Collections.list(request.getParameterNames()).stream()
						.map(name -> Arrays.asList("--" + name,
								Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
						.flatMap(List::stream).collect(Collectors.toList()).toArray(new String[0]);
				commander.parse(args);
				convert(config, filePath, driver, request.isUserInRole(ROLE_ADMIN));
			} catch (Exception e) {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	private void convert(IConfig config, String resource, AudioDriver driver, boolean adminRole)
			throws IOException, SidTuneError {
		Player player = new Player(config);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		player.play(SidTune.load(util.getAbsoluteFile(resource, adminRole)));
		player.stopC64(false);
	}

}
