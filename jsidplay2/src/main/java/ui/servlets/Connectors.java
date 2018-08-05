package ui.servlets;

public enum Connectors {
	HTTP_ONLY("http"), HTTP_HTTPS("https"), HTTPS("https");

	private String preferredProtocol;

	private Connectors(String preferredProtocol) {
		this.preferredProtocol = preferredProtocol;
	}

	public String getPreferredProtocol() {
		return preferredProtocol;
	}
}
