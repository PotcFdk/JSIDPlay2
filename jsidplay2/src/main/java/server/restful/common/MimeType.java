package server.restful.common;

import java.util.Arrays;
import java.util.Locale;

public enum MimeType {
	/**
	 * Picture formats
	 */
	MIME_TYPE_JPG("image/jpeg", ".jpg", ".jpeg"),
	/**
	 * Audio formats
	 */
	MIME_TYPE_MPEG("audio/mpeg", ".mpg", ".mpeg", ".mp3"),
	/**
	 * Video formats
	 */
	MIME_TYPE_MP4("video/mp4", ".mp4"),
	/**
	 * SID formats
	 */
	MIME_TYPE_SID("audio/prs.sid", ".sid"),
	/**
	 * Binary formats
	 */
	MIME_TYPE_OCTET_STREAM("application/octet-stream"),
	/**
	 * Text
	 */
	MIME_TYPE_TEXT("text/plain; charset=utf-8"),
	/**
	 * Json
	 */
	MIME_TYPE_JSON("application/json; charset=utf-8"),
	/**
	 * Html
	 */
	MIME_TYPE_HTML("text/html; charset=utf-8");

	private String contentType;
	private String[] extensions;

	private MimeType(String contentType, String... extensions) {
		this.contentType = contentType;
		this.extensions = extensions;
	}

	public String getContentType() {
		return contentType;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public static MimeType getMimeType(String extension) {
		return Arrays.asList(values()).stream().filter(
				ct -> extension != null && Arrays.asList(ct.getExtensions()).contains(extension.toLowerCase(Locale.US)))
				.findFirst().orElse(MIME_TYPE_OCTET_STREAM);
	}

}
