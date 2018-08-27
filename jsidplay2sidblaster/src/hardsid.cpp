#include "sidblaster_builder_SIDBlasterSID.h"
#include "StdAfx.h"

// Original SIDBlaster driver
static HINSTANCE hOrigDll;

// Function Pointers to original hardsid.dll
typedef BYTE (CALLBACK* LPFNDLL_GETHARDSIDCOUNT)();
static LPFNDLL_GETHARDSIDCOUNT pfnGetHardSIDCount;
typedef BYTE (CALLBACK* LPFNDLL_HARDSIDDEVICES)();
static LPFNDLL_HARDSIDDEVICES pfnHardSID_Devices;
typedef BYTE (CALLBACK* LPFNDLL_INITHARDSID_MAPPER)();
static LPFNDLL_INITHARDSID_MAPPER pfnInitHardSID_Mapper;
typedef BYTE (CALLBACK* LPFNDLL_MUTEHARDSID_LINE)(BOOL);
static LPFNDLL_MUTEHARDSID_LINE pfnMuteHardSID_Line;
typedef void (CALLBACK* LPFNDLL_WRITETOHARDSID)(BYTE,BYTE,BYTE);
static LPFNDLL_WRITETOHARDSID pfnWriteToHardSID;
typedef BYTE (CALLBACK* LPFNDLL_READFROMHARDSID)(BYTE,BYTE);
static LPFNDLL_READFROMHARDSID pfnReadFromHardSID;
typedef void (CALLBACK* LPFNDLL_HARDSID_FILTER)(BYTE,BOOL);
static LPFNDLL_HARDSID_FILTER pfnHardSID_Filter;
typedef void (CALLBACK* LPFNDLL_HARDSID_FLUSH)(BYTE);
static LPFNDLL_HARDSID_FLUSH pfnHardSID_Flush;
typedef void (CALLBACK* LPFNDLL_HARDSID_MUTE)(BYTE,BYTE,BOOL);
static LPFNDLL_HARDSID_MUTE pfnHardSID_Mute;
typedef void (CALLBACK* LPFNDLL_HARDSID_RESET)(BYTE);
static LPFNDLL_HARDSID_RESET pfnHardSID_Reset;
typedef BYTE (CALLBACK* LPFNDLL_HARDSID_READ)(BYTE, WORD, BYTE);
static LPFNDLL_HARDSID_READ pfnHardSID_Read;
typedef void (CALLBACK* LPFNDLL_HARDSID_SYNC)(BYTE);
static LPFNDLL_HARDSID_SYNC pfnHardSID_Sync;
typedef void (CALLBACK* LPFNDLL_HARDSID_WRITE)(BYTE,WORD,BYTE,BYTE);
static LPFNDLL_HARDSID_WRITE pfnHardSID_Write;
typedef WORD (CALLBACK* LPFNDLL_HARDSID_VERSION)();
static LPFNDLL_HARDSID_VERSION pfnHardSID_Version;
typedef void (CALLBACK* LPFNDLL_HARDSID_RESET2)(BYTE,BYTE);
static LPFNDLL_HARDSID_RESET2 pfnHardSID_Reset2;
typedef BOOL (CALLBACK* LPFNDLL_HARDSID_LOCK)(BYTE);
static LPFNDLL_HARDSID_LOCK pfnHardSID_Lock;
typedef void (CALLBACK* LPFNDLL_HARDSID_UNLOCK)(BYTE);
static LPFNDLL_HARDSID_UNLOCK pfnHardSID_Unlock;
typedef void (CALLBACK* LPFNDLL_HARDSID_SOFTFLUSH)(BYTE);
static LPFNDLL_HARDSID_SOFTFLUSH pfnHardSID_SoftFlush;
typedef void (CALLBACK* LPFNDLL_HARDSID_MUTEALL)(BYTE,BOOL);
static LPFNDLL_HARDSID_MUTEALL pfnHardSID_MuteAll;
typedef void (CALLBACK* LPFNDLL_HARDSID_DELAY)(BYTE,WORD);
static LPFNDLL_HARDSID_DELAY pfnHardSID_Delay;
typedef BYTE (CALLBACK* LPFNDLL_HARDSID_TRY_WRITE)(BYTE,WORD,BYTE,BYTE);
static LPFNDLL_HARDSID_TRY_WRITE pfnHardSID_Try_Write;

