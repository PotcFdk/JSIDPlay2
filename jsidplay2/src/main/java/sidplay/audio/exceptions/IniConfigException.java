package sidplay.audio.exceptions;

import java.io.IOException;

@SuppressWarnings("serial")
public class IniConfigException extends IOException {
	public IniConfigException() {
	}

	public IniConfigException(String message) {
		super(message);
	}
}