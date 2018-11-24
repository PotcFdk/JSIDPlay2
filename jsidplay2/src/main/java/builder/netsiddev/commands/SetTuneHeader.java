package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_SID_HEADER;

public class SetTuneHeader implements NetSIDPkg {
	private final byte[] header;

	public SetTuneHeader(byte[] header) {
		this.header = header;
	}

	public byte[] toByteArray() {
		int i = 0;
		byte[] cmd = new byte[6 + header.length];
		cmd[i++] = (byte) SET_SID_HEADER.ordinal();
		cmd[i++] = 0;
		cmd[i++] = 0;
		cmd[i++] = 0;
		cmd[i++] = (byte) ((header.length >> 8) & 0xff);
		cmd[i++] = (byte) (header.length & 0xff);
		for (byte headerByte : header) {
			cmd[i++] = headerByte;
		}
		return cmd;
	}
}
