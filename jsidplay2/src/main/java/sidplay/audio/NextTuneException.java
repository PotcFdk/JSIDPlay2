package sidplay.audio;

@SuppressWarnings("serial")
public class NextTuneException extends RuntimeException {
	public NextTuneException() {
	}

	public NextTuneException(Exception e) {
		super(e.getMessage());
	}
}