package libsidplay.sidtune;

@SuppressWarnings("serial")
public class SidTuneError extends Exception {
	public SidTuneError(final String error) {
		super(error);
	}

	@Override
	public String getMessage() {
		StringBuilder message = new StringBuilder();
		Throwable cause = getCause();
		if (cause != null) {
			message.append(cause.getMessage()).append(System.getProperty("line.separator"));
		}
		return message.append(super.getMessage()).toString();
	}
}
