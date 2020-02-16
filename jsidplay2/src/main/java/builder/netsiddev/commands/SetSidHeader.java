package builder.netsiddev.commands;

import static server.netsiddev.Command.SET_SID_HEADER;

public class SetSidHeader implements NetSIDPkg {
	private final byte[] header;

	public SetSidHeader(byte[] header) {
		this.header = header;
	}

	public byte[] toByteArray() {
		int i = 0;
		byte[] cmd = new byte[4 + header.length];
		cmd[i++] = (byte) SET_SID_HEADER.ordinal();
		cmd[i++] = 0;
		cmd[i++] = 0;
		cmd[i++] = 0;
		for (byte headerByte : header) {
			cmd[i++] = headerByte;
		}
		return cmd;
	}
}
