package builder.jhardsid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbEndpoint;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbInterfacePolicy;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;

/**
 * Implements hardsid.dll api calls Written by Sandor Téli java port by Ken
 * Händel
 * 
 * XXX
 * 
 * <pre>
 * To tell the USB Host Manager of javax-usb to use the usb4java implementation you have to put the following property into the javax.usb.properties file which must be located in the root of your classpath
 * -Djavax.usb.services=org.usb4java.javax.Services
 * The default USB communication timeout of usb4java is 2500 milliseconds. To change this to 250 milliseconds for example add this to the properties file
 * -Dorg.usb4java.javax.timeout=250
 * The default USB device scan interval of usb4java is 500 milliseconds. To change this to 1000 milliseconds for example add this to the properties file
 * -Dorg.usb4java.javax.scanInterval=1000
 * If you want to use USBDK on Windows then you have to enable the feature with the following entry
 * -Dorg.usb4java.javax.useUSBDK=true
 * This setting is ignored on other platforms
 * </pre>
 * 
 * @author ken
 */
public class HardSIDUSB {

	private static final int USB_PACKAGE_SIZE = 512;
	private static final int WRITEBUFF_SIZE = USB_PACKAGE_SIZE; // write buffer for async mode - only one USB
																// package
	private static final int WRITEBUFF_SIZE_SYNC = 0x800;
	private static final int MAX_DEVCOUNT = 4;
	private static final int READBUFF_SIZE = 64;
	private static final int HW_BUFFBEG = 0x2000;
	private static final int HW_BUFFSIZE = 0x2000;
	private static final int HW_FILLRATIO = 0x1000;

	private static final short VENDOR_ID = 0x6581;
	private static final short PRODUCT_ID = (short) 0x8580;

	private UsbInterface iface;
	private UsbPipe[] inPipeBulk = new UsbPipe[MAX_DEVCOUNT], outPipeBulk = new UsbPipe[MAX_DEVCOUNT];

	byte lastaccsids[] = new byte[MAX_DEVCOUNT];

	private DevType[] devtypes = new DevType[MAX_DEVCOUNT];
	private ByteBuffer[] writebuff = new ByteBuffer[MAX_DEVCOUNT];

	private UsbDevice[] devhandles = new UsbDevice[MAX_DEVCOUNT];

	private int devcount = 0;

	private boolean initialized = false;
	private boolean error = false;

	boolean sync = false;

	boolean buffchk = true;
	int buffsize = WRITEBUFF_SIZE;

	private int pkgcount = 0;
	private short sysmode = 0;
	private int playcursor = HW_BUFFBEG;
	private int circbuffcursor = HW_BUFFBEG;

	private long lastrelayswitch = 0;

	public HardSIDUSB() {
		for (int i = 0; i < devtypes.length; i++) {
			devtypes[i] = DevType.UNKNOWN;
		}
	}

	/**
	 * initializes the managament library
	 * 
	 * @param syncmode synchronous or asynchronous mode
	 * @param sysmode  SIDPLAY or VST
	 * @return init was ok or failed
	 */
	public boolean hardsid_usb_init(boolean syncmode, SysMode sysmode) {
		try {
			if (sysmode != SysMode.SIDPLAY) {
				throw new RuntimeException("Only SIDPLAY mode currently supported!");
			}
			if (!syncmode) {
				throw new RuntimeException("Only synchronous mode currently supported!");
			}
			boolean fnd = false;
			sync = syncmode;

			if (sync)
				buffsize = WRITEBUFF_SIZE_SYNC;
			else
				buffsize = WRITEBUFF_SIZE;

			if (initialized) {
				hardsid_usb_close();
			}
			initialized = true;
			error = false;
			fnd = findalldevices(UsbHostManager.getUsbServices().getRootUsbHub(), VENDOR_ID, PRODUCT_ID);
			if (fnd) {
				sync = true;
				hardsid_usb_setmode(0, sysmode); // incomplete device number handling...
				sync = syncmode;
			}

			return fnd;
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException
				| SecurityException | UsbException e) {
			e.printStackTrace();
			error = true;
			return false;
		}
	}

