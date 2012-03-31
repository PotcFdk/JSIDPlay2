package hardsid_builder;

public class HsidDLL2 {

	/**
	 * Load hardsid.dll with a different name
	 */
	public native boolean LoadLibrary(String libName);

	/**
	 * typedef void (CALLBACK* lpInitHardSID_Mapper)(void);<BR>
	 * InitHardSID_Mapper = (lpInitHardSID_Mapper) GetProcAddress(hardsiddll,
	 * "InitHardSID_Mapper");
	 */
	public native void InitHardSID_Mapper();

	/**
	 * typedef WORD (CALLBACK* HsidDLL2_Version_t) (void);<BR>
	 * version = (HsidDLL2_Version_t) GetProcAddress(hardsiddll,
	 * "HardSID_Version");
	 * 
	 * @return version
	 */
	public native int HardSID_Version();

	/**
	 * typedef void (CALLBACK* lpHardSID_Delay)(Uint8 DeviceID, Uint16 Cycles);<BR>
	 * HardSID_Delay = (lpHardSID_Delay) GetProcAddress(hardsiddll,
	 * "HardSID_Delay");
	 * 
	 * @param DeviceID
	 * @param Cycles
	 */
	public native void HardSID_Delay(int DeviceID, int Cycles);

	/**
	 * typedef BYTE (CALLBACK* HsidDLL2_Devices_t) (void);<BR>
	 * Devices = (HsidDLL2_Devices_t) GetProcAddress(hardsiddll,
	 * "HardSID_Devices");
	 * 
	 * @return
	 */
	public native int HardSID_Devices();

