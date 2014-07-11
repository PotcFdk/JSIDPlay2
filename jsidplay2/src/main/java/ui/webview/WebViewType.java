package ui.webview;

public enum WebViewType {
	CSDB("http://csdb.dk/"), CODEBASE64("http://codebase64.org/"), REMIX_KWED_ORG(
			"http://remix.kwed.org/"), SID_OTH4_COM("http://sid.oth4.com/");

	private String url;

	private WebViewType(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
