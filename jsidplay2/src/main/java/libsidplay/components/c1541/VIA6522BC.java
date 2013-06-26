/*
 * via1d15xx.c - VIA1 emulation in the 1541, 1541II, 1570 and 1571 disk drive.
 *
 * Written by
 *  Andreas Boose <viceteam@t-online.de>
 *  Andre' Fachat <fachat@physik.tu-chemnitz.de>
 *  Daniel Sladic <sladic@eecg.toronto.edu>
 *  Ettore Perazzoli <ettore@comm2000.it>
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
package libsidplay.components.c1541;

import libsidplay.components.iec.IECBus;

/**
 * Handling of the Bus Controller.
 * 
 * @author Ken Händel
 * 
 */
public abstract class VIA6522BC extends VIACore {
	/**
	 * IEC Bus.
	 */
	private final IECBus iecbus;

	/**
	 * The parallel cable connected to the device.
	 */
	private IParallelCable parallelCable;

	/**
	 * The C1541 device ID.
	 */
	private int deviceId;

	/**
	 * Set a parallel cable.
	 * 
	 * @param cable parallel cable
	 */
	public final void setParallelCable(final IParallelCable cable) {
		this.parallelCable = cable;
	}

	/**
	 * Get the parallel cable.
	 * 
	 * @return parallel cable
	 */
	public final IParallelCable getParallelCable() {
		return parallelCable;
	}

	/**
	 * Creates a new instance of VIA6522BC.
	 * 
	 * @param id
	 *            the C1541 device ID
	 * @param bus
	 *            the IEC Bus
	 */
	public VIA6522BC(final int id, final IECBus bus) {
		super("1541Drive" + id + "VIA6522BC");
		this.deviceId = id;
		this.iecbus = bus;
	}

	@Override
	public void setCa2(final int state) {
	}

	@Override
	public void setCb2(final int state) {
	}

	@Override
	public final void storePra(final int addr, final byte value) {
		getParallelCable().driveWrite(value, (via[VIA_PCR] & 0xe) == 0xa,
				deviceId);
	}

	@Override
	public final void storePrb(final byte byt) {
		if (byt != oldpb) {
			iecbus.updateDrive(deviceId, byt);
		}
	}

	@Override
	public void storeAcr(final byte value) {
	}

	@Override
	public void storeSr(final byte value) {
	}

	@Override
	public void storeT2l(final byte value) {
	}

	@Override
	public final byte readPra() {
		return (byte) (via[VIACore.VIA_PRA] & via[VIACore.VIA_DDRA] | getParallelCable()
				.driveRead((via[VIA_PCR] & 0xe) == 0xa)
				& ~via[VIACore.VIA_DDRA]);
	}

	@Override
	public final byte readPrb() {
		/* 0 for drive0, 0x20 for drive 1 */
		final byte orval = (byte) ((deviceId & ~0x08) << 5);
		return (byte) ((via[VIACore.VIA_PRB] & 0x1a | iecbus.deviceRead()) ^ 0x85 | orval);
	}

}
