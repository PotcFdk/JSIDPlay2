package libsidplay.common;

public enum Engine {
	/** Software (emulation using RESID or RESIDfp) */
	EMULATION,
	/** Software (Network SID Device via socket connection) */
	NETSID,
	/** Hardware (HardSID4U USB device) */
	HARDSID,
	/** Hardware (SidBlaster USB device) */
	SIDBLASTER,
	/** Hardware (ExSID USB device) */
	EXSID
}
