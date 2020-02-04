package server.restful.servlets;

import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;
import static server.restful.common.MimeType.getMimeType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.beust.jcommander.JCommander;

import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.MimeType;
import server.restful.common.ServletUtil;
import sidplay.Player;
import sidplay.audio.AVIDriver;
import sidplay.audio.Audio;
import sidplay.audio.AudioDriver;
import sidplay.audio.MP3Driver.MP3Stream;
import sidplay.audio.MP4Driver;
import sidplay.audio.WAVDriver.WAVStream;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.entities.config.Configuration;
import ui.filefilter.CartFileFilter;
import ui.filefilter.DiskFileFilter;
import ui.filefilter.TapeFileFilter;
import ui.filefilter.TuneFileFilter;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	private static final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	private ServletUtil util;

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		this.util = new ServletUtil(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + "/convert";
	}

	/**
	 * Stream SID as MP3.
	 * 
	 * <BR>
	 * E.g. stream audio<BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	 * } <BR>
	 * E.g. stream video<BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/Demos/ALGODANCER2/ALGODANCER2.d64?defaultLength=00:30&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true
	 * }
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String filePath = request.getPathInfo();
		File file = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
		try {
			if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".sid")
					|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".dat")
					|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mus")
					|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".str")) {
				IConfig config = new IniConfig(false, null);

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(config).programName(getClass().getName()).build().parse(args);

				AudioDriver driver = getAudioDriverOfAudioFormat(config, response.getOutputStream());

				response.setContentType(MimeType.getMimeType(driver.getExtension()).getContentType());
				convertAudio(config, file, driver);
			} else if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3") && (cartFileFilter.accept(file)
					|| tuneFileFilter.accept(file) || diskFileFilter.accept(file) || tapeFileFilter.accept(file))) {
				IConfig config = new IniConfig(false, null);

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(config).programName(getClass().getName()).build().parse(args);

				AudioDriver driver = getAudioDriverOfVideoFormat(config);

				response.setContentType(MimeType.getMimeType(driver.getExtension()).getContentType());
				File videoFile = convertVideo(config, file, driver);
				copy(videoFile, response.getOutputStream());
				videoFile.delete();
			} else {
				response.setContentType(getMimeType(getFilenameSuffix(filePath)).getContentType());
				response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + new File(filePath).getName());
				copy(file, response.getOutputStream());
			}
		} catch (Exception e) {
			response.setContentType(MIME_TYPE_TEXT.getContentType());
			e.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private String[] getRequestParameters(HttpServletRequest request) {
		return Collections.list(request.getParameterNames()).stream()
				.map(name -> Arrays.asList("--" + name,
						Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
				.flatMap(List::stream).toArray(String[]::new);
	}

	private AudioDriver getAudioDriverOfAudioFormat(IConfig config, OutputStream outputstream) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(Audio.MP3)) {
		case WAV:
			return new WAVStream(outputstream);

		case MP3:
		default:
			return new MP3Stream(outputstream);
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

	private AudioDriver getAudioDriverOfVideoFormat(IConfig config) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(Audio.MP4)) {
		case AVI:
			return new AVIDriver();
		case MP4:
		default:
			return new MP4Driver();
		}
	}

	private File convertVideo(IConfig config, File file, AudioDriver driver)
			throws IOException, SidTuneError, URISyntaxException {
		Player player = new Player(config);
		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(),
				new File(config.getSidplay2Section().getTmpDir()));
		videoFile.deleteOnExit();
		player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		player.setAudioDriver(driver);
		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);
		player.stopC64(false);
		return videoFile;
	}
}
