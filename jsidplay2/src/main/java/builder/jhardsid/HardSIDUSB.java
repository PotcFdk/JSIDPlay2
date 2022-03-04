package builder.jhardsid;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.List;

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;
import javax.usb.UsbNotActiveException;
import javax.usb.UsbNotClaimedException;
import javax.usb.UsbNotOpenException;
import javax.usb.UsbPipe;

/**
 * Implements hardsid.dll api calls Written by Sandor Téli
 * 
 * Java port by Ken Händel
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

	private static final int WRITEBUFF_SIZE = USB_PACKAGE_SIZE;
	private static final int WRITEBUFF_SIZE_SYNC = 0x800;

	private static final int MAX_DEVCOUNT = 4;
	private static final int READBUFF_SIZE = 64;
	private static final int HW_BUFFBEG = 0x2000;
	private static final int HW_BUFFSIZE = 0x2000;
	private static final int HW_FILLRATIO = 0x1000;

	private static final short VENDOR_ID = 0x6581;
	private static final short PRODUCT_ID = (short) 0x8580;

	private UsbInterface iface;
	private UsbDevice[] devhandles = new UsbDevice[MAX_DEVCOUNT];
	private UsbPipe[] inPipeBulk = new UsbPipe[MAX_DEVCOUNT];
	private UsbPipe[] outPipeBulk = new UsbPipe[MAX_DEVCOUNT];

	private DevType[] deviceTypes = new DevType[MAX_DEVCOUNT];
	private ByteBuffer[] writeBuffer = new ByteBuffer[MAX_DEVCOUNT];
	private byte lastaccsids[] = new byte[MAX_DEVCOUNT];

	private boolean initialized, error, sync, buffChk;

	private int deviceCount, bufferSize, pkgCount, playCursor, circBuffCursor;

	private short sysMode;

	private long lastRelaySwitch;

	public HardSIDUSB() {
		for (int i = 0; i < deviceTypes.length; i++) {
			deviceTypes[i] = DevType.UNKNOWN;
		}
		buffChk = true;
		bufferSize = WRITEBUFF_SIZE;
		playCursor = HW_BUFFBEG;
		circBuffCursor = HW_BUFFBEG;
	}

	/**
	 * initializes the management library
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

			if (sync) {
				bufferSize = WRITEBUFF_SIZE_SYNC;
			} else {
				bufferSize = WRITEBUFF_SIZE;
			}

			if (initialized) {
				hardsid_usb_close();
			}
			initialized = true;
			error = false;
			fnd = findAllDevices(UsbHostManager.getUsbServices().getRootUsbHub(), VENDOR_ID, PRODUCT_ID);
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

				for (d = 0; d < deviceCount; d++) {

					if (deviceTypes[d] == DevType.HSUP || deviceTypes[d] == DevType.HSUNO) {
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
		return deviceCount;
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
		switch (deviceTypes[deviceId]) {
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
	private boolean findAllDevices(UsbHub hub, short vendorId, short productId) {
		try {
			deviceCount = 0;

			// When you set this interval to 0 then usb4java only scans once during
			// application startup. If you want to trigger a manual device scan you can do
			// it by calling the scan method on the USB services class (Must be casted to
			// the usb4java implementation, because this is not a javax-usb feature
			((org.usb4java.javax.Services) UsbHostManager.getUsbServices()).scan();

			for (UsbDevice device : (List<UsbDevice>) hub.getAttachedUsbDevices()) {
				UsbDeviceDescriptor desc = device.getUsbDeviceDescriptor();
				if (device.isUsbHub()) {
					return findAllDevices((UsbHub) device, vendorId, productId);

				} else if (desc.idVendor() == vendorId && desc.idProduct() == productId) {

					UsbConfiguration configuration = device.getActiveUsbConfiguration();
					iface = configuration.getUsbInterface((byte) 0);
					try {
						// try to forcefully claim USB device, if supported!
						iface.claim(usbInterface -> true);
					} catch (UsbException e) {
						// try to just claim USB device
						try {
							iface.claim();
						} catch (UsbException e2) {
							System.err.println(e2.getMessage());
							printInstallationHint();
							return false;
						}
					}
					devhandles[deviceCount] = device;

					DevType devType = getDeviceType(device);
					if (devType == DevType.UNKNOWN) {
						return false;
					}
					deviceTypes[deviceCount] = devType;
					lastaccsids[deviceCount] = (byte) 0xff;

					outPipeBulk[deviceCount] = iface.getUsbEndpoint((byte) 0x02).getUsbPipe();
					outPipeBulk[deviceCount].open();

					inPipeBulk[deviceCount] = iface.getUsbEndpoint((byte) 0x81).getUsbPipe();
					inPipeBulk[deviceCount].open();

					writeBuffer[deviceCount] = ByteBuffer.allocate(bufferSize).order(ByteOrder.LITTLE_ENDIAN);
					deviceCount++;
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
				pkgCount = s.get(12) & 0xffff;
				playCursor = s.get(13) & 0xffff;
				circBuffCursor = s.get(14) & 0xffff;
				sysMode = s.get(15);
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

			if (playCursor < circBuffCursor)
				freespace = playCursor + HW_BUFFSIZE - circBuffCursor;
			else if (playCursor > circBuffCursor)
				freespace = playCursor - circBuffCursor;
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
			if (!initialized || error || writeBuffer[deviceId].position() == 0) {
				return WState.ERROR;
			}

			int pkgstowrite = ((writeBuffer[deviceId].position() - 1) / USB_PACKAGE_SIZE) + 1;
			int writesize = pkgstowrite * USB_PACKAGE_SIZE;
			writeBuffer[deviceId].clear();

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
				System.arraycopy(writeBuffer[deviceId].array(), 0, toWrite, 0, writesize);

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

		if (sync && (writeBuffer[deviceId].position() == (bufferSize - 2))) {
			WState ws = hardsid_usb_sync(deviceId);
			if (ws != WState.OK)
				return ws;
		}

		writeBuffer[deviceId].putShort((short) (((reg & 0xff) << 8) | (data & 0xff)));
		if (writeBuffer[deviceId].position() == bufferSize) {
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

			switch (deviceTypes[deviceId]) {
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

						while (lastRelaySwitch > 0 && (System.currentTimeMillis() - lastRelaySwitch) < 250) {
							Thread.sleep(0);
						}
						// timediff = GetTickCount() - lastrelayswitch;
						lastRelaySwitch = System.currentTimeMillis();

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
				if (sync && (writeBuffer[deviceId].position() == (bufferSize - 2))) {
					WState ws;
					ws = hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
					if (ws != WState.OK)
						return ws;
				} else if (sync && (writeBuffer[deviceId].position() == (bufferSize - 4))) {
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

			if (writeBuffer[deviceId].position() > 0) {
				if (sync && buffChk) {
					WState ws;
					ws = hardsid_usb_sync(deviceId);
					if (ws != WState.OK)
						return ws;
				}
				if ((writeBuffer[deviceId].position() % WRITEBUFF_SIZE) > 0) {
					writeBuffer[deviceId].putShort((short) 0xffff);
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
			writeBuffer[deviceId].clear();

			if (hardsid_usb_readstate(deviceId) != WState.OK) {
				error = true;
				return;
			}

			if (pkgCount == 0) {
				return;
			}
			hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
			hardsid_usb_write_direct(deviceId, (byte) 0xff, (byte) 0xff);
			hardsid_usb_write_internal(deviceId);
			while (true) {
				if (hardsid_usb_readstate(deviceId) != WState.OK) {
					error = true;
					break;
				} else {
					if (pkgCount == 0) {
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
			if (newsysmode == SysMode.VST && deviceTypes[0] != DevType.HS4U) {
				error = true;
				return WState.ERROR;
			}

			if (hardsid_usb_readstate(deviceId) != WState.OK) {
				error = true;
				return WState.ERROR;
			}

			if ((sysMode & 0x0f) == newsysmode.getSysMode())
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
					if (sysMode == (newsysmode.getSysMode() | 0x80)) {
						break;
					}
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

	public static void printInstallationHint() {
		if (OS.get() == OS.WINDOWS) {
			printWindowsInstallationHint();
		} else if (OS.get() == OS.LINUX) {
			printLinuxInstallationHint();
		} else if (OS.get() == OS.MAC) {
			printMacInstallationHint();
		}
	}

	private static void printLinuxInstallationHint() {
		System.err.println("\"To give proper permissions, please type the following commands:\"");
		System.err.println();
		System.err.println("sudo vi /etc/udev/rules.d/hardsid4u.rules");
		System.err.println("\"Now, add the following single line:\"");
		System.err.println(
				"SUBSYSTEM==\"usb\",ATTR{idVendor}==\"6581\",ATTR{idProduct}==\"8580\",MODE=\"0660\",GROUP=\"plugdev\"");
		System.err.println("\"And finally type this command to refresh device configuration:\"");
		System.err.println("sudo udevadm trigger");
	}

	private static void printMacInstallationHint() {
		System.err.println("Unknown things to do... N.Y.T");
	}

	private static void printWindowsInstallationHint() {
		System.err.println("Unknown things to do... N.Y.T");
	}

	private DevType getDeviceType(UsbDevice device) {
		DevType devType = DevType.UNKNOWN;
		try {
			System.out.println("Open device:");
			for (byte i = 1; i < 128; i++) {
				String descriptorString = device.getUsbStringDescriptor(i).getString();
				System.out.println(descriptorString);
				if (i == 2) {
					if (descriptorString.startsWith("HardSID 4U")) {
						devType = DevType.HS4U;
					} else {
						// XXX other hardware, HSUNO, HSUP?
						System.err.println("Unknown device, expected \"HardSID 4U\", but is " + descriptorString);
					}
				}
			}
		} catch (Exception e) {
		}
		return devType;
	}

}
