#include "StdAfx.h"
#include "hardsid_builder_HardSID4U.h"

using namespace std;

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

// constants: HardSID commands
typedef enum {
	CMD_FLUSH,
	CMD_WAIT_FLUSH,
	CMD_MUTE,
	CMD_RESET,
	CMD_TRY_DELAY,
	CMD_TRY_WRITE,
	CMD_TRY_READ
} command_t;
typedef enum {
	RSP_OK, RSP_BUSY, RSP_ERR, RSP_READ
} response_t;

enum {
	JSIDPLAY2_DEVICES = 8,
	MAX_WRITE_CYCLES = 4096, /* c64 cycles */
	CMD_BUFFER_SIZE = 4096, /* bytes */
	WAIT_BETWEEN_ATTEMPTS = 2, /* ms */
};

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
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1DeviceCount(JNIEnv *,
		jobject) {
	BYTE erg = 	hardsid_usb_getdevcount();
	return (jint) erg;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_SIDCount
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1SIDCount(JNIEnv *,
		jobject, jint deviceId) {
	BYTE DeviceID = deviceId;
	BYTE Sids = hardsid_usb_getsidcount(DeviceID);
	return (jint) Sids;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Read
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Read(JNIEnv *,
		jobject, jint deviceId, jint sidNum, jint cycles, jint reg) {
	BYTE DeviceID = deviceId;
	//BYTE SidNum = sidNum;
	WORD Cycles = cycles;
	//BYTE SID_reg = reg;
	if (Cycles > 0) {
		while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
	return (jint) 0xff;	//unsupported reads!
}

// Add a SID write to the ring buffer, until it is full,
// then send it to JSIDPlay2 to be queued there and executed
BYTE HardSID_Try_Write(BYTE DeviceID, BYTE SidNum, WORD Cycles, BYTE SID_reg, BYTE Data) {
	if (Cycles > 0) {
		while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
	//issues a write
	//if the hardware buffer is full, sleeps the thread until there is some space for this write
	while (hardsid_usb_write(DeviceID, (SidNum << 5) | SID_reg, Data)
			== HSID_USB_WSTATE_BUSY)
		Sleep(0);
	return 0;
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Write
 * Signature: (IIIII)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Write(JNIEnv *,
		jobject, jint deviceIdx, jint sidNum, jint cycles, jint reg, jint dat) {
	BYTE DeviceID = deviceIdx;
	BYTE SidNum = sidNum;
	WORD Cycles = cycles;
	BYTE SID_reg = reg;
	BYTE Data = dat;
	while (HardSID_Try_Write(DeviceID, SidNum, Cycles, SID_reg, Data)
			== HSID_USB_WSTATE_BUSY) {
		// Try_Write sleeps for us
	}
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Reset
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Reset(JNIEnv *,
		jobject, jint deviceIdx) {
	BYTE DeviceID = deviceIdx;
	hardsid_usb_abortplay(DeviceID);
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Delay
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Delay(JNIEnv *,
		jobject, jint deviceIdx, jint cycles) {
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
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Flush(JNIEnv *,
		jobject, jint deviceIdx) {
	BYTE DeviceID = deviceIdx;
	while (hardsid_usb_flush(DeviceID) == HSID_USB_WSTATE_BUSY)
		Sleep(0);
}

// stuff required to link against hardsid_usb.lib (that was built using MSVC)

extern "C" {
extern char *__cdecl __wrap_ultoa(unsigned long _Val, char *_Dstbuf,
		int _Radix) {
	return _ultoa(_Val, _Dstbuf, _Radix);
}

extern void __cdecl __security_check_cookie() {
}
}
