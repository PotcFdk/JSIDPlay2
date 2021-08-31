package ui.tools;

import static jakarta.servlet.http.HttpServletRequest.BASIC_AUTH;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;
import static server.restful.servlets.whatssid.FindHashServlet.FIND_HASH_PATH;
import static server.restful.servlets.whatssid.FindTuneServlet.FIND_TUNE_PATH;
import static server.restful.servlets.whatssid.InsertHashesServlet.INSERT_HASHES_PATH;
import static server.restful.servlets.whatssid.InsertTuneServlet.INSERT_TUNE_PATH;
import static server.restful.servlets.whatssid.TuneExistsServlet.TUNE_EXISTS_PATH;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import libsidutils.fingerprinting.rest.FingerPrintingDataSource;
import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import server.restful.common.HttpMethod;

/**
 * This currently unused class makes it possible to create/fill the
 * fingerprinting database on a remote machine running JSIDPlay2Server. As an
 * alternative the WhatssSidService implementation is used, instead, to create
 * the database by the tool FingerPrintingCreator on the same machine.
 * 
 * @author ken
 *
 */
public class FingerprintingClient implements FingerPrintingDataSource {

	private String url;
	private String username;
	private String password;
	private int connectionTimeout;

	private boolean useXml;

	public FingerprintingClient(String url, String username, String password, int connectionTimeout) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.connectionTimeout = connectionTimeout;
	}

	public void setUseXml(boolean useXml) {
		this.useXml = useXml;
	}

	@Override
	public IdBean insertTune(MusicInfoBean musicInfoBean) {
		try {
			HttpURLConnection connection = send(musicInfoBean, MusicInfoBean.class, INSERT_TUNE_PATH, HttpMethod.PUT);

			if (connection.getResponseCode() == HttpStatus.SC_OK) {
				return receive(IdBean.class, connection);
			}
			throw new IOException(connection.getURL() + "\nResponseCode: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void insertHashes(HashBeans hashBeans) {
		try {
			HttpURLConnection connection = send(hashBeans, HashBeans.class, INSERT_HASHES_PATH, HttpMethod.PUT);

			if (connection.getResponseCode() == HttpStatus.SC_OK) {
				return;
			}
			throw new IOException(connection.getURL() + "\nResponseCode: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public HashBeans findHashes(IntArrayBean intArray) {
		try {
			HttpURLConnection connection = send(intArray, IntArrayBean.class, FIND_HASH_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == HttpStatus.SC_OK) {
				return receive(HashBeans.class, connection);
			}
			throw new IOException(connection.getURL() + "\nResponseCode: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MusicInfoBean findTune(SongNoBean songNoBean) {
		try {
			HttpURLConnection connection = send(songNoBean, SongNoBean.class, FIND_TUNE_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == HttpStatus.SC_OK) {
				return receive(MusicInfoBean.class, connection);
			}
			throw new IOException(connection.getURL() + "\nResponseCode: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean tuneExists(MusicInfoBean musicInfoBean) {
		try {
			HttpURLConnection connection = send(musicInfoBean, MusicInfoBean.class, TUNE_EXISTS_PATH, HttpMethod.POST);

			if (connection.getResponseCode() == HttpStatus.SC_OK) {
				return receive(Boolean.class, connection);
			}
			throw new IOException(connection.getURL() + "\nResponseCode: " + connection.getResponseCode());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private <T> HttpURLConnection send(T parameter, Class<T> tClass, String requestPath, String requestMethod)
			throws MalformedURLException, IOException, ProtocolException, JAXBException {
		HttpURLConnection connection = (HttpURLConnection) new URL(url + requestPath).openConnection();
		connection.setConnectTimeout(connectionTimeout);
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod(requestMethod);
		connection.setRequestProperty(HttpHeaders.AUTHORIZATION, BASIC_AUTH + " "
				+ Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8)));

		if (useXml) {
			connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MIME_TYPE_XML.toString());
			connection.setRequestProperty(HttpHeaders.ACCEPT, MIME_TYPE_XML.getMimeType());
			JAXBContext.newInstance(tClass).createMarshaller().marshal(parameter, connection.getOutputStream());
		} else {
			connection.setRequestProperty(HttpHeaders.CONTENT_TYPE, MIME_TYPE_JSON.toString());
			connection.setRequestProperty(HttpHeaders.ACCEPT, MIME_TYPE_JSON.getMimeType());
			connection.getOutputStream().write(new ObjectMapper().writeValueAsBytes(parameter));
		}
		connection.getOutputStream().flush();

		return connection;
	}

	@SuppressWarnings("unchecked")
	private <T> T receive(Class<T> theClass, HttpURLConnection connection) {
		try {
			if (connection.getContentLength() == 0) {
				return null;
			}
			Object obj;
			if (useXml) {
				obj = JAXBContext.newInstance(theClass).createUnmarshaller().unmarshal(connection.getInputStream());
			} else {
				obj = new ObjectMapper().readValue(connection.getInputStream(), theClass);
			}
			if (theClass.isInstance(obj)) {
				return (T) obj;
			}
			return null;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			connection.disconnect();
		}
	}

}
