#include "stdafx.h"
#include "hardsid.h"

// constants: HardSID commands
typedef enum { CMD_FLUSH, CMD_WAIT_FLUSH, CMD_MUTE, CMD_RESET, CMD_TRY_DELAY, CMD_TRY_WRITE, CMD_TRY_READ } command_t;
typedef enum { RSP_OK, RSP_BUSY, RSP_ERR, RSP_READ } response_t;
typedef enum { HSID_USB_WSTATE_OK=1, HSID_USB_WSTATE_BUSY } wstate_t;

static enum {
	JSIDPLAY2_DEVICES=8,
	MAX_WRITE_CYCLES=4096, /* c64 cycles */
	CMD_BUFFER_SIZE=4096, /* bytes */
	WAIT_BETWEEN_ATTEMPTS=2, /* ms */
};

using namespace std;

// socket connection to JSIDPlay2
static SOCKET connectedSocket;
static bool connected = false;

// Frequency stuff to calculate cycles between SID writes
static LARGE_INTEGER lastTime;

// writes buffered at client
static BYTE cmd_buffer[CMD_BUFFER_SIZE];
// index at cmd_buffer.
static int cmd_index = 0;
// cycles queued in command.
static int cmd_buffer_cycles = 0;

// Original HardSID4U driver
static HINSTANCE hOrigDll;

// Number of real HardSID4U devices, where the fake HardSID is put on top.
static BYTE numRealDevices;

// Function Pointers to original HardSID DLL
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
typedef void (CALLBACK* LPFNDLL_SETDEBUG)(BOOL);
static LPFNDLL_SETDEBUG pfnSetDebug;
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