	/**
	 * typedef void (CALLBACK* HsidDLL2_Filter_t) (BYTE DeviceID, BOOL filter);<BR>
	 * Filter = (HsidDLL2_Filter_t) GetProcAddress(hardsiddll,
	 * "HardSID_Filter");
	 * 
	 * @param DeviceID
	 * @param filter
	 */
	public native void HardSID_Filter(int DeviceID, boolean filter);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Flush_t) (BYTE DeviceID);<BR>
	 * HardSID_Flush = (HsidDLL2_Flush_t) GetProcAddress(hardsiddll,
	 * "HardSID_Flush");
	 * 
	 * @param DeviceID
	 */
	public native void HardSID_Flush(int DeviceID);

	/**
	 * typedef void (CALLBACK* HsidDLL2_MuteAll_t) (BYTE DeviceID, BOOL mute);<BR>
	 * MuteAll = (HsidDLL2_MuteAll_t) GetProcAddress(hardsiddll,
	 * "HardSID_MuteAll");
	 * 
	 * @param DeviceID
	 * @param mute
	 */
	public native void HardSID_MuteAll(int DeviceID, boolean mute);

	/**
	 * typedef BYTE (CALLBACK* HsidDLL2_Read_t) (BYTE DeviceID, WORD cycles,
	 * BYTE SID_reg);<BR>
	 * Read = (HsidDLL2_Read_t) GetProcAddress(hardsiddll, "HardSID_Read");
	 * 
	 * @param DeviceID
	 * @param cycles
	 * @param SID_reg
	 * @return
	 */
	public native int HardSID_Read(int DeviceID, int cycles, int SID_reg);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Sync_t) (BYTE DeviceID);<BR>
	 * Sync = (HsidDLL2_Sync_t) GetProcAddress(hardsiddll, "HardSID_Sync");
	 * 
	 * @param DeviceID
	 */
	public native void HardSID_Sync(int DeviceID);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Write_t) (BYTE DeviceID, WORD cycles,
	 * BYTE SID_reg, BYTE data);<BR>
	 * HardSID_Write = (HsidDLL2_Write_t) GetProcAddress(hardsiddll,
	 * "HardSID_Write");
	 * 
	 * @param DeviceID
	 * @param cycles
	 * @param SID_reg
	 * @param data
	 */
	public native void HardSID_Write(int DeviceID, int cycles, int SID_reg,
			int data);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Reset_t) (BYTE DeviceID);<BR>
	 * Reset = (HsidDLL2_Reset_t) GetProcAddress(hardsiddll, "HardSID_Reset");
	 * 
	 * @deprecated Version < 2.04
	 * @param DeviceID
	 */
	@Deprecated
	public native void HardSID_Reset(int DeviceID);

	/**
	 * typedef BOOL (CALLBACK* HsidDLL2_Lock_t) (BYTE DeviceID);<BR>
	 * Lock = (HsidDLL2_Lock_t) GetProcAddress(hardsiddll, "HardSID_Lock");
	 * 
	 * @since 2.04
	 * 
	 * @param DeviceID
	 * @return
	 */
	public native boolean HardSID_Lock(int DeviceID);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Unlock_t) (BYTE DeviceID);<BR>
	 * Unlock = (HsidDLL2_Unlock_t) GetProcAddress(hardsiddll,
	 * "HardSID_Unlock");
	 * 
	 * @since 2.04
	 * 
	 * @param DeviceID
	 */
	public native void HardSID_Unlock(int DeviceID);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Reset2_t) (BYTE DeviceID, BYTE volume);<BR>
	 * Reset2 = (HsidDLL2_Reset2_t) GetProcAddress(hardsiddll,
	 * "HardSID_Reset2");
	 * 
	 * @since 2.04
	 * 
	 * @param DeviceID
	 * @param volume
	 */
	public native void HardSID_Reset2(int DeviceID, int volume);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Mute_t) (BYTE DeviceID, BYTE channel,
	 * BOOL mute);<BR>
	 * Mute = (HsidDLL2_Mute_t) GetProcAddress(hardsiddll, "HardSID_Mute");
	 * 
	 * @param DeviceID
	 * @param channel
	 * @param mute
	 */
	public native void HardSID_Mute(int DeviceID, int channel, boolean mute);

	/**
	 * typedef void (CALLBACK* HsidDLL2_Mute2_t) (BYTE DeviceID, BYTE channel,
	 * BOOL mute, BOOL x);<BR>
	 * Mute2 = (HsidDLL2_Mute2_t) GetProcAddress(hardsiddll, "HardSID_Mute2");
	 * 
	 * @param DeviceID
	 * @param channel
	 * @param mute
	 * @param x ?
	 * @since 2.07
	 */
	public native void HardSID_Mute2(int DeviceID, int channel, boolean mute, boolean x);

	/**
	 * typedef void (CALLBACK* lpHardSID_SoftFlush)(Uint8 DeviceID);<BR>
	 * HardSID_SoftFlush = (lpHardSID_SoftFlush) GetProcAddress(hardsiddll,
	 * "HardSID_SoftFlush");
	 * 
	 * @param DeviceID
	 */
	public native void HardSID_SoftFlush(int DeviceID);

	/**
	 * typedef void (CALLBACK* lpMuteHardSID_Line)(int Mute);<BR>
	 * MuteHardSID_Line = (lpMuteHardSID_Line) GetProcAddress(hardsiddll,
	 * "MuteHardSID_Line");
	 * 
	 * @param Mute
	 */
	public native void MuteHardSID_Line(int Mute);

	/**
	 * typedef Uint8 (CALLBACK* lpReadFromHardSID)(Uint8 DeviceID, Uint8
	 * SID_reg);<BR>
	 * ReadFromHardSID = (lpReadFromHardSID) GetProcAddress(hardsiddll,
	 * "ReadFromHardSID");
	 * 
	 * @param DeviceID
	 * @param SID_reg
	 * @return
	 */
	public native int ReadFromHardSID(int DeviceID, int SID_reg);

	/**
	 * typedef void (CALLBACK* lpWriteToHardSID)(Uint8 DeviceID, Uint8 SID_reg,
	 * Uint8 Data);<BR>
	 * WriteToHardSID = (lpWriteToHardSID) GetProcAddress(hardsiddll,
	 * "WriteToHardSID");
	 * 
	 * @param DeviceID
	 * @param SID_reg
	 * @param Data
	 */
	public native void WriteToHardSID(int DeviceID, int SID_reg, int Data);

	/**
	 * typedef void (CALLBACK* HsidDLL2_SetDebug_t) (BOOL debug);<BR>
	 * SetDebug = (HsidDLL2_SetDebug_t) GetProcAddress(hardsiddll, "SetDebug");
	 * 
	 * @param debug
	 */
	public native void SetDebug(boolean debug);

	/**
	 * typedef BYTE (CALLBACK* HsidDLL2_GetHardSIDCount_t) ();<BR>
	 * GetHardSIDCount = (HsidDLL2_GetHardSIDCount_t) GetProcAddress(hardsiddll, "GetHardSIDCount");
	 * 
	 * @param debug
	 */
	public native int GetHardSIDCount();
	
	// unknown functions:

	// GetDLLVersion, HardSID_Group, HardSID_Try_Write,
	// MuteHardSID, MuteHardSIDAll
}
