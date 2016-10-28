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

// socket connection to JSIDPlay2
SOCKET connectedSocket;
bool connected;

// Frequency stuff to calculate cycles between SID writes
LARGE_INTEGER lastTime;

// writes buffered at client
BYTE cmd_buffer[CMD_BUFFER_SIZE];
// index at cmd_buffer.
int cmd_index;
// cycles queued in command.
int cmd_buffer_cycles;

BOOL contact_jsidplay2() {
	WSADATA wsa;
	SOCKADDR_IN addr;

	if (WSAStartup(MAKEWORD(2, 0), &wsa) != 0) {
		return false;
	}

	connectedSocket = socket(AF_INET, SOCK_STREAM, 0);
	if (connectedSocket == INVALID_SOCKET) {
		return FALSE;
	}

	memset(&addr, 0, sizeof(SOCKADDR_IN));
	addr.sin_family = AF_INET;
	addr.sin_addr.s_addr = inet_addr("127.0.0.1");
	addr.sin_port = htons(6581);
	if (connect(connectedSocket, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
		return FALSE;
	}
	return TRUE;
}

void cleanup() {
	hardsid_usb_close();
	if (connectedSocket != 0) {
		closesocket(connectedSocket);
		connectedSocket = 0;
	}
}

BOOL APIENTRY DllMain(HINSTANCE hModule, DWORD ul_reason_for_call,
		LPVOID lpReserved) {
	switch (ul_reason_for_call) {
	case DLL_PROCESS_ATTACH:
		char moduleFilename[MAX_PATH];
		GetModuleFileName((HMODULE) hModule, moduleFilename,
				sizeof(moduleFilename));
		connectedSocket = 0;
		lastTime.QuadPart = -1;
		cmd_index = 0;
		cmd_buffer_cycles = 0;

		hardsid_usb_init(TRUE, 1);

		connected = contact_jsidplay2();
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

HSID_USB_WSTATE flush_cmd_buffer(BOOL give_up_if_busy,
		BYTE *readResult) {
	while (true) {
		char result[2];
		/* Fill in packet data length so that server knows
		 * when entire packet has been read. */
		int data_length = cmd_index - 4;
		cmd_buffer[2] = (data_length >> 8) & 0xff;
		cmd_buffer[3] = data_length & 0xff;

		int sendlen = send(connectedSocket, (const char *) &cmd_buffer[0],
				cmd_index, 0);
		/* FIXME: there are 3 error responses. How do handle errors correctly,
		 * can we return error to caller? */
		if (sendlen != cmd_index) {
			MessageBox(GetActiveWindow(), "Server error",
					"Unable to write SID command to server",
					MB_OK | MB_ICONERROR);
			cmd_index = 0;
			return HSID_USB_WSTATE_OK;
		}
		int recvlen = recv(connectedSocket, result, 2, 0);
		if (recvlen == -1) {
			MessageBox(GetActiveWindow(), "Server error",
					"Unable to receive SID command response from server",
					MB_OK | MB_ICONERROR);
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
		recvlen = recv(connectedSocket, (char *) &cmd_buffer[0],
				sizeof(cmd_buffer) - 1, 0);
		if (recvlen >= 0) {
			cmd_buffer[recvlen] = 0;
		} else {
			strcpy((char *) &cmd_buffer[0], "No error provided by server.");
		}
		MessageBox(GetActiveWindow(), "Server error", (LPCSTR) &cmd_buffer[0],
		MB_OK | MB_ICONERROR);

		cmd_index = 0;
		return HSID_USB_WSTATE_OK;
	}
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
	if (connected) {
		// Add our fakes after the real ones.
		return (jint) (Sids + JSIDPLAY2_DEVICES);
	} else {
		return (jint) Sids;
	}
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Read
 * Signature: (IIII)I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Read(JNIEnv *,
		jobject, jint deviceId, jint sidNum, jint cycles, jint reg) {
	BYTE DeviceID = deviceId;
	BYTE SidNum = sidNum;
	WORD Cycles = cycles;
	BYTE SID_reg = reg;
	if (SidNum < hardsid_usb_getsidcount(DeviceID)) {
		if (Cycles > 0) {
			while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
				Sleep(0);
		}
		return (jint) 0xff;	//unsupported reads!
	} else {
		SidNum -= hardsid_usb_getsidcount(DeviceID);
		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}
		cmd_buffer[cmd_index++] = CMD_TRY_READ;
		cmd_buffer[cmd_index++] = SidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = (Cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = Cycles & 0xff;
		cmd_buffer[cmd_index++] = SID_reg;

		BYTE result = 0xff;
		flush_cmd_buffer(false, &result);
		return (jint) result;
	}
}

HSID_USB_WSTATE maybe_send_writes_to_server() {
	/* flush writes after a bit of buffering */
	if (cmd_index == sizeof(cmd_buffer)
			|| cmd_buffer_cycles > MAX_WRITE_CYCLES) {
		if (flush_cmd_buffer(true, NULL) == HSID_USB_WSTATE_BUSY) {
			return HSID_USB_WSTATE_BUSY;
		}
		cmd_buffer_cycles = 0;
	}
	return HSID_USB_WSTATE_OK;
}

// Add a SID write to the ring buffer, until it is full,
// then send it to JSIDPlay2 to be queued there and executed
BYTE HardSID_Try_Write(BYTE DeviceID, BYTE SidNum, WORD Cycles, BYTE SID_reg, BYTE Data) {
	if (SidNum < hardsid_usb_getsidcount(DeviceID)) {
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
	} else {
		SidNum -= hardsid_usb_getsidcount(DeviceID);

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
			cmd_buffer[cmd_index++] = SidNum;
			cmd_index += 2;
		}

		/* add write to queue */
		cmd_buffer[cmd_index++] = (Cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = Cycles & 0xff;
		cmd_buffer[cmd_index++] = SID_reg;
		cmd_buffer[cmd_index++] = Data;
		cmd_buffer_cycles += Cycles;
		/* NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case. */
		maybe_send_writes_to_server();

		return HSID_USB_WSTATE_OK;
	}
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
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Reset(JNIEnv *,
		jobject, jint deviceIdx, jint sidNum) {
	BYTE DeviceID = deviceIdx;
	BYTE SidNum = sidNum;
	if (SidNum < hardsid_usb_getsidcount(DeviceID)) {
		hardsid_usb_abortplay(DeviceID);
	} else {
		SidNum -= hardsid_usb_getsidcount(DeviceID);
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_FLUSH;
		cmd_buffer[cmd_index++] = SidNum; /* SID number */
		cmd_index += 2;
		flush_cmd_buffer(false, NULL);

		cmd_buffer[cmd_index++] = CMD_RESET;
		cmd_buffer[cmd_index++] = SidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = 0;
		flush_cmd_buffer(false, NULL);
	}
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Delay
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Delay(JNIEnv *,
		jobject, jint deviceIdx, jint sidNum, jint cycles) {
	BYTE DeviceID = deviceIdx;
	BYTE SidNum = sidNum;
	WORD Cycles = cycles;
	if (SidNum < hardsid_usb_getsidcount(DeviceID)) {
		if (Cycles > 0) {
			while (hardsid_usb_delay(DeviceID, Cycles) == HSID_USB_WSTATE_BUSY)
				Sleep(0);
		}
	} else {
		SidNum -= hardsid_usb_getsidcount(DeviceID);

		/* deal with unsubmitted writes */
		if (cmd_index != 0) {
			flush_cmd_buffer(false, NULL);
			cmd_buffer_cycles = 0;
		}

		cmd_buffer[cmd_index++] = CMD_TRY_DELAY;
		cmd_buffer[cmd_index++] = SidNum; /* SID number */
		cmd_index += 2;
		cmd_buffer[cmd_index++] = (Cycles & 0xff00) >> 8;
		cmd_buffer[cmd_index++] = Cycles & 0xff;
		flush_cmd_buffer(false, NULL);
	}
}

/*
 * Class:     hardsid_builder_HardSID4U
 * Method:    HardSID_Flush
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HardSID4U_HardSID_1Flush(JNIEnv *,
		jobject, jint deviceIdx, jint sidNum) {
	BYTE DeviceID = deviceIdx;
	BYTE SidNum = sidNum;
	if (SidNum < hardsid_usb_getsidcount(DeviceID)) {
		while (hardsid_usb_flush(DeviceID) == HSID_USB_WSTATE_BUSY)
			Sleep(0);
	} else {
		SidNum -= hardsid_usb_getsidcount(DeviceID);
		/* do not submit unsubmitted writes, just trash buffer. */
		cmd_index = 0;
		cmd_buffer_cycles = 0;
		cmd_buffer[cmd_index++] = CMD_FLUSH;
		cmd_buffer[cmd_index++] = SidNum; /* SID number */
		cmd_index += 2;
		flush_cmd_buffer(false, NULL);
	}
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