/* Look up procedures from the hardsid_orig.dll and store their addresses.
*/
static BOOL resolve_hardsid_symbols(char *moduleFilename)
{
	char *lastBSlash = strrchr(moduleFilename, '\\');
	char origDriver[MAX_PATH];
	memset(origDriver,0,MAX_PATH);
	strncpy( origDriver, moduleFilename, (lastBSlash-moduleFilename) );
	strcat( origDriver, "\\HardSID_orig.dll");

	// Load original driver and read function pointers
	// to call its functions.
	hOrigDll = LoadLibrary(origDriver);
	if (! hOrigDll) {
		/* no hardsid_orig.dll; we can manage. */
		MessageBox(GetActiveWindow(),
			origDriver,
			"Original Driver missing", MB_OK | MB_ICONERROR);
		return TRUE;
	}

	pfnHardSID_Version = (LPFNDLL_HARDSID_VERSION)GetProcAddress(hOrigDll, "HardSID_Version");
	if (!pfnHardSID_Version) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Version in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	if ((*pfnHardSID_Version)() < 0x301) {
		MessageBox(GetActiveWindow(),
			"HardSID_Version() must be at least 3.01.",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnGetHardSIDCount = (LPFNDLL_GETHARDSIDCOUNT)GetProcAddress(hOrigDll, "GetHardSIDCount");
	if (!pfnGetHardSIDCount) {
		MessageBox(GetActiveWindow(),
			"Function: GetHardSIDCount in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Devices = (LPFNDLL_HARDSIDDEVICES)GetProcAddress(hOrigDll, "HardSID_Devices");
	if (!pfnHardSID_Devices) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Devices in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnInitHardSID_Mapper = (LPFNDLL_INITHARDSID_MAPPER)GetProcAddress(hOrigDll, "InitHardSID_Mapper");
	if (!pfnInitHardSID_Mapper) {
		MessageBox(GetActiveWindow(),
			"Function: InitHardSID_Mapper in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnMuteHardSID_Line = (LPFNDLL_MUTEHARDSID_LINE)GetProcAddress(hOrigDll, "MuteHardSID_Line");
	if (!pfnMuteHardSID_Line) {
		MessageBox(GetActiveWindow(),
			"Function: MuteHardSID_Line in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnWriteToHardSID = (LPFNDLL_WRITETOHARDSID)GetProcAddress(hOrigDll, "WriteToHardSID");
	if (!pfnWriteToHardSID) {
		MessageBox(GetActiveWindow(),
			"Function: WriteToHardSID in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnReadFromHardSID = (LPFNDLL_READFROMHARDSID)GetProcAddress(hOrigDll, "ReadFromHardSID");
	if (!pfnReadFromHardSID) {
		MessageBox(GetActiveWindow(),
			"Function: ReadFromHardSID in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnSetDebug = (LPFNDLL_SETDEBUG)GetProcAddress(hOrigDll, "SetDebug");
	if (!pfnSetDebug) {
		MessageBox(GetActiveWindow(),
			"Function: SetDebug in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Filter = (LPFNDLL_HARDSID_FILTER)GetProcAddress(hOrigDll, "HardSID_Filter");
	if (!pfnHardSID_Filter) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Filter in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Flush = (LPFNDLL_HARDSID_FLUSH)GetProcAddress(hOrigDll, "HardSID_Flush");
	if (!pfnHardSID_Flush) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Flush in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Mute = (LPFNDLL_HARDSID_MUTE)GetProcAddress(hOrigDll, "HardSID_Mute");
	if (!pfnHardSID_Mute) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Mute in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Reset = (LPFNDLL_HARDSID_RESET)GetProcAddress(hOrigDll, "HardSID_Reset");
	if (!pfnHardSID_Reset) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Reset in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Read = (LPFNDLL_HARDSID_READ)GetProcAddress(hOrigDll, "HardSID_Read");
	if (!pfnHardSID_Read) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Read in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Sync = (LPFNDLL_HARDSID_SYNC)GetProcAddress(hOrigDll, "HardSID_Sync");
	if (!pfnHardSID_Sync) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Sync in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Write = (LPFNDLL_HARDSID_WRITE)GetProcAddress(hOrigDll, "HardSID_Write");
	if (!pfnHardSID_Write) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Write in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Reset2 = (LPFNDLL_HARDSID_RESET2)GetProcAddress(hOrigDll, "HardSID_Reset2");
	if (!pfnHardSID_Reset2) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Reset2 in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Lock = (LPFNDLL_HARDSID_LOCK)GetProcAddress(hOrigDll, "HardSID_Lock");
	if (!pfnHardSID_Lock) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Lock in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Unlock = (LPFNDLL_HARDSID_UNLOCK)GetProcAddress(hOrigDll, "HardSID_Unlock");
	if (!pfnHardSID_Unlock) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Unlock in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_SoftFlush = (LPFNDLL_HARDSID_SOFTFLUSH)GetProcAddress(hOrigDll, "HardSID_SoftFlush");
	if (!pfnHardSID_SoftFlush) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_SoftFlush in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_MuteAll = (LPFNDLL_HARDSID_MUTEALL)GetProcAddress(hOrigDll, "HardSID_MuteAll");
	if (!pfnHardSID_MuteAll) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_MuteAll in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Delay = (LPFNDLL_HARDSID_DELAY)GetProcAddress(hOrigDll, "HardSID_Delay");
	if (!pfnHardSID_Delay) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Delay in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}
	pfnHardSID_Try_Write = (LPFNDLL_HARDSID_TRY_WRITE)GetProcAddress(hOrigDll, "HardSID_Try_Write");
	if (!pfnHardSID_Try_Write) {
		MessageBox(GetActiveWindow(),
			"Function: HardSID_Try_Write in HardSID_orig.dll not found!",
			"fake HardSID error", MB_OK | MB_ICONERROR);
		return FALSE;
	}

	return TRUE;
}

static BOOL contact_jsidplay2()
{
	WSADATA wsa;
	SOCKADDR_IN addr;

	if (WSAStartup(MAKEWORD(2,0), &wsa) != 0) {
		return false;
	}

	connectedSocket = socket(AF_INET,SOCK_STREAM,0);
	if(connectedSocket == INVALID_SOCKET) {
		return FALSE;
	}

	memset(&addr, 0, sizeof(SOCKADDR_IN));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = inet_addr("127.0.0.1");
	addr.sin_port = htons(6581);
	if (connect(connectedSocket, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
		return FALSE;
	}

	connected = TRUE;
	return TRUE;
}

static void cleanup()
{
	if (connectedSocket != 0) {
		closesocket(connectedSocket);
		connectedSocket = 0;
	}
	if (hOrigDll != NULL) {
		FreeLibrary(hOrigDll);
		hOrigDll = NULL;
	}
}

BOOL APIENTRY DllMain( HINSTANCE hModule, 
					  DWORD  ul_reason_for_call, 
					  LPVOID lpReserved
					  )
{
	switch (ul_reason_for_call) {
		case DLL_PROCESS_ATTACH:
			char moduleFilename[MAX_PATH];
			GetModuleFileName((HMODULE)hModule, moduleFilename, sizeof(moduleFilename));
			connectedSocket = 0;
			hOrigDll = NULL;

			if (! resolve_hardsid_symbols(moduleFilename)) {
				cleanup();
				return FALSE;
			}

			if (! contact_jsidplay2()) {
				// This can happen, if
				// JSIDPlay2 is not running, then
				// use only the real HardSID4U devices
				return TRUE;
			}

			/* initialize device count for ourselves & others */
			HardSID_Devices();

			lastTime.QuadPart = -1;
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

static wstate_t flush_cmd_buffer(BOOL give_up_if_busy, BYTE *readResult)
{
	while (true) {
		char result[2];
		/* Fill in packet data length so that server knows
		* when entire packet has been read. */
		int data_length = cmd_index - 4;
		cmd_buffer[2] = (data_length >> 8) & 0xff;
		cmd_buffer[3] = data_length & 0xff;

		int sendlen = send(connectedSocket, (const char *) &cmd_buffer[0], cmd_index, 0);
		/* FIXME: there are 3 error responses. How do handle errors correctly,
		* can we return error to caller? */
		if (sendlen != cmd_index) {
			MessageBox(GetActiveWindow(),
				"Server error",
				"Unable to write SID command to server", MB_OK | MB_ICONERROR);
			cmd_index = 0;
			return HSID_USB_WSTATE_OK;
		}
		int recvlen = recv(connectedSocket, result, 2, 0);
		if (recvlen == -1) {
			MessageBox(GetActiveWindow(),
				"Server error",
				"Unable to receive SID command response from server", MB_OK | MB_ICONERROR);
			cmd_index = 0;
			return HSID_USB_WSTATE_OK;
		}

		int rc = result[0];

		/* server accepted. Reset variables. */
		if (rc == RSP_OK) {
			cmd_index = 0;
			return HSID_USB_WSTATE_OK;
		}

		if (rc == RSP_BUSY) {
			if (give_up_if_busy) {
				return HSID_USB_WSTATE_BUSY;
			}
			Sleep(WAIT_BETWEEN_ATTEMPTS);
			continue;
		}

		/* the only caller that uses TRY_READ passes "false" on give_up_if_busy,
		* so this is the only way this can end, barring errors. */
		if (rc == RSP_READ && recvlen == 2) {
			cmd_index = 0;
			*readResult = result[1];
			return HSID_USB_WSTATE_OK;
		}

		/* display server error, or as much of that as we can */
		recvlen = recv(connectedSocket, (char *) &cmd_buffer[0], sizeof(cmd_buffer)-1, 0);
		if (recvlen >= 0) {
			cmd_buffer[recvlen] = 0;
		} else {
			strcpy((char *) &cmd_buffer[0], "No error provided by server.");
		}
		MessageBox(GetActiveWindow(),
			"Server error",
			(LPCSTR) &cmd_buffer[0],
			MB_OK | MB_ICONERROR);

		cmd_index = 0;
		return HSID_USB_WSTATE_OK;
	}
}

// Get the number of HardSID devices
BYTE __stdcall HardSID_Devices()
{
	numRealDevices = 0;
	if (hOrigDll != NULL) {
		// Get the real HardSID devices
		numRealDevices = (*pfnHardSID_Devices)();
	}
	if (connected) {
		// Add our fakes after the real ones.
		return numRealDevices + JSIDPLAY2_DEVICES;
	} else {
		return numRealDevices;
	}
}

// Disable/enable hardsid internal filter
void __stdcall HardSID_Filter(BYTE deviceID, BOOL filter)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Filter)(deviceID, filter);
	}
}

// Flush clears the ring buffer and the fakeHardSID queue immediately
void __stdcall HardSID_Flush(BYTE deviceID)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Flush)(deviceID);
	} else {
		deviceID -= numRealDevices;
		/* do not submit unsubmitted writes, just trash buffer. */
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_FLUSH;
		cmd_buffer[cmd_index++] = deviceID;				/* deviceID */
		cmd_index += 2;
		flush_cmd_buffer(false, NULL);
	}
}

// Mute a single voice
void __stdcall HardSID_Mute(BYTE deviceID, BYTE voice, BOOL mute)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Mute)(deviceID, voice, mute);
	} else {
		deviceID -= numRealDevices;
		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}
		cmd_buffer[cmd_index++] = CMD_MUTE;
		cmd_buffer[cmd_index++] = deviceID;				/* deviceID */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = voice;
		cmd_buffer[cmd_index++] = char(mute);
		flush_cmd_buffer(false, NULL);
	}
}

// Delete any FIFOs and immediately reset SID.
void __stdcall HardSID_Reset(BYTE deviceID)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Reset)(deviceID);
	} else {
		HardSID_Reset2(deviceID, 0);
	}
}

// Read from HardSID
BYTE __stdcall HardSID_Read(BYTE deviceID, WORD cycles, BYTE SID_reg)
{
	if (deviceID < numRealDevices) {
		return (*pfnHardSID_Read)(deviceID, cycles, SID_reg);
	} else {
		deviceID -= numRealDevices;
		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}
		cmd_buffer[cmd_index++] = CMD_TRY_READ;
		cmd_buffer[cmd_index++] = deviceID;				/* deviceID */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = (cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = cycles & 0xff;
		cmd_buffer[cmd_index++] = SID_reg;

		BYTE result = 0xff;
		flush_cmd_buffer(false, &result);
		return result;
	}
}

// Play the entire contents of a buffer/fifo for particular SID.
// in truth, our WAIT_FLUSH forces all queues to be emptied due to
// all SID commands being interleaved.
void __stdcall HardSID_Sync(BYTE deviceID) {
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Sync)(deviceID);
	} else  {
		deviceID -= numRealDevices;
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}
		cmd_buffer[cmd_index++] = CMD_WAIT_FLUSH;
		cmd_buffer[cmd_index++] = deviceID;
		cmd_index += 2;
		flush_cmd_buffer(false, NULL);
	}
}

