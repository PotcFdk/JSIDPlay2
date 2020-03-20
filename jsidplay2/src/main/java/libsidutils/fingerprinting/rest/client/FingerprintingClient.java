package libsidutils.fingerprinting.rest.client;

import static javax.servlet.http.HttpServletRequest.BASIC_AUTH;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import libsidutils.fingerprinting.rest.FingerPrintingApi;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import server.restful.common.MimeType;

public class FingerprintingClient implements FingerPrintingApi {

	private final static String PATH = "http://127.0.0.1:8080/jsidplay2service/JSIDPlay2REST";

	@Override
	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		try {
			HttpURLConnection connection = send(musicInfoBean, MusicInfoBean.class, "/insert-tune", "PUT");

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(IdBean.class, connection);
			}
			throw new RuntimeException("Invalid response: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void insertHashes(HashBeans hashBeans) {
		try {
			HttpURLConnection connection = send(hashBeans, HashBeans.class, "/insert-hashes", "PUT");

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return;
			}
			throw new RuntimeException("Invalid response: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public HashBeans findAllHashes(IntArrayBean intArray) {
		try {
			HttpURLConnection connection = send(intArray, IntArrayBean.class, "/hash", "POST");

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(HashBeans.class, connection);
			}
			throw new RuntimeException("Invalid response: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MusicInfoBean findTune(SongNoBean songNoBean) {
		try {
			HttpURLConnection connection = send(songNoBean, SongNoBean.class, "/tune", "POST");

			if (connection.getResponseCode() == Response.Status.OK.getStatusCode()) {
				return receive(MusicInfoBean.class, connection);
			}
			throw new RuntimeException("Invalid response: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> HttpURLConnection send(T parameter, Class<T> tClass, String requestPath, String requestMethod)
			throws MalformedURLException, IOException, ProtocolException, JAXBException {

		HttpURLConnection connection = (HttpURLConnection) new URL(PATH + requestPath).openConnection();
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod(requestMethod);
		String encoded = Base64.getEncoder().encodeToString(("jsidplay2:jsidplay2!").getBytes(StandardCharsets.UTF_8));
		connection.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTH + " " + encoded);
		connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MimeType.MIME_TYPE_XML.getContentType());
		connection.setRequestProperty(HttpHeaders.ACCEPT, MimeType.MIME_TYPE_XML.getContentType());

		JAXBContext.newInstance(tClass).createMarshaller().marshal(parameter, connection.getOutputStream());
		connection.getOutputStream().flush();

		return connection;
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Class<T> theClass, HttpURLConnection connection) {
		try {
			Object obj = JAXBContext.newInstance(theClass).createUnmarshaller().unmarshal(connection.getInputStream());
			if (theClass.isInstance(obj)) {
				connection.disconnect();
				return (T) obj;
			}
			connection.disconnect();
			return null;
		} catch (Exception e) {
				throw new RuntimeException(e);
		}
	}

}
