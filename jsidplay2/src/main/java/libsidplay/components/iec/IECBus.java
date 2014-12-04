/*
 * iecbus.c - IEC bus handling.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.iec;

import java.util.Arrays;

import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.VIACore;

/**
 * IEC Bus Implementation.
 * 
 * @author Ken Händel
 * 
 */
public class IECBus {
	/**
	 * Number of IEC devices.
	 */
	public static final int IECBUS_NUM = 16;

	protected byte drvBus[] = new byte[IECBUS_NUM];
	protected byte drvData[] = new byte[IECBUS_NUM];
	protected byte drvPort;
	protected byte cpuBus;
	protected byte cpuPort;

	/**
	 * Floppy Disk Drives connected to the bus.
	 */
	protected C1541[] drives;

	/**
	 * Serial devices connected to the bus.
	 */
	protected SerialIECDevice[] serialDevices;

	public IECBus() {
		drives = new C1541[0];
		serialDevices = new SerialIECDevice[0];
		reset();
	}

	/**
	 * Reset bus state.
	 */
	public final void reset() {
		Arrays.fill(drvBus, (byte) 0xff);
		Arrays.fill(drvData, (byte) 0xff);
		cpuBus = (byte) 0xff;
		cpuPort = (byte) 0xff;
		drvPort = (byte) 0x85;
	}

	/**
	 * Set Floppy Disk Drives.
	 * 
	 * @param drvs
	 *            Floppies
	 */
	public final void setFloppies(final C1541[] drvs) {
		this.drives = drvs;
	}

	/**
	 * Set Serial Devices.
	 * 
	 * @param devices
	 *            Serial Devices
	 */
	public final void setSerialDevices(final SerialIECDevice[] devices) {
		serialDevices = devices;
	}

	// triggered by CIA
	/**
	 * Read from IEC bus.
	 * 
	 * @return bus value
	 */
	public final byte readFromIECBus() {
		for (final SerialIECDevice serialDevice : serialDevices) {
			serialDevice.clock();
		}
		return cpuPort;
	}

	// triggered by CIA
	/**
	 * Write to IEC bus.
	 * 
	 * @param data
	 *            write value
	 */
	public final void writeToIECBus(final byte data) {
		for (final SerialIECDevice serialDevice : serialDevices) {
			serialDevice.clock();
		}
		final byte oldCpuBus = cpuBus;
		cpuBus = (byte) ((data & 0xff) << 2 & 0x80 | (data & 0xff) << 2 & 0x40 | (data & 0xff) << 1 & 0x10);

		if (((oldCpuBus ^ cpuBus) & 0x10) != 0) {
			for (final C1541 drive : drives) {
				drive.getBusController().signal(VIACore.VIA_SIG_CA1,
						(cpuBus & 0x10) != 0 ? 0 : VIACore.VIA_SIG_RISE);
			}
		}
		for (final C1541 drive : drives) {
			setDriveBus(drive.getID());
		}
		updatePorts();
	}

	// triggered by SerialIECDevices
	public final byte deviceRead() {
		return drvPort;
	}

	// triggered by SerialIECDevices
	public final void deviceWrite(final int deviceNum, final byte data) {
		drvBus[deviceNum] = data;
		updatePorts();
	}

	// triggered by VIA6522BC
	public final void updateDrive(final int deviceNum, final byte byt) {
		drvData[deviceNum] = (byte) ~byt;
		setDriveBus(deviceNum);
		updatePorts();
	}

	protected final void setDriveBus(final int deviceNum) {
		drvBus[deviceNum] = (byte) ((drvData[deviceNum] & 0xff) << 3 & 0x40 | (drvData[deviceNum] & 0xff) << 6
				& ((~drvData[deviceNum] ^ cpuBus) & 0xff) << 3 & 0x80);
	}

	protected final void updatePorts() {
		cpuPort = cpuBus;
		for (final SerialIECDevice serialDevice : serialDevices) {
			cpuPort &= drvBus[serialDevice.getID()];
		}
		for (final C1541 drive : drives) {
			cpuPort &= drvBus[drive.getID()];
		}
		drvPort = (byte) ((cpuPort & 0xff) >> 4 & 0x4 | (cpuPort & 0xff) >> 7 | (cpuBus & 0xff) << 3 & 0x80);
	}

}
