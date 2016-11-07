package netsiddev_builder.commands;

import static netsiddev_builder.NetSIDCommand.CMD_MUTE;

public class Mute implements NetSIDPkg {
	private byte sidNum;
	private byte voice;
	private byte mute;

	public Mute(byte sidNum, byte voice, boolean mute) {
		this.sidNum = sidNum;
		this.voice = voice;
		this.mute = (byte) (mute ? 1 : 0);
	}

	public byte[] toByteArray() {
		return new byte[] { CMD_MUTE.getCmd(), sidNum, 0, 0, voice, mute };
	}
}
