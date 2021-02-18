package sidplay.fingerprinting;

import static java.util.Base64.getEncoder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import libsidutils.fingerprinting.IFingerprintMatcher;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.WavBean;

/**
 * Send WAV to a server to identify a tune by WhatsSID.
 *
 * @author ken
 *
 */
public class FingerprintJsonClient implements IFingerprintMatcher {

	private String url;
	private String username;
	private String password;
	private int connectionTimeout;

	public FingerprintJsonClient(String url, String username, String password, int connectionTimeout) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.connectionTimeout = connectionTimeout;
	}

	@Override
	public MusicInfoWithConfidenceBean match(WavBean wavBean) throws IOException {
		HttpURLConnection connection = sendJson(wavBean);
		if (connection != null && connection.getResponseCode() == 200 && connection.getContentLength() > 0) {
			return receiveJson(connection);
		}
		return null;
	}

	private HttpURLConnection sendJson(WavBean wavBean) throws IOException {
		String header = getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
		String wavBytes = getEncoder().encodeToString(wavBean.getWav());

		HttpURLConnection connection = (HttpURLConnection) new URL(url + "/whatssid").openConnection();
		connection.setConnectTimeout(connectionTimeout);
		connection.setDoOutput(true);
		connection.setInstanceFollowRedirects(false);
		connection.setRequestMethod("POST");
		connection.setRequestProperty("Authorization", "Basic " + header);
		connection.setRequestProperty("Content-Type", "application/json");
		connection.setRequestProperty("Accept", "application/json");
		connection.getOutputStream().write(("{\"wav\": \"" + wavBytes + "\"}").getBytes(StandardCharsets.UTF_8));
		connection.getOutputStream().flush();
		return connection;
	}

	private MusicInfoWithConfidenceBean receiveJson(HttpURLConnection connection) throws IOException {
		try {
			String response = null;
			try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
				response = scanner.useDelimiter("\\A").next();
			}
			MusicInfoWithConfidenceBean match = new MusicInfoWithConfidenceBean();
			match.setMusicInfo(new MusicInfoBean());
			{
				// match string values
				Matcher m = Pattern.compile("\"[^\"]*\":\"[^\"]*\"").matcher(response);
				while (m.find()) {
					String[] keyValue = m.group().split(":");
					setMatch(keyValue[0], keyValue[1], match);
				}
			}
			{
				// match numeric values
				Matcher m = Pattern.compile("\"[^\"]*\":[0-9.]+").matcher(response);
				while (m.find()) {
					String[] keyValue = m.group().split(":");
					setMatch(keyValue[0], keyValue[1], match);
				}
			}
			return match;
		} finally {
			connection.disconnect();
		}
	}

	private void setMatch(String key, String value, MusicInfoWithConfidenceBean match) {
		switch (key) {
		case "\"songNo\"":
			match.getMusicInfo().setSongNo(Integer.parseInt(value));
			break;
		case "\"title\"":
			match.getMusicInfo().setTitle(value.substring(1, value.length() - 1));
			break;
		case "\"artist\"":
			match.getMusicInfo().setArtist(value.substring(1, value.length() - 1));
			break;
		case "\"album\"":
			match.getMusicInfo().setAlbum(value.substring(1, value.length() - 1));
			break;
		case "\"fileDir\"":
			match.getMusicInfo().setFileDir(value.substring(1, value.length() - 1));
			break;
		case "\"infoDir\"":
			match.getMusicInfo().setInfoDir(value.substring(1, value.length() - 1));
			break;
		case "\"audioLength\"":
			match.getMusicInfo().setAudioLength(Double.parseDouble(value));
			break;
		case "\"confidence\"":
			match.setConfidence(Integer.parseInt(value));
			break;
		case "\"relativeConfidence\"":
			match.setRelativeConfidence(Double.parseDouble(value));
			break;
		case "\"offset\"":
			match.setOffset(Integer.parseInt(value));
			break;
		case "\"offsetSeconds\"":
			match.setOffsetSeconds(Double.parseDouble(value));
			break;
		default:
			break;
		}
	}

}
