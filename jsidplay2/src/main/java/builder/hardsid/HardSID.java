package builder.hardsid;

import com.sun.jna.Library;

public interface HardSID extends Library {

	public static interface UsbWaitState {
		public static final int HSID_USB_WSTATE_OK = 1;
		public static final int HSID_USB_WSTATE_BUSY = 2;
		public static final int HSID_USB_WSTATE_ERROR = 3;
		public static final int HSID_USB_WSTATE_END = 4;
	}

	/**
	 * initializes the management library in sync or async mode and selects SIDPlay
	 * mode on the device (sysmode=1)
	 */
	boolean hardsid_usb_init(boolean syncmode, byte sysmode);

	/**
	 * returns the number of USB HardSID devices plugged into the computer
	 */
	byte hardsid_usb_getdevcount();

	/**
	 * returns the number of detected SID chips on the given device
	 */
	byte hardsid_usb_getsidcount(byte deviceID);

	/**
	 * schedules a write command (sidNum&lt;&lt;5 is part of Reg)
	 */
	int hardsid_usb_write(byte deviceID, byte reg, byte data);

	/**
	 * plays the remaining data from the buffer
	 */
	int hardsid_usb_flush(byte deviceID);

	/**
	 * schedules a delay command
	 */
	int hardsid_usb_delay(byte deviceID, short cycles);

	/**
	 * aborts the playback ASAP
	 */
	void hardsid_usb_abortplay(byte deviceID);
}
