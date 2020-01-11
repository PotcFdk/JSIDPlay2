package sidplay.ini;

import java.io.IOException;

@SuppressWarnings("serial")
public class IniConfigException extends IOException {
	public IniConfigException() {
	}
	public IniConfigException(String message) {
		super(message);
	}
}