package builder.hardsid;

import com.sun.jna.Library;

public interface HardSID extends Library {
	byte HardSID_Devices();

	byte HardSID_SIDCount(byte deviceID);

	void HardSID_Flush(byte deviceID);

	void HardSID_Reset(byte deviceID);
	
	void HardSID_Delay(byte deviceID, short cycles);
	
	void HardSID_Write(byte deviceID, byte sidNum, byte reg, byte data);

}
