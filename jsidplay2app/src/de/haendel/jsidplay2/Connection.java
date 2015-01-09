package de.haendel.jsidplay2;

public class Connection {
	private String hostname;
	private String port;
	private String username;
	private String password;

	public final String getHostname() {
		return hostname;
	}

	public final void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public final String getPort() {
		return port;
	}

	public final void setPort(String port) {
		this.port = port;
	}

	public final String getUsername() {
		return username;
	}

	public final void setUsername(String username) {
		this.username = username;
	}

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

}
