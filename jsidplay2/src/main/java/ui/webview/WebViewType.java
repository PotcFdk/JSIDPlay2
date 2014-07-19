package ui.webview;

public enum WebViewType {
	CSDB("http://csdb.dk/"), CODEBASE64("http://codebase64.org/"), REMIX_KWED_ORG(
			"http://remix.kwed.org/"), SID_OTH4_COM("http://sid.oth4.com/"), C64_SK(
			"http://www.c64.sk/"), FORUM64_DE("http://www.forum64.de/"), LEMON64_COM(
			"http://www.lemon64.com/"), JSIDPLAY2(
			"http://sourceforge.net/p/jsidplay2/code/HEAD/tree/trunk/jsidplay2/");

	private String url;

	private WebViewType(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
