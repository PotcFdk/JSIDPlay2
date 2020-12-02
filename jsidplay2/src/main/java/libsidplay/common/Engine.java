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
	/** Hardware (SidBlaster USB device) test mode */
	SIDBLASTER_TEST
}
