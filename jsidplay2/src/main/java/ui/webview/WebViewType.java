package ui.webview;

import static ui.webview.WebViewType.Constants.DOC_IMAGES_DIR;
import static ui.webview.WebViewType.Constants.DOC_RESOURCE_DIR;
import static ui.webview.WebViewType.Constants.JAR_URL;
import static ui.webview.WebViewType.Constants.JAVADOC_RESOURCE_DIR;

import java.net.URL;

public enum WebViewType {
	/**
	 * The C-64 Scene Database
	 */
	CSDB("https://csdb.dk/"),
	/**
	 * Codebase 64 Wiki
	 */
	CODEBASE64("http://codebase64.org/"),
	/**
	 * Remix.Kwed.Org The Devinitive Guide To C64 Remakes
	 */
	REMIX_KWED_ORG("https://remix.kwed.org/"),
	/**
	 * C-64 Portal
	 */
	C64_SK("http://www.c64.sk/"),
	/**
	 * Forum 64
	 */
	FORUM64_DE("https://www.forum64.de/"),
	/**
	 * Lemon Retro Store
	 */
	LEMON64_COM("https://www.lemon64.com/"),
	/**
	 * Stone Oakvalley's Authentic SID Collection (SOASC=)
	 */
	SOASC("https://www.6581-8580.com/"),
	/**
	 * JSIDPlay2 Source Code
	 */
	JSIDPLAY2_SRC("https://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/"),
	/**
	 * JSIDPlay2 Java Documentation
	 */
	JSIDPLAY2_JAVADOC(JAR_URL + JAVADOC_RESOURCE_DIR + "index.html"),
	/**
	 * JSIDPlay2 User Guide
	 */
	USERGUIDE(JAR_URL + DOC_RESOURCE_DIR + "UserGuide.html");

	private String url;

	private WebViewType(String url) {
		this.url = url;
	}

	public String getUrl() {
		if (url.startsWith(JAR_URL)) {
			if (this == JSIDPLAY2_JAVADOC) {
				String userGuideUrl = USERGUIDE.url.replace(JAR_URL, "");
				URL resource = getClass().getResource(userGuideUrl);
				String result = resource != null ? resource.toExternalForm() : "";
				result = result.replace(userGuideUrl, "/index.html");
				result = result.replace("/jsidplay2-", "/jsidplay2_doc-");
				result = result.replace(".jar", "-javadoc.jar");
				return result;
			} else {
				URL resource = getClass().getResource(url.replace(JAR_URL, ""));
				String result = resource != null ? resource.toExternalForm() : "";
				return result;
			}
		}
		return url;
	}

	/**
	 * Convert relative path names starting with "images/" of the documentation to
	 * absolute path names (This is for the internal {@link WebViewType#USERGUIDE}
	 * contained in the main JAR as resources located in sub-folder "/doc/").
	 *
	 * @param url URL to make absolute
	 * @return absolute URL
	 */
	public static String toAbsoluteUrl(String url) {
		if (url.startsWith(DOC_IMAGES_DIR)) {
			return DOC_RESOURCE_DIR + url;
		}
		return url;
	}

	static class Constants {
		static final String JAR_URL = "jar:file:";
		static final String DOC_RESOURCE_DIR = "/doc/";
		static final String DOC_IMAGES_DIR = "images/";
		static final String JAVADOC_RESOURCE_DIR = "/";
	}
}
