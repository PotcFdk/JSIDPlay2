package netsiddev_builder.commands;

import static netsiddev.Command.SET_SID_BALANCE;

public class SetSidBalance implements NetSIDPkg {
	private byte sidNum;
	private byte balanceL, balanceR;

	public SetSidBalance(byte sidNum, byte balanceL, byte balanceR) {
		this.sidNum = sidNum;
		this.balanceL = balanceL;
		this.balanceR = balanceR;
	}

	public byte[] toByteArray() {
		return new byte[] { (byte) SET_SID_BALANCE.ordinal(), sidNum, 0, 0, balanceL, balanceR };
	}
}