// Write value to SID
void __stdcall HardSID_Write(BYTE deviceID, WORD cycles, BYTE SID_reg, BYTE data) {
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Write)(deviceID, cycles, SID_reg, data);
	} else {
		while (HardSID_Try_Write(deviceID, cycles, SID_reg, data) == HSID_USB_WSTATE_BUSY) {
			// Try_Write sleeps for us
		}
	}
}

// HardSID driver version.
WORD __stdcall HardSID_Version()
{
	return HARDSID_VERSION;
}

// DLLs above version 0x0203
void __stdcall HardSID_Reset2(BYTE deviceID, BYTE volume)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Reset2)(deviceID, volume);
	} else {
		deviceID -= numRealDevices;
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_FLUSH;
		cmd_buffer[cmd_index++] = deviceID;				/* deviceID */
		cmd_index += 2;
		flush_cmd_buffer(false, NULL);

		cmd_buffer[cmd_index++] = CMD_RESET;
		cmd_buffer[cmd_index++] = deviceID;				/* deviceID */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = volume;
		flush_cmd_buffer(false, NULL);
	}
}

// Lock the current device to be used by the client
BOOL __stdcall HardSID_Lock(BYTE deviceID)
{
	if (deviceID < numRealDevices) {
		return (*pfnHardSID_Lock)(deviceID);
	}
	return TRUE;
}

