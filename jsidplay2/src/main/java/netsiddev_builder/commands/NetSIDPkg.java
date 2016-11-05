package netsiddev_builder.commands;

public interface NetSIDPkg {
	byte[] toByteArray();
	default byte[] toByteArrayWithLength() {
		byte[] arr = toByteArray();
		/*
		 * Fill in packet data length so that server knows when entire
		 * packet has been read.
		 */
		arr[2] = (byte) (((arr.length - 4) >> 8) & 0xff);
		arr[3] = (byte) ((arr.length - 4) & 0xff);
		return arr;
	}
}
