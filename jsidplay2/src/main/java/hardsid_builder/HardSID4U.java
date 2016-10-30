package hardsid_builder;

public class HardSID4U {
	native int HardSID_DeviceCount();

	native int HardSID_SIDCount(int DeviceID);

	native int HardSID_Read(int DeviceID, int chipNum, int cycles, int SID_reg);

	native void HardSID_Write(int DeviceID, int chipNum, int cycles, int SID_reg, int data);

	native void HardSID_Reset(int DeviceID);

	native void HardSID_Delay(int DeviceID, int Cycles);

	native void HardSID_Flush(int DeviceID);
}
