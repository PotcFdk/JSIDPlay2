package server.restful.servlets;

import static java.lang.Thread.MAX_PRIORITY;
import static java.lang.Thread.getAllStackTraces;
import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static libsidplay.config.IAudioSystemProperties.MAX_LENGTH;
import static libsidplay.config.IAudioSystemProperties.MAX_RTMP_THREADS;
import static libsidplay.config.IAudioSystemProperties.PRESS_SPACE_INTERVALL;
import static libsidplay.config.IAudioSystemProperties.RTMP_EXTERNAL_DOWNLOAD_URL;
import static libsidplay.config.IAudioSystemProperties.RTMP_INTERNAL_DOWNLOAD_URL;
import static libsidplay.config.IAudioSystemProperties.RTMP_UPLOAD_URL;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.PathUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.closeEntityManager;
import static server.restful.JSIDPlay2Server.getEntityManager;
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
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.JCommander;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.Event;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletParameters;
import sidplay.Player;
import sidplay.audio.AACDriver.AACStreamDriver;
import sidplay.audio.AVIDriver.AVIFileDriver;
import sidplay.audio.Audio;
import sidplay.audio.AudioDriver;
import sidplay.audio.FLACDriver.FLACStreamDriver;
import sidplay.audio.FLVDriver.FLVFileDriver;
import sidplay.audio.FLVDriver.FLVStreamDriver;
import sidplay.audio.MP3Driver.MP3StreamDriver;
import sidplay.audio.MP4Driver.MP4FileDriver;
import sidplay.audio.ProxyDriver;
import sidplay.audio.SIDDumpDriver.SIDDumpStreamDriver;
import sidplay.audio.SIDRegDriver.SIDRegStreamDriver;
import sidplay.audio.SleepDriver;
import sidplay.audio.WAVDriver.WAVStreamDriver;
import sidplay.ini.IniConfig;
import sidplay.player.State;
import ui.common.Convenience;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.entities.config.Configuration;
import ui.entities.whatssid.service.WhatsSidService;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	public static final String CONVERT_PATH = "/convert";

	private static final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + CONVERT_PATH;
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

				Audio audio = getAudioFormat(config);
				AudioDriver driver = getAudioDriverOfAudioFormat(audio, response.getOutputStream());

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

				Audio audio = getVideoFormat(config);
				AudioDriver driver = getAudioDriverOfVideoFormat(audio, uuid, servletParameters.getDownload());

				response.setContentType(getMimeType(driver.getExtension()).toString());
				if (Boolean.FALSE.equals(servletParameters.getDownload()) && audio == Audio.FLV) {
					if (getAllStackTraces().keySet().stream().map(Thread::getName).filter(RTMP_THREAD::equals)
							.count() < MAX_RTMP_THREADS) {
						Thread thread = new Thread(() -> {
							try {
								convertLiveVideo(config, file, driver, getEntityManager());
							} catch (IOException | SidTuneError e) {
								log("Error converting video!", e);
							} finally {
								closeEntityManager();
							}
						}, RTMP_THREAD);
						thread.setPriority(MAX_PRIORITY);
						thread.start();
						response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
						response.setHeader(HttpHeaders.LOCATION, getRTMPUrl(request.getRemoteAddr(), uuid));
					} else {
						String message = "Max threads reached: " + MAX_RTMP_THREADS + ", please retry in some minutes!";
						info(message);
						response.setContentType(MIME_TYPE_TEXT.toString());
						new PrintStream(response.getOutputStream()).println(message);
					}
				} else {
					if (Boolean.TRUE.equals(servletParameters.getDownload())) {
						response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
								+ (getFilenameWithoutSuffix(file.getName()) + driver.getExtension()));
					}
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

	private Audio getAudioFormat(IConfig config) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(Audio.MP3)) {
		case WAV:
			return Audio.WAV;
		case FLAC:
			return Audio.FLAC;
		case AAC:
			return Audio.AAC;
		case MP3:
		default:
			return Audio.MP3;
		case SID_DUMP:
			return Audio.SID_DUMP;
		case SID_REG:
			return Audio.SID_REG;
		}
	}

	private AudioDriver getAudioDriverOfAudioFormat(Audio audio, OutputStream outputstream) {
		switch (audio) {
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

	private Audio getVideoFormat(IConfig config) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(Audio.FLV)) {
		case FLV:
		default:
			return Audio.FLV;
		case AVI:
			return Audio.AVI;
		case MP4:
			return Audio.MP4;
		}
	}

	private AudioDriver getAudioDriverOfVideoFormat(Audio audio, UUID uuid, Boolean download) {
		switch (audio) {
		case FLV:
		default:
			return Boolean.TRUE.equals(download) ? new FLVFileDriver()
					: new ProxyDriver(new SleepDriver(), new FLVStreamDriver(RTMP_UPLOAD_URL + "/" + uuid));
		case AVI:
			return new AVIFileDriver();
		case MP4:
			return new MP4FileDriver();
		}
	}

	private void convertLiveVideo(IConfig config, File file, AudioDriver driver, EntityManager em)
			throws IOException, SidTuneError {
		final ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		sidplay2Section.setDefaultPlayLength(Math.min(sidplay2Section.getDefaultPlayLength(), MAX_LENGTH));

		Player player = new Player(config);
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			config.getSidplay2Section().setHvsc(root);
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setFingerPrintMatcher(new FingerPrinting(new IniFingerprintConfig(), new WhatsSidService(em)));
		player.setAudioDriver(driver);

		addPressSpaceListener(player);
		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);
		player.stopC64(false);
	}

	private File convertVideo(IConfig config, File file, AudioDriver driver) throws IOException, SidTuneError {
		final ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		sidplay2Section.setDefaultPlayLength(Math.min(sidplay2Section.getDefaultPlayLength(), MAX_LENGTH));

		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(), sidplay2Section.getTmpDir());
		videoFile.deleteOnExit();

		Player player = new Player(config);
		player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		player.setAudioDriver(driver);

		addPressSpaceListener(player);
		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);
		player.stopC64(false);

		return videoFile;
	}

	private void addPressSpaceListener(Player player) {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {
				player.getC64().getEventScheduler().schedule(new Event("Press Space") {

					@Override
					public void event() throws InterruptedException {
						// press space every N seconds
						player.getC64().getKeyboard().keyPressed(SPACE);
						player.getC64().getEventScheduler().schedule(new Event("Key Released: " + SPACE.name()) {
							@Override
							public void event() throws InterruptedException {
								player.getC64().getKeyboard().keyReleased(SPACE);
							}
						}, (long) (player.getC64().getClock().getCpuFrequency()));

						player.getC64().getEventScheduler().schedule(this,
								PRESS_SPACE_INTERVALL * (long) player.getC64().getClock().getCpuFrequency());
					}

				}, PRESS_SPACE_INTERVALL * (long) player.getC64().getClock().getCpuFrequency());
			}
		});
	}

	private String getRTMPUrl(String remoteAddress, UUID uuid) {
		boolean isLocal = remoteAddress.startsWith("192.168.") || remoteAddress.equals("127.0.0.1");
		String baseUrl = isLocal ? RTMP_INTERNAL_DOWNLOAD_URL : RTMP_EXTERNAL_DOWNLOAD_URL;
		if (baseUrl.startsWith("http")) {
			// HLS protocol
			return baseUrl + "/" + uuid + ".m3u8";
		}
		// RTMP protocol
		return baseUrl + "/" + uuid;
	}

}
