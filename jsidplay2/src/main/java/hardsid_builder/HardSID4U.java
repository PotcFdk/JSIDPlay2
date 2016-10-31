package hardsid_builder;

public class HardSID4U {
	native byte HardSID_DeviceCount();

	native byte HardSID_SIDCount(byte DeviceID);

	native byte HardSID_Read(byte DeviceID, byte chipNum, byte SID_reg);

	native void HardSID_Write(byte DeviceID, byte chipNum, byte SID_reg, byte data);

	native void HardSID_Reset(byte DeviceID);

	native void HardSID_Delay(byte DeviceID, short Cycles);

	native void HardSID_Flush(byte DeviceID);
}
