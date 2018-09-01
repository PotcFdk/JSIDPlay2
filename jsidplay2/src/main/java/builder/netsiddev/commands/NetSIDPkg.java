package builder.netsiddev.commands;

public interface NetSIDPkg {
	byte[] toByteArray();

	/**
	 * Fill in packet data length so that server knows when entire packet has
	 * been read.
	 * 
	 * @return byte array containing the command with length information
	 */
	default byte[] toByteArrayWithLength() {
		byte[] arr = toByteArray();
		arr[2] = (byte) ((arr.length - 4 >> 8) & 0xff);
		arr[3] = (byte) (arr.length - 4 & 0xff);
		return arr;
	}
}
