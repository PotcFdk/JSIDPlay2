package builder.hardsid;

import com.sun.jna.Library;

public interface HardSID extends Library {

	/**
	 * initializes the management library in sync or async mode and selects SIDPlay
	 * mode on the device (sysmode=1)
	 */
	boolean hardsid_usb_init(boolean syncMode, SysMode sysMode);

	/**
	 * closes the managament library
	 */
	void hardsid_usb_close();

	/**
	 * returns the number of USB HardSID devices plugged into the computer
	 */
	byte hardsid_usb_getdevcount();

	/**
	 * returns the number of detected SID chips on the given device
	 */
	byte hardsid_usb_getsidcount(byte deviceID);

	/**
	 * read state
	 */
	WState hardsid_usb_readstate(byte deviceID);

	/**
	 * sync
	 */
	WState hardsid_usb_sync(byte deviceID);

	/**
	 * perform the communication in async or sync mode
	 */
	WState hardsid_usb_write_internal(byte deviceID);

	/**
	 * schedules a write command
	 */
	WState hardsid_usb_write_direct(byte deviceID, byte reg, byte data);

	/**
	 * schedules a write command (sidNum&lt;&lt;5 is part of Reg)
	 */
	WState hardsid_usb_write(byte deviceID, byte reg, byte data);

	/**
	 * plays the remaining data from the buffer
	 */
	WState hardsid_usb_flush(byte deviceID);

	/**
	 * selects one of the sysmodes on the device
	 */
	WState hardsid_usb_setmode(byte dev_id, SysMode newSysMode);

	/**
	 * schedules a delay command
	 */
	WState hardsid_usb_delay(byte deviceID, short cycles);

	/**
	 * aborts the playback ASAP
	 */
	void hardsid_usb_abortplay(byte deviceID);

	/**
	 * queries driver state variables (such as errorpacketcount)
	 */
	int hardsid_usb_querystatus(byte deviceID);

	/**
	 * returns the device type (HardSID 4U, HardSID UPlay, etc...)
	 */
	DeviceType hardsid_usb_getdevicetype(byte deviceID);
}