// Unlock the current device to be no more used by the client
void __stdcall HardSID_Unlock(BYTE deviceID)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Unlock)(deviceID);
	}
}

// Mute all voices at once
void __stdcall HardSID_MuteAll(BYTE deviceID, BOOL mute)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_MuteAll)(deviceID, mute);
	} else {
		HardSID_Mute(deviceID, 0, mute);
		HardSID_Mute(deviceID, 1, mute);
		HardSID_Mute(deviceID, 2, mute);
	}
}

// Delay some cycles for the next SID write
void __stdcall HardSID_Delay(BYTE deviceID, WORD cycles)
{
	if (deviceID < numRealDevices) {
		(*pfnHardSID_Delay)(deviceID, cycles);
		return;
	} else {
		deviceID -= numRealDevices;

		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}

		cmd_buffer[cmd_index++] = CMD_TRY_DELAY;
		cmd_buffer[cmd_index++] = deviceID;
		cmd_index += 2;
		cmd_buffer[cmd_index++] = (cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = cycles & 0xff;
		flush_cmd_buffer(false, NULL);
	}
}

// HS4U unofficial extensions

/* sync server and client */
// (why does this method exist, why isn't Sync suitable?)
void __stdcall HardSID_SoftFlush(BYTE deviceID)
{
	HardSID_Sync(deviceID);
}

static wstate_t maybe_send_writes_to_server() {
	/* flush writes after a bit of buffering */
	if (cmd_index == sizeof(cmd_buffer) || cmd_buffer_cycles > MAX_WRITE_CYCLES) {
		if (flush_cmd_buffer(true, NULL) == HSID_USB_WSTATE_BUSY) {
			return HSID_USB_WSTATE_BUSY;
		}
		cmd_buffer_cycles = 0;
	}
	return HSID_USB_WSTATE_OK;
}

