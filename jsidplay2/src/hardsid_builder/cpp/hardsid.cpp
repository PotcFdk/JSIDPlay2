#include "stdafx.h"
#include "hardsid.h"
#include "hardsid_builder_HsidDLL2.h"

using namespace std;

// DLL main
BOOL APIENTRY DllMain( HINSTANCE hModule, 
                       DWORD  ul_reason_for_call, 
                       LPVOID lpReserved
					 )
{
	switch (ul_reason_for_call)
	{
		case DLL_PROCESS_ATTACH:
			break;
		case DLL_PROCESS_DETACH:
			break;
		case DLL_THREAD_ATTACH:
			break;
		case DLL_THREAD_DETACH:
			break;
    }
    return TRUE;
}

// JNI methods

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    LoadLibrary
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_hardsid_1builder_HsidDLL2_LoadLibrary
(JNIEnv *_env, jobject, jstring libName) {
	const char *libNameC = NULL;
	jboolean iscopy;
	if (libName!=NULL) {
		libNameC = _env->GetStringUTFChars(libName, &iscopy);
	} else {
		return (jboolean) FALSE;
	}

	HINSTANCE hJvmDll = LoadLibrary(libNameC);
	hardsiddll = hJvmDll;
	if (hJvmDll==NULL) {
		return (jboolean) FALSE;
	}
	InitHardSID_Mapper = (lpInitHardSID_Mapper) GetProcAddress(hardsiddll, "InitHardSID_Mapper");
	HardSID_Version = (HsidDLL2_Version_t) GetProcAddress(hardsiddll, "HardSID_Version");
	HardSID_Delay = (lpHardSID_Delay) GetProcAddress(hardsiddll, "HardSID_Delay");
	HardSID_Devices = (HsidDLL2_Devices_t) GetProcAddress(hardsiddll, "HardSID_Devices");
	HardSID_Filter = (HsidDLL2_Filter_t) GetProcAddress(hardsiddll, "HardSID_Filter");
	HardSID_Flush = (HsidDLL2_Flush_t) GetProcAddress(hardsiddll, "HardSID_Flush");
	HardSID_MuteAll = (HsidDLL2_MuteAll_t) GetProcAddress(hardsiddll, "HardSID_MuteAll");
	HardSID_Read = (HsidDLL2_Read_t) GetProcAddress(hardsiddll, "HardSID_Read");
	HardSID_Sync = (HsidDLL2_Sync_t) GetProcAddress(hardsiddll, "HardSID_Sync");
	HardSID_Write = (HsidDLL2_Write_t) GetProcAddress(hardsiddll, "HardSID_Write");
	HardSID_Reset = (HsidDLL2_Reset_t) GetProcAddress(hardsiddll, "HardSID_Reset");
	HardSID_Lock = (HsidDLL2_Lock_t) GetProcAddress(hardsiddll, "HardSID_Lock");
	HardSID_Unlock = (HsidDLL2_Unlock_t) GetProcAddress(hardsiddll, "HardSID_Unlock");
	HardSID_Reset2 = (HsidDLL2_Reset2_t) GetProcAddress(hardsiddll, "HardSID_Reset2");
	HardSID_Mute = (HsidDLL2_Mute_t) GetProcAddress(hardsiddll, "HardSID_Mute");
	HardSID_Mute2 = (HsidDLL2_Mute2_t) GetProcAddress(hardsiddll, "HardSID_Mute2");
	HardSID_SoftFlush = (lpHardSID_SoftFlush) GetProcAddress(hardsiddll, "HardSID_SoftFlush");
	MuteHardSID_Line = (lpMuteHardSID_Line) GetProcAddress(hardsiddll, "MuteHardSID_Line");
	ReadFromHardSID = (lpReadFromHardSID) GetProcAddress(hardsiddll, "ReadFromHardSID");
	WriteToHardSID = (lpWriteToHardSID) GetProcAddress(hardsiddll, "WriteToHardSID");
	SetDebug = (HsidDLL2_SetDebug_t) GetProcAddress(hardsiddll, "SetDebug");
	GetHardSIDCount = (HsidDLL2_GetHardSIDCount_t) GetProcAddress(hardsiddll, "GetHardSIDCount");
	return (jboolean) TRUE;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    InitHardSID_Mapper
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_InitHardSID_1Mapper
(JNIEnv *, jobject) {
	InitHardSID_Mapper();
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Version
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Version
  (JNIEnv *, jobject) {
	  WORD erg = HardSID_Version();
	  return (jint)erg;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Delay
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Delay
  (JNIEnv *, jobject, jint dev, jint cyc) {
	  BYTE DeviceID = dev;
	  WORD Cycles = cyc;
	  HardSID_Delay(DeviceID, Cycles);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Devices
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Devices
  (JNIEnv *, jobject) {
	BYTE erg = HardSID_Devices();
	  return (jint)erg;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Filter
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Filter
  (JNIEnv *, jobject, jint dev, jboolean fil) {
	  BYTE DeviceID = dev;
	  BOOL Filter = fil;
	  HardSID_Filter(DeviceID, Filter);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Flush
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Flush
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  HardSID_Flush(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_MuteAll
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1MuteAll
  (JNIEnv *, jobject, jint dev, jboolean mut) {
	  BYTE DeviceID = dev;
	  BOOL Mute = mut;
	  HardSID_MuteAll(DeviceID, Mute);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Read
 * Signature: (III)I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Read
  (JNIEnv *, jobject, jint dev, jint cyc, jint reg) {
	  BYTE DeviceID = dev;
	  WORD Cycles = cyc;
	  BYTE SID_reg = reg;
	  BYTE erg = HardSID_Read(DeviceID, Cycles, SID_reg);
	  return (jint)erg;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Sync
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Sync
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  HardSID_Sync(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Write
 * Signature: (IIII)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Write
  (JNIEnv *, jobject, jint dev, jint cyc, jint reg, jint dat) {
	BYTE DeviceID = dev;
	WORD Cycles = cyc;
	BYTE SID_reg = reg;
	BYTE data = dat;
	HardSID_Write(DeviceID, Cycles, SID_reg, data);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Reset
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Reset
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  HardSID_Reset(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Lock
 * Signature: (I)Z
 */
JNIEXPORT jboolean JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Lock
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  BOOL erg = HardSID_Lock(DeviceID);
	  return (jboolean)erg;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Unlock
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Unlock
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  HardSID_Unlock(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Reset2
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Reset2
  (JNIEnv *, jobject, jint dev, jint vol) {
	  BYTE DeviceID = dev;
	  BYTE Volume = vol;
	  HardSID_Reset2(DeviceID, Volume);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Mute
 * Signature: (IIZ)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Mute
  (JNIEnv *, jobject, jint dev, jint chn, jboolean mut) {
	BYTE DeviceID = dev;
	BYTE Channel = chn;
	BYTE Mute = mut;
	HardSID_Mute(DeviceID, Channel, Mute);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_Mute2
 * Signature: (IIZZ)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1Mute2
  (JNIEnv *, jobject, jint dev, jint chn, jboolean mut) {
	BYTE DeviceID = dev;
	BYTE Channel = chn;
	BYTE Mute = mut;
	HardSID_Mute(DeviceID, Channel, Mute);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    HardSID_SoftFlush
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_HardSID_1SoftFlush
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  HardSID_SoftFlush(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    MuteHardSID_Line
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_MuteHardSID_1Line
  (JNIEnv *, jobject, jint dev) {
	  BYTE DeviceID = dev;
	  MuteHardSID_Line(DeviceID);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    ReadFromHardSID
 * Signature: (II)I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HsidDLL2_ReadFromHardSID
  (JNIEnv *, jobject, jint dev, jint reg) {
	  BYTE DeviceID = dev;
	  BYTE SID_reg = reg;
	  BYTE erg = ReadFromHardSID(DeviceID, SID_reg);
	  return (jint)erg;
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    WriteToHardSID
 * Signature: (III)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_WriteToHardSID
  (JNIEnv *, jobject, jint dev, jint reg, jint dat) {
	BYTE DeviceID = dev;
	BYTE SID_reg = reg;
	BYTE data = dat;
	WriteToHardSID(DeviceID, SID_reg, data);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    SetDebug
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_hardsid_1builder_HsidDLL2_SetDebug
  (JNIEnv *, jobject, jboolean deb) {
	BOOL Debug = deb;
	SetDebug(Debug);
}

/*
 * Class:     hardsid_builder_HsidDLL2
 * Method:    GetHardSIDCount
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_hardsid_1builder_HsidDLL2_GetHardSIDCount
  (JNIEnv *, jobject) {
	BYTE erg = GetHardSIDCount();
	return (jint)erg;
}
