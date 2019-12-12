package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.MimeType.MIME_TYPE_TEXT;
import static server.restful.common.MimeType.getMimeType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
		String decodedPath = URLDecoder.decode(request.getRequestURI(), UTF_8.name());
		String filePath = decodedPath.substring(decodedPath.indexOf(getServletPath()) + getServletPath().length());
		File file = util.getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));

		if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".sid")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".dat")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mus")
				|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".str")) {
			try {
				String[] args = Collections.list(request.getParameterNames()).stream()
						.map(name -> Arrays.asList("--" + name,
								Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
						.flatMap(List::stream).toArray(String[]::new);

				IConfig config = new IniConfig(false, null);

				JCommander commander = JCommander.newBuilder().addObject(config)
						.acceptUnknownOptions(true).programName(getClass().getName()).build();
				commander.parse(args);

				SimpleImmutableEntry<Audio, Class<? extends AudioDriver>> audioAndDriver = getAudioOfAudio(config);
				Audio audio = audioAndDriver.getKey();
				AudioDriver driver = audioAndDriver.getValue().getConstructor(OutputStream.class)
						.newInstance(response.getOutputStream());

				commander = JCommander.newBuilder().addObject(driver)
						.acceptUnknownOptions(true).programName(getClass().getName()).build();
				commander.parse(args);

				response.setContentType(MimeType.getMimeType(audio.getExtension()).getContentType());
				convertAudio(config, file, driver);
			} catch (Exception e) {
				response.setContentType(MIME_TYPE_TEXT.getContentType());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3") && (cartFileFilter.accept(file)
				|| tuneFileFilter.accept(file) || diskFileFilter.accept(file) || tapeFileFilter.accept(file))) {
			File videoFile = null;
			try {
				String[] args = Collections.list(request.getParameterNames()).stream()
						.map(name -> Arrays.asList("--" + name,
								Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
						.flatMap(List::stream).toArray(String[]::new);

				IConfig config = new IniConfig(false, null);

				JCommander commander = JCommander.newBuilder().addObject(config).programName(getClass().getName())
						.build();
				commander.parse(args);

				SimpleImmutableEntry<Audio, Class<? extends AudioDriver>> audioAndDriver = getAudioOfVideo(config);
				Audio audio = audioAndDriver.getKey();
				AudioDriver driver = audioAndDriver.getValue().newInstance();

				commander = JCommander.newBuilder().addObject(driver).programName(getClass().getName()).build();

				videoFile = convertVideo(config, file, driver, audio);

				response.setContentType(MimeType.getMimeType(audio.getExtension()).getContentType());
				copy(videoFile, response.getOutputStream());
			} catch (Exception e) {
				response.setContentType(MIME_TYPE_TEXT.getContentType());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			} finally {
				if (videoFile != null) {
					videoFile.delete();
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			try {
				response.setContentType(getMimeType(getFilenameSuffix(filePath)).getContentType());
				copy(file, response.getOutputStream());
				response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + new File(filePath).getName());
			} catch (Exception e) {
				response.setContentType(MIME_TYPE_TEXT.getContentType());
				e.printStackTrace(new PrintStream(response.getOutputStream()));
			}
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	private SimpleImmutableEntry<Audio, Class<? extends AudioDriver>> getAudioOfAudio(IConfig config) {
		Audio audio = config.getAudioSection().getAudio();
		if (audio == null) {
			return new SimpleImmutableEntry<>(Audio.MP3, MP3Stream.class);
		}
		switch (audio) {
		case WAV:
			return new SimpleImmutableEntry<>(Audio.WAV, WAVStream.class);

		case MP3:
			return new SimpleImmutableEntry<>(Audio.MP3, MP3Stream.class);

		default:
			return new SimpleImmutableEntry<>(Audio.MP3, MP3Stream.class);
		}
	}

	private SimpleImmutableEntry<Audio, Class<? extends AudioDriver>> getAudioOfVideo(IConfig config) {
		Audio audio = config.getAudioSection().getAudio();
		if (audio == null) {
			return new SimpleImmutableEntry<>(Audio.MP4, MP4Driver.class);
		}
		switch (audio) {
		case AVI:
			return new SimpleImmutableEntry<>(Audio.AVI, AVIDriver.class);
		case MP4:
			return new SimpleImmutableEntry<>(Audio.MP4, MP4Driver.class);

		default:
			return new SimpleImmutableEntry<>(Audio.MP4, MP4Driver.class);
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

	private File convertVideo(IConfig config, File file, AudioDriver driver, Audio audio)
			throws IOException, SidTuneError, URISyntaxException {
		Player player = new Player(config);
		if (config.getSidplay2Section().getDefaultPlayLength() == 0) {
			// Prevent unlimited video length in record mode!
			config.getSidplay2Section().setDefaultPlayLength(30);
		}
		File tmpDirectory = new File(config.getSidplay2Section().getTmpDir());
		File videoFile = File.createTempFile("jsidplay2video", audio.getExtension(), tmpDirectory);
		File extractedFile = File.createTempFile("jsidplay2autostart", PathUtils.getFilenameSuffix(file.getName()),
				tmpDirectory);
		try (OutputStream extractedFileOutputStream = new FileOutputStream(extractedFile)) {
			copy(file, extractedFileOutputStream);
		}
		player.setRecordingFilenameProvider(tune -> videoFile.getAbsolutePath());
		player.setAudioDriver(driver);
		new Convenience(player).autostart(extractedFile, Convenience.LEXICALLY_FIRST_MEDIA, null);
		extractedFile.delete();
		player.stopC64(false);
		return videoFile;
	}
}
