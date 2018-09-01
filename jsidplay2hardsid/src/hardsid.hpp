#include "StdAfx.h"

#ifndef _Included_HardSID
#define _Included_HardSID
#ifdef __cplusplus
extern "C" {
#endif

BYTE HardSID_Devices();

BYTE HardSID_SIDCount(BYTE DeviceID);

void HardSID_Flush(BYTE deviceID);

void HardSID_Reset(BYTE deviceID);

void HardSID_Delay(BYTE deviceID, SHORT cycles);

void HardSID_Write(BYTE deviceID, BYTE sidNum, BYTE reg, BYTE data);

#ifdef __cplusplus
}
#endif
#endif
