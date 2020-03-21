package sidplay.audio.exceptions;

@SuppressWarnings("serial")
public class EndTuneException extends InterruptedException {
	public EndTuneException() {
	}

	public EndTuneException(Exception e) {
		super(e.getMessage());
	}
}