// Add a SID write to the ring buffer, until it is full,
// then send it to JSIDPlay2 to be queued there and executed
BYTE __stdcall HardSID_Try_Write(BYTE deviceID, WORD cycles, BYTE SID_reg, BYTE data)
{
	if (deviceID < numRealDevices) {
		return (*pfnHardSID_Try_Write)(deviceID, cycles, SID_reg, data);
	} else {
		deviceID -= numRealDevices;

		/* flush writes after a bit of buffering. If no flush,
		* then returns OK and we queue more. If flush attempt fails,
		* we must cancel. */
		if (maybe_send_writes_to_server() == HSID_USB_WSTATE_BUSY) {
			/* Sigh. Acid64 is daft. Why doesn't it sleep if it gets a BUSY code
			* but immediately retries? That eliminates the whole point I thought
			* Try_Write had. With a logic like this HardSID_Write works just as
			* good as this. Damnit. */
			Sleep(WAIT_BETWEEN_ATTEMPTS);
			return HSID_USB_WSTATE_BUSY;
		}

		if (cmd_index == 0) {
			/* start new write buffering sequence */
			cmd_buffer[cmd_index++] = CMD_TRY_WRITE;
			cmd_buffer[cmd_index++] = deviceID;
			cmd_index += 2;
		}

		/* add write to queue */
		cmd_buffer[cmd_index++] = (cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = cycles & 0xff;
		cmd_buffer[cmd_index++] = SID_reg;
		cmd_buffer[cmd_index++] = data;
		cmd_buffer_cycles += cycles;
		/* NB: if flush attempt fails, we have nevertheless queued command
		* locally and thus are allowed to return OK in any case. */
		maybe_send_writes_to_server();

		return HSID_USB_WSTATE_OK;
	}
}

// Old official hardsid interface methods.

/* depreciated */
BYTE __stdcall InitHardSID_Mapper()
{
	if (hOrigDll != NULL) {
		return (*pfnInitHardSID_Mapper)();
	}
	return 0;
}

/* depreciated. */
BYTE __stdcall GetHardSIDCount()
{
	numRealDevices = 0;
	if (hOrigDll != NULL) {
		// Get the real HardSID devices
		numRealDevices = (*pfnHardSID_Devices)();
	}
	if (connected) {
		// Add our fakes after the real ones.
		return numRealDevices + JSIDPLAY2_DEVICES;
	} else {
		return numRealDevices;
	}
}

/* depreciated */
// Mute every hardsid device
void __stdcall MuteHardSID_Line(BOOL mute)
{
	if (numRealDevices > 0) {
		(*pfnMuteHardSID_Line)(mute);
	}
	if (connected) {
		for (int i = 0; i < JSIDPLAY2_DEVICES; i ++) {
			HardSID_MuteAll(i, mute);
		}
	}
}

/* use cpu performance counters to estimate how much wallclock has
 * elapsed since the earlier write or read. (This does not really
 * work on modern CPUs, which have variable frequency, or several
 * cores at different frequencies, and can even completely stop
 * without activity.) */
static DWORD c64_cycles_since_last_call() {
	LARGE_INTEGER frequency, currentTime;
	QueryPerformanceFrequency(&frequency);
	QueryPerformanceCounter(&currentTime);

	if (lastTime.QuadPart == -1) {
		lastTime.QuadPart = currentTime.QuadPart;
	}

	double dftDuration = (double) (currentTime.QuadPart - lastTime.QuadPart) * (1e6 / (double) frequency.QuadPart);
	lastTime.QuadPart = currentTime.QuadPart;

	return (DWORD) dftDuration;
}

/* depreciated */
// Write the SID write to HardSID (no cycles data given)
void __stdcall WriteToHardSID(BYTE deviceID, BYTE SID_reg, BYTE data)
{
	if (deviceID < numRealDevices) {
		(*pfnWriteToHardSID)(deviceID, SID_reg, data);
	} else {
		if (connected) {
			DWORD cycles = c64_cycles_since_last_call();
			while (cycles > 0xFFFF) {
				HardSID_Delay(deviceID, 0xFFFF);
				cycles -= 0xFFFF;
			}

			while (HardSID_Try_Write(deviceID, (WORD)cycles, SID_reg, data) == HSID_USB_WSTATE_BUSY) {
				// Try_Write sleeps for us.
			}
		}
	}
}

/* depreciated */
// Read from HardSID (no cycles data given)
BYTE __stdcall ReadFromHardSID(BYTE deviceID, BYTE SID_reg)
{
	if (deviceID < numRealDevices) {
		return (*pfnReadFromHardSID)(deviceID, SID_reg);
	} else {
		DWORD cycles = c64_cycles_since_last_call();
		while (cycles > 0xFFFF) {
			HardSID_Delay(deviceID, 0xFFFF);
			cycles -= 0xFFFF;
		}

		return HardSID_Read(deviceID, cycles, SID_reg);
	}
}

/* depreciated */
void __stdcall SetDebug(BOOL debug)
{
	if (numRealDevices > 0) {
		(*pfnSetDebug)(debug);
	}
}