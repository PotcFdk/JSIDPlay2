package server.restful.servlets;

import static server.restful.JSIDPlay2Server.ROLE_ADMIN;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
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
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.ContentType;
import server.restful.common.ServletUtil;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.MP3Driver.MP3Stream;
import sidplay.consoleplayer.ParameterTimeConverter;
import sidplay.audio.MP4Driver;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.entities.config.Configuration;

@Parameters(resourceBundle = "server.restful.servlets.ConvertServlet")
public class ConvertServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SERVLET_PATH_CONVERT = "/convert";

	@Parameter(names = { "--startTime", "-t" }, descriptionKey = "START_TIME", converter = ParameterTimeConverter.class)
	private Integer startTime;

	private ServletUtil util;

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	/**
	 * Stream SID as MP3.
	 * 
	 * E.g. stream audio
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	 * 
	 * E.g. stream video
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/Demos/ALGODANCER2/ALGODANCER2.d64?defaultLength=00:30&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String decodedPath = URIUtil.decodePath(request.getRequestURI());
		String filePath = decodedPath
				.substring(decodedPath.indexOf(SERVLET_PATH_CONVERT) + SERVLET_PATH_CONVERT.length());
		File file = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));

		if (filePath.toLowerCase(Locale.ENGLISH).endsWith(".mp3")) {
			try {
				response.setContentType(ContentType.MIME_TYPE_MPEG.getContentType());
				ZipFileUtils.copy(file, response.getOutputStream());
				response.addHeader("Content-Disposition", "attachment; filename=" + new File(filePath).getName());
			} catch (Exception e) {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".sid")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".dat")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".str")) {
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
				convertAudio(config, file, driver);
			} catch (Exception e) {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			File mp4File = null;
			try {
				response.setContentType(ContentType.MIME_TYPE_MP4.getContentType());
				IConfig config = new IniConfig(false, null);
				MP4Driver driver = new MP4Driver();
				JCommander commander = JCommander.newBuilder().addObject(this).addObject(config).addObject(driver)
						.programName(getClass().getName()).build();
				String[] args = Collections.list(request.getParameterNames()).stream()
						.map(name -> Arrays.asList("--" + name,
								Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
						.flatMap(List::stream).collect(Collectors.toList()).toArray(new String[0]);
				commander.parse(args);
				mp4File = convertVideo(config, file, driver);
				ZipFileUtils.copy(mp4File, response.getOutputStream());
			} catch (Exception e) {
				response.setContentType(MimeTypes.Type.TEXT_PLAIN_UTF_8.asString());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			} finally {
				if (mp4File != null) {
					mp4File.delete();
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	private void convertAudio(IConfig config, File file, AudioDriver driver) throws IOException, SidTuneError {
		Player player = new Player(config);
		String root = util.getConfiguration().getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		player.play(SidTune.load(file));
		player.stopC64(false);
	}

	private File convertVideo(IConfig config, File file, AudioDriver driver)
			throws IOException, SidTuneError, URISyntaxException {
		Player player = new Player(config);
		File mp4File = File.createTempFile("jsidplay2video", ".mp4");
		player.setRecordingFilenameProvider(tune -> mp4File.getAbsolutePath());
		player.setAudioDriver(driver);
		player.getTimer().setStart(startTime);
		File extractedFile = File.createTempFile("jsidplay2autostart", PathUtils.getFilenameSuffix(file.getName()));
		try (OutputStream d64OutputStream = new FileOutputStream(extractedFile)) {
			ZipFileUtils.copy(file, d64OutputStream);
		}
		new Convenience(player).autostart(extractedFile, Convenience.LEXICALLY_FIRST_MEDIA, null);
		extractedFile.delete();
		player.stopC64(false);
		return mp4File;
	}
}
