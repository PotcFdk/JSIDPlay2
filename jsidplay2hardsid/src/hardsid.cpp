#include "StdAfx.h"
#include "hardsid_builder_HardSID4U.h"

typedef enum {
	HSID_USB_WSTATE_OK = 1,
	HSID_USB_WSTATE_BUSY,
	HSID_USB_WSTATE_ERROR,
	HSID_USB_WSTATE_END
} HSID_USB_WSTATE;

extern "C" {
// initializes the management library in sync or async mode
// and selects SIDPlay mode on the device (sysmode=1)
bool hardsid_usb_init(BOOL syncmode, BYTE sysmode);

// closes the management library
void hardsid_usb_close(void);

// returns the number of USB HardSID devices plugged into the computer
BYTE hardsid_usb_getdevcount(void);

// returns the number of detected SID chips on the given device
BYTE hardsid_usb_getsidcount(BYTE dev_id);

// schedules a write command (sidNum<<5 is part of Reg)
HSID_USB_WSTATE hardsid_usb_write(BYTE dev_id, BYTE reg, BYTE data);

// plays the remaining data from the buffer
HSID_USB_WSTATE hardsid_usb_flush(BYTE dev_id);

// schedules a delay command
HSID_USB_WSTATE hardsid_usb_delay(BYTE dev_id, WORD cycles);

//aborts the playback ASAP
void hardsid_usb_abortplay(BYTE dev_id);
}

BOOL APIENTRY DllMain(HINSTANCE hModule, DWORD ul_reason_for_call,
		LPVOID lpReserved) {
	switch (ul_reason_for_call) {
	case DLL_PROCESS_ATTACH:
		hardsid_usb_init(TRUE, 1);
		break;

	case DLL_PROCESS_DETACH:
		hardsid_usb_close();
		break;

	case DLL_THREAD_ATTACH:
		break;

	case DLL_THREAD_DETACH:
		break;
	}

	return TRUE;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_DeviceCount
 * Signature: ()B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1DeviceCount(
		JNIEnv *, jobject) {
	BYTE erg = hardsid_usb_getdevcount();
	return (jbyte) erg;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_SIDCount
 * Signature: (B)B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1SIDCount(
		JNIEnv *, jobject, jbyte deviceId) {
	BYTE DeviceID = deviceId;
	BYTE Sids = hardsid_usb_getsidcount(DeviceID);
	return (jbyte) Sids;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Read
 * Signature: (BBSB)B
 */
__declspec(dllexport) JNIEXPORT jbyte JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Read(
		JNIEnv *, jobject, jbyte deviceId, jbyte sidNum, jshort cycles, jbyte reg) {
	BYTE DeviceID = deviceId;
	//BYTE SidNum = sidNum;
	BYTE Cycles = cycles;
	//BYTE SID_reg = reg;
	if (Cycles > 0) {
		while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
	return (jbyte) 0xff;	//unsupported read!
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Write
 * Signature: (BBSBB)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Write(
		JNIEnv *, jobject, jbyte deviceIdx, jbyte sidNum, jshort cycles, jbyte reg,
		jbyte dat) {
	BYTE DeviceID = deviceIdx;
	BYTE SidNum = sidNum;
	BYTE Cycles = cycles;
	BYTE SID_reg = reg;
	BYTE Data = dat;
	if (Cycles > 0) {
		while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
	//issues a write
	//if the hardware buffer is full, sleeps the thread until there is some space for this write
	while (hardsid_usb_write(DeviceID, (SidNum << 5) | SID_reg, Data)
			== HSID_USB_WSTATE_BUSY)
		Sleep(0);
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Reset
 * Signature: (B)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Reset(
		JNIEnv *, jobject, jbyte deviceIdx) {
	BYTE DeviceID = deviceIdx;
	while (hardsid_usb_flush(DeviceID) == HSID_USB_WSTATE_BUSY)
		Sleep(0);
	hardsid_usb_abortplay(DeviceID);
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Delay
 * Signature: (BS)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Delay(
		JNIEnv *, jobject, jbyte deviceIdx, jshort cycles) {
	BYTE DeviceID = deviceIdx;
	WORD Cycles = cycles;
	if (Cycles > 0) {
		while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Flush
 * Signature: (B)V
 */
__declspec(dllexport) JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Flush(
		JNIEnv *, jobject, jbyte deviceIdx) {
	BYTE DeviceID = deviceIdx;
	while (hardsid_usb_flush(DeviceID) == HSID_USB_WSTATE_BUSY)
		Sleep(0);
}
