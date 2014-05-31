/**
 * georam.c - GEORAM emulation.
 *
 * Written by
 *  Marco van den Heuvel <blackystardust68@yahoo.com>
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
 * The GeoRAM is a banked memory system. It uses the registers at
 * $dffe and $dfff to determine what part of the GeoRAM memory should
 * be mapped to $de00-$deff.
 *
 * XXX The BBG (Battery Backed GeoRAM) is a version that retains the
 * RAM contents after power-off. This is not emulated, yet.
 *
 *@author Ken
 */
package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.Arrays;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class GeoRAM extends Cartridge {

	/**
	 * GEORAM register selects a page within a 16KB bank.
	 * 
	 * <PRE>
	 * Register | bits
	 * -------------------
	 * $dffe    | xx543210
	 * 
	 * x = unused, not connected.
	 * </PRE>
	 */
	protected byte dffe;
	/**
	 * GEORAM register selects a 16KB bank. The number of 16k blocks that is
	 * available depends on the size of the GeoRAM/BBG:
	 * 
	 * <PRE>
	 * RAM size | $dfff
	 * ------------------
	 *   64k | $00-$03
	 *  128k | $00-$07
	 *  256k | $00-$0f
	 *  512k | $00-$1f
	 * 1024k | $00-$3f
	 * 2048k | $00-$7f
	 * 2048k | $00-$ff
	 * </PRE>
	 */
	protected byte dfff;
	/**
	 * GEORAM memory.
	 */
	protected byte[] ram;

	public GeoRAM(DataInputStream dis, PLA pla, int sizeKB) throws IOException {
		super(pla);
		assert sizeKB == 64 || sizeKB == 128 || sizeKB == 256 || sizeKB == 512
				|| sizeKB == 1 << 10 || sizeKB == 2 << 10;

		if (sizeKB == 0) {
			// empty file means maximum size!
			sizeKB = 2 << 10;
		}
		ram = new byte[sizeKB << 10];
		Arrays.fill(ram, (byte) 0);
		if (dis != null) {
			try {
				dis.readFully(ram);
			} catch (EOFException e) {
				/* no problem, we'll just keep the rest uninitialized... */
			}
		}
		reset();
	}

	@Override
	public void reset() {
		super.reset();
		dffe = 0;
		dfff = 0;
	}

	protected final Bank io1Bank = new Bank() {
		@Override
		public byte read(final int address) {
			return ram[dfff << 14 | dffe << 8 | address & 0xff];
		}

		@Override
		public void write(final int address, final byte value) {
			ram[dfff << 14 | dffe << 8 | address & 0xff] = value;
		}
	};

	protected final Bank io2Bank = new Bank() {
		@Override
		public byte read(final int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(final int address, final byte value) {
			switch (address & 0xff) {
			case 0xff:
				// 16 KB bank selector
				dfff = (byte) (value & (ram.length >> 14) - 1);
				break;
			case 0xfe:
				// page selector within a bank (6 bits are used)
				dffe = (byte) (value & 0x3f);
				break;

			default:
				break;
			}
		}
	};

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + (ram.length >> 10) + " KB)";
	}

}
