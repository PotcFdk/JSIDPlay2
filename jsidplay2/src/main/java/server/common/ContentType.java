package server.common;

import java.util.Arrays;
import java.util.Locale;

public enum ContentType {
	/**
	 * Picture formats
	 */
	MIME_TYPE_JPG("image/jpeg", ".jpg", ".jpeg"),
	/**
	 * Audio formats
	 */
	MIME_TYPE_MPEG("audio/mpeg", ".mpg", ".mpeg", ".mp3"),
	/**
	 * SID formats
	 */
	MIME_TYPE_SID("audio/prs.sid", ".sid"),
	/**
	 * Other formats
	 */
	MIME_TYPE_OCTET_STREAM("application/octet-stream");

	private String contentType;
	private String[] extensions;

	private ContentType(String contentType, String... extensions) {
		this.contentType = contentType;
		this.extensions = extensions;
	}

	public String getContentType() {
		return contentType;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public static ContentType getContentType(String extension) {
		return Arrays.asList(values()).stream().filter(
				ct -> extension != null && Arrays.asList(ct.getExtensions()).contains(extension.toLowerCase(Locale.US)))
				.findFirst().orElse(MIME_TYPE_OCTET_STREAM);
	}
	
}
