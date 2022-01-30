package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;
import static server.restful.common.IServletSystemProperties.CACHE_SIZE;
import static server.restful.common.IServletSystemProperties.FRAME_MAX_LENGTH_UPLOAD;
import static server.restful.common.IServletSystemProperties.RTMP_DURATION_TOO_LONG_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_PLAYER_TIMEOUT_PERIOD;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.xml.bind.JAXBContext;

import org.apache.http.HttpHeaders;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.schlichtherle.truezip.file.TFile;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WAVBean;
import sidplay.Player;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	private enum Status {
		INIT, ON_PLAY;

		private LocalDateTime started;

		private Status() {
			this.started = LocalDateTime.now();
		}
	}

	protected static final String C64_MUSIC = "/C64Music";
	protected static final String CGSC = "/CGSC";

	private static final Map<WAVBean, MusicInfoWithConfidenceBean> musicInfoWithConfidenceBeanMap = Collections
			.synchronizedMap(new LRUCache<WAVBean, MusicInfoWithConfidenceBean>(CACHE_SIZE));

	private static final Map<UUID, SimpleImmutableEntry<Player, Status>> playerMap = Collections
			.synchronizedMap(new HashMap<>());

	protected Configuration configuration;

	protected Properties directoryProperties;

	protected JSIDPlay2Servlet(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public abstract String getServletPath();

	protected void doGet(HttpServletRequest request) {
		log(thread() + request(request) + queryString(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void doPost(HttpServletRequest request) {
		log(thread() + request(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void doPut(HttpServletRequest request) {
		log(thread() + request(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void info(String msg) {
		log(thread() + msg);
	}

	protected void error(Throwable t) {
		log(thread() + t.getMessage(), t);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getInput(HttpServletRequest request, Class<T> tClass) throws IOException {
		try (ServletInputStream inputStream = request.getInputStream()) {
			String contentType = request.getContentType();
			if (contentType == null || MIME_TYPE_JSON.isCompatible(contentType)) {
				return new ObjectMapper().readValue(inputStream, tClass);
			} else if (MIME_TYPE_XML.isCompatible(contentType)) {
				return (T) JAXBContext.newInstance(tClass).createUnmarshaller().unmarshal(inputStream);
			} else if (ServletFileUpload.isMultipartContent(request)) {
				// file upload (multipart/mixed)
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				FileItemIterator itemIterator = new ServletFileUpload().getItemIterator(request);
				while (itemIterator.hasNext()) {
					try (InputStream itemInputStream = itemIterator.next().openStream()) {
						ZipFileUtils.copy(itemInputStream, result);
					}
					// just the first file
					break;
				}
				Constructor<T> constructor = tClass.getConstructor(new Class[] { byte[].class, long.class });
				return constructor.newInstance(result.toByteArray(), FRAME_MAX_LENGTH_UPLOAD);
			} else {
				throw new IOException("Unsupported content type: " + contentType);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
		try (ServletOutputStream out = response.getOutputStream()) {
			if (result == null) {
				return;
			}
			Optional<String> optionalContentType = Optional.ofNullable(request.getHeader(HttpHeaders.ACCEPT))
					.map(accept -> Arrays.asList(accept.split(","))).orElse(Collections.emptyList()).stream()
					.findFirst();
			if (!optionalContentType.isPresent() || MIME_TYPE_JSON.isCompatible(optionalContentType.get())) {
				response.setContentType(MIME_TYPE_JSON.toString());
				new ObjectMapper().writeValue(out, result);
			} else if (MIME_TYPE_XML.isCompatible(optionalContentType.get())) {
				response.setContentType(MIME_TYPE_XML.toString());
				JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
			}
		} catch (Exception e) {
			// ignore client aborts
		}
	}

	protected File getAbsoluteFile(String path, boolean adminRole) throws FileNotFoundException {
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvsc();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgsc();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				return PathUtils.getFile(directoryValue + path.substring(directoryLogicalName.length()),
						new TFile(directoryValue), null);
			}
		}
		throw new FileNotFoundException(path);
	}

	protected MusicInfoWithConfidenceBean getMusicInfoWithConfidenceBean(WAVBean wavBean) {
		return musicInfoWithConfidenceBeanMap.get(wavBean);
	}

	protected MusicInfoWithConfidenceBean putMusicInfoWithConfidenceBean(WAVBean wavBean,
			MusicInfoWithConfidenceBean musicInfoWithConfidenceBean) {
		musicInfoWithConfidenceBeanMap.put(wavBean, musicInfoWithConfidenceBean);
		return musicInfoWithConfidenceBean;
	}

	protected Player createPlayer(UUID uuid, Player player) {
		playerMap.put(uuid, new SimpleImmutableEntry<>(player, Status.INIT));
		return player;
	}

	protected void onPlay(UUID uuid) {
		info("onPlay: RTMP stream of: " + uuid);

		Player player = playerMap.get(uuid).getKey();
		playerMap.put(uuid, new SimpleImmutableEntry<>(player, Status.ON_PLAY));

		for (UUID otherUuid : playerMap.keySet()) {
			info("onPlay: REMAINING RTMP stream: " + otherUuid);
		}
	}

	protected void onPlayDone(UUID uuid) {
		info("onPlayDone: RTMP stream of: " + uuid);

		Player player = playerMap.remove(uuid).getKey();

		if (player != null) {
			info("onPlayDone: QUIT RTMP stream of: " + uuid);
			player.quit();
		}

		for (UUID otherUuid : playerMap.keySet()) {
			info("onPlayDone: REMAINING RTMP stream: " + otherUuid);
		}
	}

	public static void cleanupPlayerPeriodically() {
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				Collection<UUID> toRemove = new ArrayList<>();
				for (UUID uuid : playerMap.keySet()) {
					SimpleImmutableEntry<Player, Status> pair = playerMap.get(uuid);
					if ((pair.getValue() == Status.INIT
							&& Duration.between(pair.getValue().started, LocalDateTime.now())
									.getSeconds() > RTMP_NOT_PLAYED_TIMEOUT)
							|| (pair.getValue() == Status.ON_PLAY
									&& Duration.between(pair.getValue().started, LocalDateTime.now())
											.getSeconds() > RTMP_DURATION_TOO_LONG_TIMEOUT)) {
						toRemove.add(uuid);
					}
				}
				for (UUID uuid : toRemove) {
					Player player = playerMap.get(uuid).getKey();
					if (player != null) {
						player.quit();
					}
				}
				playerMap.keySet().removeIf(toRemove::contains);
			}
		};
		new Timer().schedule(task, 0, RTMP_PLAYER_TIMEOUT_PERIOD);
	}

	private String thread() {
		StringBuilder result = new StringBuilder();
		result.append(Thread.currentThread().getName());
		result.append(": ");
		return result.toString();
	}

	private String request(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(request.getMethod());
		result.append(" ");
		result.append(request.getRequestURI());
		return result.toString();
	}

	private String queryString(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		if (request.getQueryString() != null) {
			result.append("?");
			result.append(request.getQueryString());
		}
		return result.toString();
	}

	private String remoteAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(", from ");
		result.append(request.getRemoteAddr());
		result.append(" (");
		result.append(request.getRemotePort());
		result.append(")");
		return result.toString();
	}

	private String localAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(", to ");
		result.append(request.getLocalAddr());
		result.append(" (");
		result.append(request.getLocalPort());
		result.append(")");
		return result.toString();
	}

	private String memory() {
		StringBuilder result = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		result.append(String.format(", %,dMb/%,dMb", runtime.totalMemory() - runtime.freeMemory() >> 20,
				runtime.maxMemory() >> 20));
		return result.toString();
	}

}
