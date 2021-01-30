package sidplay.audio.exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class IniConfigException extends IOException {

	private Runnable configRepairer;

	public IniConfigException() {
	}

	public IniConfigException(String message, Runnable configRepairer) {
		super(message);
		this.configRepairer = configRepairer;
	}

	public Runnable getConfigRepairer() {
		return configRepairer;
	}
}