#ifndef _HARDSID_H_
#define _HARDSID_H_ 1

/* Lacking an official SDK, the secrets of HardSID API
 * are collected here as my feeble attempt to figure it out.
 * -alankila */

/* This header describes methods available as of version 3.01 */
#define HARDSID_VERSION 0x301

/* Official, endorsed methods. These calls are buffered,
 * and any call can block until the device can accept new commands. */
BYTE __stdcall HardSID_Devices(); // return number of chips
BYTE __stdcall HardSID_Read(BYTE deviceID, WORD cycles, BYTE SID_reg); // return value at SID_reg after the provided cycles elapsed
void __stdcall HardSID_Sync(BYTE deviceID); // wait until buffers have been consumed by all implementations
void __stdcall HardSID_Write(BYTE deviceID, WORD cycles, BYTE SID_reg, BYTE data); // write data to SID_reg after the provided cycles elapsed
WORD __stdcall HardSID_Version(); // return API version
void __stdcall HardSID_Reset(BYTE deviceID); // flush buffers & reset chip
void __stdcall HardSID_Reset2(BYTE deviceID, BYTE volume); // flush buffers & reset chip & leave volume at provided value
BOOL __stdcall HardSID_Lock(BYTE deviceID); // lock a device to our use, return locking success
void __stdcall HardSID_Unlock(BYTE deviceID); // free device from our use
void __stdcall HardSID_MuteAll(BYTE deviceID, BOOL mute); // mute all devices
void __stdcall HardSID_Delay(BYTE deviceID, WORD cycles); // send delay, but no write. (Music player does nothing.)

/* HS4U specials */
void __stdcall HardSID_SoftFlush(BYTE deviceID);
BYTE __stdcall HardSID_Try_Write(BYTE deviceID, WORD cycles, BYTE SID_reg, BYTE data);
// unknown __stdcall HardSID_Group(unknown) // Found on 3.01 HS4U DLL. What is this?

/* Depreciated old realtime methods.
 * These take a lot of CPU and have serious compatibility problems.
 * Usage is strongly discouraged. */
BYTE __stdcall InitHardSID_Mapper(); // unknown purpose
BYTE __stdcall GetHardSIDCount(); // same as HardSID_Devices()
void __stdcall MuteHardSID_Line(BOOL mute); // mute/unmute every chip
void __stdcall WriteToHardSID(BYTE deviceID, BYTE SID_reg, BYTE data); // realtime write
BYTE __stdcall ReadFromHardSID(BYTE deviceID, BYTE SID_reg); // realtime read
void __stdcall SetDebug(BOOL debug); // enable/disable debug.

#endif