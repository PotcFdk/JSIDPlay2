package ui.webview;

public enum WebViewType {
	/**
	 * The C-64 Scene Database
	 */
	CSDB("http://csdb.dk/"),
	/**
	 * Codebase 64 Wiki
	 */
	CODEBASE64("http://codebase64.org/"),
	/**
	 * Remix.Kwed.Org The Devinitive Guide To C64 Remakes
	 */
	REMIX_KWED_ORG("http://remix.kwed.org/"),
	/**
	 * The SID 6581/8580 Recordings Archive
	 */
	SID_OTH4_COM("http://sid.oth4.com/"),
	/**
	 * C-64 Portal
	 */
	C64_SK("http://www.c64.sk/"),
	/**
	 * Forum 64
	 */
	FORUM64_DE("http://www.forum64.de/"),
	/**
	 * Lemon Retro Store
	 */
	LEMON64_COM("http://www.lemon64.com/"),
	/**
	 * JSIDPlay2 Source Code
	 */
	JSIDPLAY2("http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/"),
	/**
	 * JSIDPlay2 User Guide
	 */
	USERGUIDE("jar:file:" + "/doc/UserGuide.html");

	private static final String JAR_URL = "jar:file:";
	private String url;

	private WebViewType(String url) {
		this.url = url;
	}

	public String getUrl() {
		if (url.startsWith(JAR_URL)) {
			return getClass().getResource(url.replace(JAR_URL, "")).toExternalForm();
		}
		return url;
	}
}
