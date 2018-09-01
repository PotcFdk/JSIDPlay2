package builder.sidblaster;

import com.sun.jna.Library;

public interface HardSID extends Library {

	int HSID_USB_WSTATE_BUSY = 2;
	
	// Version 2 Interface
	
	void HardSID_Delay(byte DeviceID, short Cycles);

	byte HardSID_Devices();

	void HardSID_Filter(byte DeviceID, boolean filter);

	void HardSID_Flush(byte DeviceID);

	void HardSID_Mute(byte DeviceID, byte voice, boolean mute);

	void HardSID_MuteAll(byte DeviceID, boolean mute);

	byte HardSID_Read(byte DeviceID, short Cycles, byte SID_reg);

	void HardSID_Reset(byte DeviceID);

	void HardSID_Sync(byte DeviceID);

	short HardSID_Version();

	byte HardSID_Write(byte DeviceID, short Cycles, byte SID_reg, byte data);

	// Version 2.04 Extensions
	
	void HardSID_Reset2(byte DeviceID, byte volume);

	boolean HardSID_Lock(byte DeviceID);

	void HardSID_Unlock(byte DeviceID);

	// Version 2.07 Extensions
	
	void HardSID_Mute2(byte DeviceID, byte channel, boolean mute, boolean manual);

	// Version 2.09 Extensions

	short HardSID_Clock(byte DeviceID, byte preset);

	
	
	byte GetHardSIDCount(byte DeviceID);

	void HardSID_SoftFlush(byte DeviceID);

	byte InitHardSID_Mapper();

	byte MuteHardSID_Line(boolean mute);

	byte ReadFromHardSID(byte DeviceID, byte SID_reg);

	void WriteToHardSID(byte DeviceID, byte SID_reg, byte data);

	byte HardSID_Try_Write(byte DeviceID, short Cycles, byte SID_reg, byte data);

	// void HardSID_ExternalTiming ?

}
