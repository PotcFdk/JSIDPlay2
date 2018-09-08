package server.restful.common;

public enum Connectors {
	HTTP("http", 8080), HTTP_HTTPS("https", 8443), HTTPS("https", 8443);

	private String preferredProtocol;
	private int portNum;

	private Connectors(String preferredProtocol, int portNum) {
		this.preferredProtocol = preferredProtocol;
		this.portNum = portNum;
	}

	public String getPreferredProtocol() {
		return preferredProtocol;
	}
	
	public int getPortNum() {
		return portNum;
	}
}
