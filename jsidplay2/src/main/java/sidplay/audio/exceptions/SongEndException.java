package sidplay.audio.exceptions;

@SuppressWarnings("serial")
public class SongEndException extends RuntimeException {
	public SongEndException() {
	}

	public SongEndException(Exception e) {
		super(e.getMessage());
	}
}