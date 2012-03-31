typedef void (CALLBACK* lpInitHardSID_Mapper)(void);
typedef WORD (CALLBACK* HsidDLL2_Version_t) (void);
typedef void (CALLBACK* lpHardSID_Delay)(BYTE DeviceID, WORD Cycles);
typedef BYTE (CALLBACK* HsidDLL2_Devices_t) (void);
typedef void (CALLBACK* HsidDLL2_Filter_t) (BYTE DeviceID, BOOL filter);
typedef void (CALLBACK* HsidDLL2_Flush_t) (BYTE DeviceID);
typedef void (CALLBACK* HsidDLL2_MuteAll_t) (BYTE DeviceID, BOOL mute);
typedef BYTE (CALLBACK* HsidDLL2_Read_t) (BYTE DeviceID, WORD cycles, BYTE SID_reg);
typedef void (CALLBACK* HsidDLL2_Sync_t) (BYTE DeviceID);
typedef void (CALLBACK* HsidDLL2_Write_t) (BYTE DeviceID, WORD cycles, BYTE SID_reg, BYTE data);
typedef void (CALLBACK* HsidDLL2_Reset_t) (BYTE DeviceID);
typedef BOOL (CALLBACK* HsidDLL2_Lock_t) (BYTE DeviceID);
typedef void (CALLBACK* HsidDLL2_Unlock_t) (BYTE DeviceID);
typedef void (CALLBACK* HsidDLL2_Reset2_t) (BYTE DeviceID, BYTE volume);
typedef void (CALLBACK* HsidDLL2_Mute2_t) (BYTE DeviceID, BYTE channel, BOOL mute, BOOL x);
typedef void (CALLBACK* HsidDLL2_Mute_t) (BYTE DeviceID, BYTE channel, BOOL mute);
typedef void (CALLBACK* lpHardSID_SoftFlush)(BYTE DeviceID);
typedef void (CALLBACK* lpMuteHardSID_Line)(int Mute);
typedef BYTE (CALLBACK* lpReadFromHardSID)(BYTE DeviceID, BYTE SID_reg);
typedef void (CALLBACK* lpWriteToHardSID)(BYTE DeviceID, BYTE SID_reg, BYTE Data);
typedef void (CALLBACK* HsidDLL2_SetDebug_t) (BOOL debug);
typedef BYTE (CALLBACK* HsidDLL2_GetHardSIDCount_t) ();



// HardSID DLL
HMODULE hardsiddll;
// hardsid.dll functions
lpInitHardSID_Mapper InitHardSID_Mapper;
HsidDLL2_Version_t HardSID_Version;
lpHardSID_Delay HardSID_Delay;
HsidDLL2_Devices_t HardSID_Devices;
HsidDLL2_Filter_t HardSID_Filter;
HsidDLL2_Flush_t HardSID_Flush;
HsidDLL2_MuteAll_t HardSID_MuteAll;
HsidDLL2_Read_t HardSID_Read;
HsidDLL2_Sync_t HardSID_Sync;
HsidDLL2_Write_t HardSID_Write;
HsidDLL2_Reset_t HardSID_Reset;
HsidDLL2_Lock_t HardSID_Lock;
HsidDLL2_Unlock_t HardSID_Unlock;
HsidDLL2_Reset2_t HardSID_Reset2;
HsidDLL2_Mute_t HardSID_Mute;
HsidDLL2_Mute2_t HardSID_Mute2;
lpHardSID_SoftFlush HardSID_SoftFlush;
lpMuteHardSID_Line MuteHardSID_Line;
lpReadFromHardSID ReadFromHardSID;
lpWriteToHardSID WriteToHardSID;
HsidDLL2_SetDebug_t SetDebug;
HsidDLL2_GetHardSIDCount_t GetHardSIDCount;
