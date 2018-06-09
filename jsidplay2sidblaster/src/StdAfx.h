// stdafx.h : include file for standard system include files,
//  or project specific include files that are used frequently, but
//      are changed infrequently
//

#if !defined(AFX_STDAFX_H__D42E664F_E7D2_4FA6_B468_0A3442858649__INCLUDED_)
#define AFX_STDAFX_H__D42E664F_E7D2_4FA6_B468_0A3442858649__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000


// Insert your headers here
#define WIN32_LEAN_AND_MEAN		// Exclude rarely-used stuff from Windows headers
#include <direct.h>
#include <sstream>
#include <comdef.h>

#include <windows.h>
#include <winsock2.h>

// stuff required to link against hardsid_usb.lib (that was built using MSVC)

extern "C" {
extern char *__cdecl __wrap_ultoa(unsigned long _Val, char *_Dstbuf,
		int _Radix) {
	return _ultoa(_Val, _Dstbuf, _Radix);
}

extern void __cdecl __security_check_cookie() {
}
}

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_STDAFX_H__D42E664F_E7D2_4FA6_B468_0A3442858649__INCLUDED_)
