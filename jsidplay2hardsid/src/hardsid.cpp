#include "hardsid.hpp"

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

__declspec(dllexport) BYTE HardSID_Devices() {
	return hardsid_usb_getdevcount();
}

__declspec(dllexport) BYTE HardSID_SIDCount(BYTE DeviceID) {
	return hardsid_usb_getsidcount(DeviceID);
}

__declspec(dllexport) void HardSID_Flush(BYTE deviceID) {
	while (hardsid_usb_flush(deviceID) == HSID_USB_WSTATE_BUSY)
		Sleep(0);
}

__declspec(dllexport) void HardSID_Reset(BYTE deviceID) {
	hardsid_usb_abortplay(deviceID);
}

__declspec(dllexport) void HardSID_Delay(BYTE deviceID, SHORT cycles) {
	while (cycles > 65535) {
		while (hardsid_usb_delay(deviceID, 65535) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
		cycles -= 65536;
	}
	if (cycles > 0) {
		while (hardsid_usb_delay(deviceID, cycles) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	}
}

__declspec(dllexport) void HardSID_Write(BYTE deviceID, BYTE sidNum, BYTE reg,
		BYTE data) {
	while (hardsid_usb_write(deviceID, (sidNum << 5) | reg, data)
			== HSID_USB_WSTATE_BUSY)
		Sleep(0);
}

