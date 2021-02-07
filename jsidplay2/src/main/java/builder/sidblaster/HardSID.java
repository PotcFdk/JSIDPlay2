package builder.sidblaster;

import com.sun.jna.Library;
import com.sun.jna.Memory;

import builder.hardsid.WState;

public interface HardSID extends Library {

	// Version 2 Interface (Cycle exact interface)

	void HardSID_Delay(byte DeviceID, short Cycles);

	byte HardSID_Devices();

	void HardSID_Filter(byte DeviceID, boolean filter);

	/**
	 * Empties the devices FIFO without playing it (if supported)
	 */
	void HardSID_Flush(byte DeviceID);

	void HardSID_Mute(byte DeviceID, byte voice, boolean mute);

	void HardSID_MuteAll(byte DeviceID, boolean mute);

	byte HardSID_Read(byte DeviceID, short Cycles, byte SID_reg);

	void HardSID_Reset(byte DeviceID);

	/**
	 * Empties the devices FIFO by playing it (if supported)
	 */
	void HardSID_Sync(byte DeviceID);

	void HardSID_Write(byte DeviceID, short Cycles, byte SID_reg, byte data);

	/**
	 * Internal version number for this DLL e.g. 0x0202 (2.02)
	 */
	short HardSID_Version();

	// Official HardSID interface (depreciated)

	@Deprecated
	byte GetHardSIDCount();

	@Deprecated
	void InitHardSID_Mapper();

	@Deprecated
	void MuteHardSID_Line(boolean mute);

	@Deprecated
	byte ReadFromHardSID(byte DeviceID, byte SID_reg);

	@Deprecated
	void setDebug(boolean enabled);

	@Deprecated
	void WriteToHardSID(byte DeviceID, byte SID_reg, byte data);

	// Mute Extensions (depreciated)

	@Deprecated
	short GetDLLVersion();

	@Deprecated
	void MuteHardSID(byte deviceID, byte channel, boolean mute);

	@Deprecated
	void MuteHardSIDAll(byte deviceID, boolean mute);

	// Version 2.04 Extensions

	/**
	 * Click reduction
	 *
	 * @since 2.04
	 */
	void HardSID_Reset2(byte DeviceID, byte volume);

	/**
	 * Lock SID to application
	 *
	 * @since 2.04
	 */
	boolean HardSID_Lock(byte DeviceID);

	/**
	 * @since 2.04
	 */
	void HardSID_Unlock(byte DeviceID);

	/**
	 * Add SID to group when enable is true. SID can only be added or moved to an
	 * existing group. If deviceID = groupID then a new group is created with the
	 * SID device becoming group master. Only writes to the master are played on the
	 * other grouped SIDs.
	 *
	 * @since 2.04
	 */
	boolean HardSID_Group(byte deviceID, boolean enable, byte groupID);

	// Version 2.07 Extensions

	/**
	 * Support whether the channel change was a request from the user or the program
	 * (auto or manual respectively). External mixers can use this to prioritize
	 * requests
	 *
	 * @since 2.07
	 */
	void HardSID_Mute2(byte DeviceID, byte channel, boolean mute, boolean manual);

	// Version 2.08 Extensions

	/**
	 * Enable support for non hardsid hardware (e.g. Catweasel MK3/4)
	 *
	 * @since 2.08
	 */
	void HardSID_OtherHardware();

	// Version 2.09 Extensions

	/**
	 * @since 2.09
	 */
	short HardSID_Clock(byte DeviceID, byte preset);

	/**
	 * @since 2.09
	 */
	void HardSID_SoftFlush(byte DeviceID);

	/**
	 * @since 2.09
	 */
	WState HardSID_Try_Write(byte DeviceID, short Cycles, byte SID_reg, byte data);

	/**
	 * @since SIDBlasterUSB 5.14beta
	 * @since 2.02
	 */
	void HardSID_GetSerial(byte DeviceID, Memory mem);

	/**
	 * @since SIDBlasterUSB 5.15beta
	 * @since 2.03
	 */
	void HardSID_SetWriteBufferSize(byte bufferSize);

	SidType HardSID_GetSIDInfo(byte DeviceID);

	/**
	 * Note: this function is "death end", host program must terminate after call
	 * and the sidblaster must reconnect
	 * 
	 * @param sidType 0 (none), 1 (6581), 2 (8580)
	 */
	int HardSID_SetSIDInfo(byte DeviceID, SidType sidType);

	default String GetSerial(byte deviceID) {
		Memory mem = new Memory(9L);
		mem.clear();
		HardSID_GetSerial(deviceID, mem);
		return mem.getString(0L, "US_ASCII");
	}

}