	/**
	 * closes the management library
	 */
	public void hardsid_usb_close() {
		try {
			int d = 0;
			if (initialized) {
//			if (!sync)
//				IsoStream(devhandles[0], true);

				for (d = 0; d < devcount; d++) {

					if (devtypes[d] == DevType.HSUP || devtypes[d] == DevType.HSUNO) {
						// wait 5ms
						while (hardsid_usb_delay(d, 5000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=1)
						while (hardsid_usb_write_direct(d, (byte) 0xf0, (byte) 6) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 60ms
						while (hardsid_usb_delay(d, 60000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// switch 5V off (POWER_DIS=1;RESET_DIS=1;MUTE_ENA=1)
						while (hardsid_usb_write_direct(d, (byte) 0xf0, (byte) 7) == WState.BUSY) {
							Thread.sleep(0);
						}
						while (hardsid_usb_flush(d) == WState.BUSY) {
							Thread.sleep(0);
						}
						lastaccsids[d] = (byte) 0xff;
					}

					outPipeBulk[d].abortAllSubmissions();
					outPipeBulk[d].close();
					inPipeBulk[d].abortAllSubmissions();
					inPipeBulk[d].close();
					iface.release();
				}

				initialized = false;
			}
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException
				| InterruptedException | UsbException e) {
			e.printStackTrace();
			error = true;
		}
	}

	/**
	 * returns the number of USB HardSID devices plugged into the computer
	 * 
	 * @return number of USB HardSID devices
	 */
	public int hardsid_usb_getdevcount() {
		if (!initialized || error) {
			return 0;
		}
		return devcount;
	}

	/**
	 * returns the number of detected SID chips on the given device
	 * 
	 * @return number of detected SID chips on the given device
	 */
	public int hardsid_usb_getsidcount(int deviceId) {
		if (!initialized || error) {
			return 0;
		}
		switch (devtypes[deviceId]) {
		case HS4U:
			return 4;
		case HSUP:
			return 2;
		case HSUNO:
			return 1;
		default:
			return 0;
		}
	}

	@SuppressWarnings("unchecked")
	private boolean findalldevices(UsbHub hub, short vendorId, short productId) {
		try {
			devcount = 0;

			// When you set this interval to 0 then usb4java only scans once during
			// application startup. If you want to trigger a manual device scan you can do
			// it by calling the scan method on the USB services class (Must be casted to
			// the usb4java implementation, because this is not a javax-usb feature
			((org.usb4java.javax.Services) UsbHostManager.getUsbServices()).scan();

			for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
				UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
				if (device.isUsbHub()) {
					return findalldevices((UsbHub) device, vendorId, productId);
				} else if (desc.idVendor() == vendorId && desc.idProduct() == productId) {
					UsbConfiguration configuration = device.getActiveUsbConfiguration();
					iface = configuration.getUsbInterface((byte) 0);
					iface.claim(new UsbInterfacePolicy() {
						@Override
						public boolean forceClaim(UsbInterface usbInterface) {
							return true;
						}
					});

					devhandles[devcount] = device;
					devtypes[devcount] = DevType.HS4U; // XXX hard-wired for now
					lastaccsids[devcount] = (byte) 0xff;
					UsbEndpoint endpoint = iface.getUsbEndpoint((byte) 0x02);
					outPipeBulk[devcount] = endpoint.getUsbPipe();
					outPipeBulk[devcount].open();
					UsbEndpoint inEndpoint = iface.getUsbEndpoint((byte) 0x81);
					inPipeBulk[devcount] = inEndpoint.getUsbPipe();
					inPipeBulk[devcount].open();
					writebuff[devcount] = ByteBuffer.allocate(buffsize).order(ByteOrder.LITTLE_ENDIAN);
					devcount++;
					return true;
				}
			}
			return false;
		} catch (UsbNotActiveException | UsbDisconnectedException | UsbNotClaimedException | UsbException e) {
			e.printStackTrace();
			error = true;
			return false;
		}
	}

	/**
	 * Read state of USB device.
	 * 
	 * @param deviceId device ID
	 * @return state of USB device
	 */
	private WState hardsid_usb_readstate(int deviceId) {
		try {
			if (!initialized || error || !sync) {
				return WState.ERROR;
			}

			byte[] readbuff = new byte[READBUFF_SIZE];
			int nBytesRead = inPipeBulk[deviceId].syncSubmit(readbuff);

			if (nBytesRead == 0) {
				error = true;
				return WState.ERROR;
			} else {
				ByteBuffer b = ByteBuffer.wrap(readbuff).order(ByteOrder.LITTLE_ENDIAN);
				ShortBuffer s = b.asShortBuffer();
//				int comm_reset_cnt = readbuff[0] & 0xffff;
//				int error_shadow = readbuff[1] & 0xffff;
//				int error_addr_shadow = readbuff[2] & 0xffff;
				pkgcount = s.get(12) & 0xffff;
				playcursor = s.get(13) & 0xffff;
				circbuffcursor = s.get(14) & 0xffff;
				sysmode = s.get(15);
				return WState.OK;
			}
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException
				| UsbException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

	/**
	 * Sync with USB device.
	 * 
	 * @param deviceId device ID
	 * @return state of USB device
	 */
	private WState hardsid_usb_sync(int deviceId) {
		if (!initialized || error || !sync) {
			return WState.ERROR;
		}

		if (hardsid_usb_readstate(deviceId) != WState.OK) {
			error = true;
			return WState.ERROR;
		} else {
			int freespace;

			if (playcursor < circbuffcursor)
				freespace = playcursor + HW_BUFFSIZE - circbuffcursor;
			else if (playcursor > circbuffcursor)
				freespace = playcursor - circbuffcursor;
			else
				freespace = HW_BUFFSIZE;

			if (freespace < HW_FILLRATIO)
				return WState.BUSY;

			return WState.OK;
		}
	}

	/**
	 * perform the communication in async or sync mode
	 * 
	 * @param deviceId device ID
	 * @return state of USB device
	 */
	private WState hardsid_usb_write_internal(int deviceId) {
		try {
			if (!initialized || error || writebuff[deviceId].position() == 0) {
				return WState.ERROR;
			}

			int pkgstowrite = (((writebuff[deviceId].position() - 1) / USB_PACKAGE_SIZE) + 1);
			int writesize = pkgstowrite * USB_PACKAGE_SIZE;
			writebuff[deviceId].clear();

			if (!sync) {
				// stream based async Isoch stream feed
//			DeviceIoControl(devhandles[0],
//							IOCTL_ISOUSB_FEED_ISO_STREAM,
//							writebuffs[0],
//							writesize,
//							&dummy, //&gpStreamObj, //pointer to stream object initted when stream was started
//							sizeof( PVOID),
//							&nBytesWrite,
//							NULL);//&(ovls[oix]));
			} else {
				// sync mode direct file write

				byte[] toWrite = new byte[writesize];
				System.arraycopy(writebuff[deviceId].array(), 0, toWrite, 0, writesize);

				int sent = outPipeBulk[deviceId].syncSubmit(toWrite);
				if (sent != writesize) {
					throw new RuntimeException("Sent error!");
				}
			}
			return WState.OK;
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException
				| UsbException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

	/**
	 * schedules a write command
	 * 
	 * @param deviceId device ID
	 * @param reg      register (chip select as high byte)
	 * @param data     register value
	 * @return state of USB device
	 */
	private WState hardsid_usb_write_direct(int deviceId, byte reg, byte data) {
		if (!initialized || error) {
			return WState.ERROR;
		}

		if (sync && (writebuff[deviceId].position() == (buffsize - 2))) {
			WState ws = hardsid_usb_sync(deviceId);
			if (ws != WState.OK)
				return ws;
		}

		writebuff[deviceId].putShort((short) (((reg & 0xff) << 8) | (data & 0xff)));
		if (writebuff[deviceId].position() == buffsize) {
			return hardsid_usb_write_internal(deviceId);
		}

		return WState.OK;
	}

	/**
	 * Write to USB device
	 * 
	 * @param deviceId device ID
	 * @param reg      register (chip select as high byte)
	 * @param data     register value
	 * @return state of USB device
	 */
	public WState hardsid_usb_write(int deviceId, byte reg, byte data) {
		try {
			byte newsidmask;

			switch (devtypes[deviceId]) {
			case HS4U:
				return hardsid_usb_write_direct(deviceId, reg, data);
			case HSUP:
				if ((reg & 0xc0) != 0) {
					// invalid SID number
					return WState.ERROR;
				} else {
					if (lastaccsids[deviceId] != (reg & 0x20)) {
						// writing to a new SID
						lastaccsids[deviceId] = (byte) (reg & 0x20);
						if ((reg & 0x20) != 0) {
							newsidmask = (byte) 0xc0;
						} else {
							newsidmask = (byte) 0xa0;
						}

						while (lastrelayswitch > 0 && (System.currentTimeMillis() - lastrelayswitch) < 250) {
							Thread.sleep(0);
						}
						// timediff = GetTickCount() - lastrelayswitch;
						lastrelayswitch = System.currentTimeMillis();

						// runtime = GetTickCount();

						// wait 4usecs (not a real delay, but an init. delay command)
						while (hardsid_usb_delay(deviceId, 4) == WState.BUSY) {
							Thread.sleep(0);
						}
						// mute on (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=1)
						while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 6) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 60ms
						while (hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// switch 5V off (POWER_DIS=1;RESET_DIS=1;MUTE_ENA=1)
						while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 7) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 30ms
						while (hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// relay switch
						while (hardsid_usb_write_direct(deviceId, newsidmask, (byte) 0) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 30ms
						while (hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// turn off the relay
						while (hardsid_usb_write_direct(deviceId, (byte) 0x80, (byte) 0) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 30ms
						while (hardsid_usb_delay(deviceId, 30000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=0;MUTE_ENA=1)
						while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 4) == WState.BUSY) {
							Thread.sleep(0);
						}
						// wait 60ms
						while (hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
							Thread.sleep(0);
						}
						// end reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=0)
						while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 2) == WState.BUSY) {
							Thread.sleep(0);
						}
						// security 10usec wait
						while (hardsid_usb_delay(deviceId, 10) == WState.BUSY) {
							Thread.sleep(0);
						}

						// send this all down to the hardware
						while (hardsid_usb_flush(deviceId) == WState.BUSY) {
							Thread.sleep(0);
						}

						/*
						 * timediff = GetTickCount() - runtime; if (timediff>=240) { Thread.sleep(0);
						 * //for breakpoint purposes }
						 */

						// writing to the SID
						return hardsid_usb_write_direct(deviceId, (byte) ((reg & 0x1f) | 0x80), data);
					} else
						// writing to the same SID as last time..
						return hardsid_usb_write_direct(deviceId, (byte) ((reg & 0x1f) | 0x80), data);
				}
			case HSUNO:
				if (lastaccsids[deviceId] == 0xff) {

					// first write, we need the 5V

					// indicate that we've enabled the 5V
					lastaccsids[deviceId] = 0x01;

					// wait 4usecs (not a real delay, but an init. delay command)
					while (hardsid_usb_delay(deviceId, 4) == WState.BUSY) {
						Thread.sleep(0);
					}
					// wait 5ms
					while (hardsid_usb_delay(deviceId, 5000) == WState.BUSY) {
						Thread.sleep(0);
					}
					// switch 5V on, start reset (POWER_DIS=0;RESET_DIS=0;MUTE_ENA=1)
					while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 4) == WState.BUSY) {
						Thread.sleep(0);
					}
					// wait 60ms
					while (hardsid_usb_delay(deviceId, 60000) == WState.BUSY) {
						Thread.sleep(0);
					}
					// end reset (POWER_DIS=0;RESET_DIS=1;MUTE_ENA=0)
					while (hardsid_usb_write_direct(deviceId, (byte) 0xf0, (byte) 2) == WState.BUSY) {
						Thread.sleep(0);
					}
					// security 10usec wait
					while (hardsid_usb_delay(deviceId, 10) == WState.BUSY) {
						Thread.sleep(0);
					}

					// send this all down to the hardware
					while (hardsid_usb_flush(deviceId) == WState.BUSY) {
						Thread.sleep(0);
					}

					// writing to the SID
					return hardsid_usb_write_direct(deviceId, (byte) ((reg & 0x1f) | 0x80), data);
				} else
					// writing to the SID normally..
					return hardsid_usb_write_direct(deviceId, (byte) ((reg & 0x1f) | 0x80), data);
			default:
				return WState.ERROR;
			}
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException
				| InterruptedException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

	/**
	 * schedules a delay command
	 * 
	 * @param deviceId device ID
	 * @param cycles   cycles to delay
	 * @return state of USB device
	 */
	public WState hardsid_usb_delay(int deviceId, int cycles) {
		try {
			if (!initialized || error) {
				return WState.ERROR;
			}

			if (cycles == 0) {
				// no command for zero delay
			} else if (cycles < 0x100) {
				// short delay
				return hardsid_usb_write_direct(deviceId, (byte) 0xee, (byte) (cycles & 0xff)); // short delay command
			} else if ((cycles & 0xff) == 0) {
				// long delay without low order byte
				return hardsid_usb_write_direct(deviceId, (byte) 0xef, (byte) (cycles >> 8)); // long delay command
			} else {
				// long delay with low order byte
				if (sync && (writebuff[deviceId].position() == (buffsize - 2))) {
					WState ws;
					ws = hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
					if (ws != WState.OK)
						return ws;
				} else if (sync && (writebuff[deviceId].position() == (buffsize - 4))) {
					WState ws;
					ws = hardsid_usb_sync(deviceId);
					if (ws != WState.OK)
						return ws;
				}

				hardsid_usb_write_direct(deviceId, (byte) 0xef, (byte) (cycles >> 8)); // long delay command
				hardsid_usb_write_direct(deviceId, (byte) 0xee, (byte) (cycles & 0xff)); // short delay command
			}

			return WState.OK;
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

	/**
	 * sends a partial package to the hardware
	 * 
	 * @param deviceId device ID
	 * @return state of USB device
	 */
	public WState hardsid_usb_flush(int deviceId) {
		try {
			if (!initialized || error) {
				return WState.ERROR;
			}

			if (writebuff[deviceId].position() > 0) {
				if (sync && buffchk) {
					WState ws;
					ws = hardsid_usb_sync(deviceId);
					if (ws != WState.OK)
						return ws;
				}
				if ((writebuff[deviceId].position() % WRITEBUFF_SIZE) > 0) {
					writebuff[deviceId].putShort((short) 0xffff);
				}
				hardsid_usb_write_internal(deviceId);
			}

			return WState.OK;
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

	/**
	 * aborts the playback ASAP
	 * 
	 * @param deviceId device ID
	 */
	public void hardsid_usb_abortplay(int deviceId) {

		try {
			// fixed: 2010.01.26 - after Wilfred's testcase
			// abort the software buffer anyway
			writebuff[deviceId].clear();

			if (hardsid_usb_readstate(deviceId) != WState.OK) {
				error = true;
				return;
			}

			if (pkgcount == 0)
				return;

			hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
			hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
			hardsid_usb_write_internal(deviceId);
			while (true) {
				if (hardsid_usb_readstate(deviceId) != WState.OK) {
					error = true;
					break;
				} else {
					if (pkgcount == 0) {
						break;
					}
				}
			}
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException e) {
			e.printStackTrace();
			error = true;
		}
	}

	/**
	 * selects one of the sysmodes on the device
	 * 
	 * @param deviceId   device ID
	 * @param newsysmode SIDPLAY or VST
	 * @return state of USB device
	 */
	public WState hardsid_usb_setmode(int deviceId, SysMode newsysmode) {
		try {
			if (newsysmode != SysMode.SIDPLAY) {
				throw new RuntimeException("Only SIDPLAY mode currently supported!");
			}
			if (newsysmode == SysMode.VST && devtypes[0] != DevType.HS4U) {
				error = true;
				return WState.ERROR;
			}

			if (hardsid_usb_readstate(deviceId) != WState.OK) {
				error = true;
				return WState.ERROR;
			}

			if ((sysmode & 0x0f) == newsysmode.getSysMode())
				return WState.OK;

			hardsid_usb_abortplay(deviceId);

			hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
			hardsid_usb_write_direct(deviceId, (byte) 0x00, (byte) newsysmode.getSysMode());
			hardsid_usb_write_internal(deviceId);
			while (true) {
				if (hardsid_usb_readstate(deviceId) != WState.OK) {
					error = true;
					break;
				} else {
					if (sysmode == (newsysmode.getSysMode() | 0x80))
						break;
				}
			}

			if (error)
				return WState.ERROR;
			else
				return WState.OK;
		} catch (UsbNotActiveException | UsbNotOpenException | IllegalArgumentException | UsbDisconnectedException e) {
			e.printStackTrace();
			error = true;
			return WState.ERROR;
		}
	}

}
