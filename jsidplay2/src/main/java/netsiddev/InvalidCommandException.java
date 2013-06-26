package netsiddev;

public class InvalidCommandException extends Exception {
	private final int dataLen;

	public InvalidCommandException(String string) {
		super(string);
		dataLen = -1;
	}

	public InvalidCommandException(String error, int length) {
		super(error);
		dataLen = length;
	}

	@Override
	public String getMessage() {
		String error = super.getMessage();
		if (dataLen >= 0) {
			error += ", but attached data packet is " + dataLen + " bytes long";
		}
		return error;
	}
	
	
}
