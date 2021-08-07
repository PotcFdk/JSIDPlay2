package server.restful.common;

import static org.apache.http.entity.ContentType.create;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.apache.http.entity.ContentType;

public enum ContentTypeAndFileExtensions {
	/**
	 * Picture formats
	 */
	MIME_TYPE_JPG(create("image/jpeg", (Charset) null), ".jpg", ".jpeg"),
	/**
	 * Audio formats
	 */
	MIME_TYPE_MPEG(create("audio/mpeg", (Charset) null), ".mpg", ".mpeg", ".mp3"),
	MIME_TYPE_WAV(create("audio/wav", (Charset) null), ".wav"),
	MIME_TYPE_FLAC(create("audio/flac", (Charset) null), ".flac"),
	MIME_TYPE_AAC(create("audio/aac", (Charset) null), ".aac"),
	MIME_TYPE_CSV(create("text/csv", (Charset) null), ".csv"),
	/**
	 * Video formats
	 */
	IME_TYPE_FLV(create("video/x-flv", (Charset) null), ".flv", ".f4v"),
	MIME_TYPE_AVI(create("video/msvideo", (Charset) null), ".avi"),
	MIME_TYPE_MP4(create("video/mp4", (Charset) null), ".mp4"),
	/**
	 * SID formats
	 */
	MIME_TYPE_SID(create("audio/prs.sid", (Charset) null), ".sid"),
	/**
	 * Binary formats
	 */
	MIME_TYPE_OCTET_STREAM(create("application/octet-stream", (Charset) null), ".bin"),
	/**
	 * Text
	 */
	MIME_TYPE_TEXT(create("text/plain", StandardCharsets.UTF_8), ".txt"),
	/**
	 * Json
	 */
	MIME_TYPE_JSON(create("application/json", StandardCharsets.UTF_8), ".json"),
	/**
	 * Xml
	 */
	MIME_TYPE_XML(create("application/xml", StandardCharsets.UTF_8), ".xml"),
	/**
	 * Html
	 */
	MIME_TYPE_HTML(create("text/html", StandardCharsets.UTF_8), ".html", ".vue"),
	/**
	 * Javascript
	 */
	MIME_TYPE_JAVASCRIPT(create("application/javascript", (Charset) null), ".js");

	private final ContentType contentType;
	private final String[] extensions;

	private ContentTypeAndFileExtensions(ContentType contentType, String... extensions) {
		this.contentType = contentType;
		this.extensions = extensions;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getMimeType() {
		return contentType.getMimeType();
	}

	public Charset getCharset() {
		return contentType.getCharset();
	}

	@Override
	public String toString() {
		return contentType.toString();
	}

	public boolean isCompatible(String headerValue) {
		ContentType otherContentType = ContentType.parse(headerValue);
		if (otherContentType.getCharset() != null) {
			return Objects.equals(getCharset(), otherContentType.getCharset())
					&& Objects.equals(getMimeType(), otherContentType.getMimeType());
		} else {
			return Objects.equals(getMimeType(), otherContentType.getMimeType());
		}
	}

	public static ContentTypeAndFileExtensions getMimeType(String extension) {
		return Arrays.asList(values()).stream().filter(
				ct -> extension != null && Arrays.asList(ct.getExtensions()).contains(extension.toLowerCase(Locale.US)))
				.findFirst().orElse(MIME_TYPE_OCTET_STREAM);
	}

}
