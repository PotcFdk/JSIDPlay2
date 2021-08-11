package server.restful.servlets;

import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.PathUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.JCommander;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IWhatsSidSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletParameters;
import sidplay.Player;
import sidplay.audio.AACDriver.AACStreamDriver;
import sidplay.audio.AVIFileDriver;
import sidplay.audio.Audio;
import sidplay.audio.AudioDriver;
import sidplay.audio.FLACDriver.FLACStreamDriver;
import sidplay.audio.FLVDriver;
import sidplay.audio.FLVDriver.FLVStreamDriver;
import sidplay.audio.MP3Driver.MP3StreamDriver;
import sidplay.audio.MP4Driver.MP4FileDriver;
import sidplay.audio.SIDDumpDriver.SIDDumpStreamDriver;
import sidplay.audio.SIDRegDriver.SIDRegStreamDriver;
import sidplay.audio.WAVDriver.WAVStreamDriver;
import sidplay.fingerprinting.FingerprintJsonClient;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.entities.config.Configuration;
import ui.entities.config.WhatsSidSection;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	private static final int PRESS_SPACE_INTERVALL = 30;

	private static final String RTMP_UPLOAD_URL = System.getProperty("rtmp.internal.upload.url",
			"rtmp://localhost/live");

	private static final String RTMP_INTERNAL_DOWNLOAD_URL = System.getProperty("rtmp.internal.download.url",
			"rtmp://haendel.ddns.net/live");

	private static final String RTMP_EXTERNAL_DOWNLOAD_URL = System.getProperty("rtmp.external.download.url",
			"rtmp://haendel.ddns.net/live");

	private static final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	private Consumer<Player> interactivityHook = player -> {
		final IEmulationSection emulationSection = player.getConfig().getEmulationSection();
		final IWhatsSidSection whatsSidSection = player.getConfig().getWhatsSidSection();

		// after chip model has been detected: disable WhatsSID
		if (emulationSection.getOverrideSection().getSidModel()[0] != null && whatsSidSection.isEnable()) {
			whatsSidSection.setEnable(false);
		}
		final C64 c64 = player.getC64();
		long time = c64.getEventScheduler().getTime(Phase.PHI2);
		long seconds = time / (int) c64.getClock().getCpuFrequency();
		if ((seconds % PRESS_SPACE_INTERVALL == 0) && !c64.getKeyboard().getKeysDown().contains(SPACE)) {
			// press space every N seconds
			c64.getKeyboard().keyPressed(SPACE);
			c64.getEventScheduler().schedule(new Event("Key Released: " + SPACE.name()) {
				@Override
				public void event() throws InterruptedException {
					c64.getKeyboard().keyReleased(SPACE);
				}
			}, (long) (c64.getClock().getCpuFrequency() + 1));
		}
	};

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
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
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			String filePath = request.getPathInfo();
			File file = getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			if (Stream.of(".sid", ".dat", ".mus", ".str")
					.filter(ext -> file.getName().toLowerCase(Locale.ENGLISH).endsWith(ext)).findFirst().isPresent()) {

				final ServletParameters servletParameters = new ServletParameters();
				final IniConfig config = servletParameters.getConfig();

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(servletParameters).programName(getClass().getName()).build()
						.parse(args);

				AudioDriver driver = getAudioDriverOfAudioFormat(config, response.getOutputStream());

				response.setContentType(getMimeType(driver.getExtension()).toString());
				if (Boolean.TRUE.equals(servletParameters.getDownload())) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ (getFilenameWithoutSuffix(file.getName()) + driver.getExtension()));
				}
				convertAudio(config, file, driver, servletParameters.getSong());
				response.setStatus(HttpServletResponse.SC_OK);
			} else if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".mp3") && (cartFileFilter.accept(file)
					|| tuneFileFilter.accept(file) || diskFileFilter.accept(file) || tapeFileFilter.accept(file))) {

				final ServletParameters servletParameters = new ServletParameters();
				final IniConfig config = servletParameters.getConfig();

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(servletParameters).programName(getClass().getName()).build()
						.parse(args);

				UUID uuid = UUID.randomUUID();

				AudioDriver driver = getAudioDriverOfVideoFormat(config, uuid);

				response.setContentType(getMimeType(driver.getExtension()).toString());
				if (Boolean.TRUE.equals(servletParameters.getDownload())) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ (getFilenameWithoutSuffix(file.getName()) + driver.getExtension()));
				}
				if (driver instanceof FLVDriver) {
					new Thread(() -> {
						try {
							convertVideo(config, file, driver).delete();
						} catch (IOException | SidTuneError e) {
							log("Error converting video!", e);
						}
					}).start();

					response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
					response.setHeader(HttpHeaders.LOCATION, getRTMPUrl(request.getRemoteAddr()) + "/" + uuid);
				} else {
					File videoFile = convertVideo(config, file, driver);
					copy(videoFile, response.getOutputStream());
					videoFile.delete();

					response.setStatus(HttpServletResponse.SC_OK);
				}
			} else {
				response.setContentType(getMimeType(getFilenameSuffix(filePath)).toString());
				response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + new File(filePath).getName());
				copy(getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN)), response.getOutputStream());
				response.setStatus(HttpServletResponse.SC_OK);
			}
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
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
			return new WAVStreamDriver(outputstream);
		case FLAC:
			return new FLACStreamDriver(outputstream);
		case AAC:
			return new AACStreamDriver(outputstream);
		case MP3:
		default:
			return new MP3StreamDriver(outputstream);
		case SID_DUMP:
			return new SIDDumpStreamDriver(outputstream);
		case SID_REG:
			return new SIDRegStreamDriver(outputstream);
		}
	}

	private void convertAudio(IConfig config, File file, AudioDriver driver, Integer song)
			throws IOException, SidTuneError {
		Player player = new Player(config);
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		SidTune tune = SidTune.load(file);
		tune.getInfo().setSelectedSong(song);
		player.play(tune);
		player.stopC64(false);
	}

	private AudioDriver getAudioDriverOfVideoFormat(IConfig config, UUID uuid) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(Audio.FLV)) {
		case FLV:
		default:
			return new FLVStreamDriver(RTMP_UPLOAD_URL + "/" + uuid);
		case AVI:
			return new AVIFileDriver();
		case MP4:
			return new MP4FileDriver();
		}
	}

	private File convertVideo(IConfig config, File file, AudioDriver driver) throws IOException, SidTuneError {
		final WhatsSidSection whatsSidSection = configuration.getWhatsSidSection();
		String url = whatsSidSection.getUrl();
		String username = whatsSidSection.getUsername();
		String password = whatsSidSection.getPassword();
		int connectionTimeout = whatsSidSection.getConnectionTimeout();

		Player player = new Player(config);
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.getConfig().getSidplay2Section().setHvsc(root);
		}
		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(),
				config.getSidplay2Section().getTmpDir());
		videoFile.deleteOnExit();
		player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		player.setAudioDriver(driver);
		player.setFingerPrintMatcher(new FingerprintJsonClient(url, username, password, connectionTimeout));
		player.setInteractivityHook(interactivityHook);
		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);
		player.stopC64(false);
		return videoFile;
	}

	private String getRTMPUrl(String remoteAddress) {
		boolean isLocal = remoteAddress.startsWith("192.168.") || remoteAddress.equals("127.0.0.1");
		return isLocal ? RTMP_INTERNAL_DOWNLOAD_URL : RTMP_EXTERNAL_DOWNLOAD_URL;
	}

}