static BOOL resolve_hardsid_symbols(char *moduleFilename)
{
	char *lastBSlash = strrchr(moduleFilename, '\\');
	char origDriver[MAX_PATH];
	memset(origDriver,0,MAX_PATH);
	strncpy( origDriver, moduleFilename, (lastBSlash-moduleFilename) );
	strcat( origDriver, "\\hardsid.dll");

	// Load original driver and read function pointers
	// to call its functions.
	hOrigDll = LoadLibrary(origDriver);
	if (! hOrigDll) {
		/* no hardsid.dll; we can manage. */
		MessageBox(GetActiveWindow(),
			origDriver,
			"Original Driver missing", MB_OK | MB_ICONERROR);
		return TRUE;
	}

	pfnHardSID_Version = (LPFNDLL_HARDSID_VERSION)GetProcAddress(hOrigDll, "HardSID_Version");
	if (!pfnHardSID_Version) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Version in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnGetHardSIDCount = (LPFNDLL_GETHARDSIDCOUNT)GetProcAddress(hOrigDll, "GetHardSIDCount");
	if (!pfnGetHardSIDCount) {
		MessageBox(GetActiveWindow(),
			"Function: GetHardSIDCount in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Devices = (LPFNDLL_HARDSIDDEVICES)GetProcAddress(hOrigDll, "HardSID_Devices");
	if (!pfnHardSID_Devices) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Devices in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnInitHardSID_Mapper = (LPFNDLL_INITHARDSID_MAPPER)GetProcAddress(hOrigDll, "InitHardSID_Mapper");
	if (!pfnInitHardSID_Mapper) {
		MessageBox(GetActiveWindow(),
			"Function: InitHardSID_Mapper in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnMuteHardSID_Line = (LPFNDLL_MUTEHARDSID_LINE)GetProcAddress(hOrigDll, "MuteHardSID_Line");
	if (!pfnMuteHardSID_Line) {
		MessageBox(GetActiveWindow(),
			"Function: MuteHardSID_Line in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnWriteToHardSID = (LPFNDLL_WRITETOHARDSID)GetProcAddress(hOrigDll, "WriteToHardSID");
	if (!pfnWriteToHardSID) {
		MessageBox(GetActiveWindow(),
			"Function: WriteToHardSID in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnReadFromHardSID = (LPFNDLL_READFROMHARDSID)GetProcAddress(hOrigDll, "ReadFromHardSID");
	if (!pfnReadFromHardSID) {
		MessageBox(GetActiveWindow(),
			"Function: ReadFromHardSID in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Filter = (LPFNDLL_HARDSID_FILTER)GetProcAddress(hOrigDll, "HardSID_Filter");
	if (!pfnHardSID_Filter) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Filter in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Flush = (LPFNDLL_HARDSID_FLUSH)GetProcAddress(hOrigDll, "HardSID_Flush");
	if (!pfnHardSID_Flush) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Flush in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Mute = (LPFNDLL_HARDSID_MUTE)GetProcAddress(hOrigDll, "HardSID_Mute");
	if (!pfnHardSID_Mute) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Mute in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Reset = (LPFNDLL_HARDSID_RESET)GetProcAddress(hOrigDll, "HardSID_Reset");
	if (!pfnHardSID_Reset) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Reset in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Read = (LPFNDLL_HARDSID_READ)GetProcAddress(hOrigDll, "HardSID_Read");
	if (!pfnHardSID_Read) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Read in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Sync = (LPFNDLL_HARDSID_SYNC)GetProcAddress(hOrigDll, "HardSID_Sync");
	if (!pfnHardSID_Sync) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Sync in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Write = (LPFNDLL_HARDSID_WRITE)GetProcAddress(hOrigDll, "HardSID_Write");
	if (!pfnHardSID_Write) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Write in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Reset2 = (LPFNDLL_HARDSID_RESET2)GetProcAddress(hOrigDll, "HardSID_Reset2");
	if (!pfnHardSID_Reset2) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Reset2 in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Lock = (LPFNDLL_HARDSID_LOCK)GetProcAddress(hOrigDll, "HardSID_Lock");
	if (!pfnHardSID_Lock) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Lock in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Unlock = (LPFNDLL_HARDSID_UNLOCK)GetProcAddress(hOrigDll, "HardSID_Unlock");
	if (!pfnHardSID_Unlock) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Unlock in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_SoftFlush = (LPFNDLL_HARDSID_SOFTFLUSH)GetProcAddress(hOrigDll, "HardSID_SoftFlush");
	if (!pfnHardSID_SoftFlush) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_SoftFlush in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_MuteAll = (LPFNDLL_HARDSID_MUTEALL)GetProcAddress(hOrigDll, "HardSID_MuteAll");
	if (!pfnHardSID_MuteAll) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_MuteAll in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Delay = (LPFNDLL_HARDSID_DELAY)GetProcAddress(hOrigDll, "HardSID_Delay");
	if (!pfnHardSID_Delay) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Delay in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Try_Write = (LPFNDLL_HARDSID_TRY_WRITE)GetProcAddress(hOrigDll, "HardSID_Try_Write");
	if (!pfnHardSID_Try_Write) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Try_Write in HardSID.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}

	return TRUE;
}

typedef enum {
	HSID_USB_WSTATE_OK = 1,
	HSID_USB_WSTATE_BUSY,
	HSID_USB_WSTATE_ERROR,
	HSID_USB_WSTATE_END
} HSID_USB_WSTATE;

static void cleanup()
{
	if (hOrigDll != NULL) {
		FreeLibrary(hOrigDll);
		hOrigDll = NULL;
	}
}
BOOL APIENTRY DllMain(HINSTANCE hModule, DWORD ul_reason_for_call,
		LPVOID lpReserved) {
	switch (ul_reason_for_call) {
	  case DLL_PROCESS_ATTACH:
			char moduleFilename[MAX_PATH];
			GetModuleFileName((HMODULE)hModule, moduleFilename, sizeof(moduleFilename));
			hOrigDll = NULL;

			if (! resolve_hardsid_symbols(moduleFilename)) {
				cleanup();
				return FALSE;
			}
	    break;

	  case DLL_PROCESS_DETACH:
			cleanup();
	    break;

	case DLL_THREAD_ATTACH:
		break;

	case DLL_THREAD_DETACH:
		break;
	}

	return TRUE;
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_DeviceCount
 * Signature: ()B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1DeviceCount(
		JNIEnv *, jobject) {
	// Get the real SIDBlaster devices
	BYTE result = (*pfnHardSID_Devices)();
	return (jbyte) result;
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_SIDCount
 * Signature: (B)B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1SIDCount(
		JNIEnv *, jobject, jbyte deviceId) {
	//BYTE DeviceID = deviceId;
	// Get the real SIDBlaster devices (just one SID per hardware device)
	BYTE result = 1;
	return (jbyte) result;
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_Read
 * Signature: (BBSB)B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1Read(
		JNIEnv *, jobject, jbyte deviceId, jbyte sidNum, jshort cycles, jbyte reg) {
	BYTE DeviceID = deviceId;
	//BYTE SidNum = sidNum;
	BYTE Cycles = cycles;
	BYTE SID_reg = reg;
	if (Cycles > 0) {
		(*pfnHardSID_Delay)(DeviceID, Cycles);
	}
	BYTE result = (*pfnReadFromHardSID)(DeviceID, SID_reg);
	return (jbyte) result;
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_Write
 * Signature: (BBSBB)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1Write(
		JNIEnv *, jobject, jbyte deviceIdx, jbyte sidNum, jshort cycles, jbyte reg,
		jbyte dat) {
	BYTE DeviceID = deviceIdx;
	//BYTE SidNum = sidNum;
	BYTE Cycles = cycles;
	BYTE SID_reg = reg;
	BYTE Data = dat;
	//issues a write
 	//if the hardware buffer is full, sleeps the thread until there is some space for this write
	while ((*pfnHardSID_Try_Write)(DeviceID, Cycles, SID_reg, Data) == HSID_USB_WSTATE_BUSY) {
		Sleep(0);
	}
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_Reset
 * Signature: (B)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1Reset(
		JNIEnv *, jobject, jbyte deviceIdx) {
	BYTE DeviceID = deviceIdx;
	(*pfnHardSID_Reset)(DeviceID);
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_Delay
 * Signature: (BS)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1Delay(
		JNIEnv *, jobject, jbyte deviceIdx, jshort cycles) {
	BYTE DeviceID = deviceIdx;
	WORD Cycles = cycles;
	(*pfnHardSID_Delay)(DeviceID, Cycles);
}

/*
 * Class:     sidblaster_builder_SIDBlasterSID
 * Method:    HardSID_Flush
 * Signature: (B)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_sidblaster_1builder_SIDBlasterSID_HardSID_1Flush(
		JNIEnv *, jobject, jbyte deviceIdx) {
	BYTE DeviceID = deviceIdx;
	(*pfnHardSID_Flush)(DeviceID);
}
