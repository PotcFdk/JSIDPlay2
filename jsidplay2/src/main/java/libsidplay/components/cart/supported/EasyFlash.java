/**

 * easyflash.c - Cartridge handling of the easyflash cart.
 *
 * Written by
 *  ALeX Kazik <alx@kazik.de>
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
 */
package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.Flash040Core;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class EasyFlash extends Cartridge {
	private Flash040Core core = new Flash040Core() {

		@Override
		protected long maincpuClk() {
			return pla.getCPU().getEventScheduler().getTime(Event.Phase.PHI1);
		}

		@Override
		protected void alarmUnset(final Event alarm) {
			pla.getCPU().getEventScheduler().cancel(alarm);
		}

		@Override
		protected void alarmSet(final Event alarm, long time) {
			pla.getCPU().getEventScheduler().schedule(alarm, time);
		}
	};

	private static final int EASYFLASH_N_BANK_BITS = 6;
	private static final int EASYFLASH_N_BANKS = (1 << (EASYFLASH_N_BANK_BITS));
	private static final int EASYFLASH_BANK_MASK = ((EASYFLASH_N_BANKS) - 1);

	/** the 29F040B state machine */
	private Flash040Core.Flash040Context easyflashStateLow;
	private Flash040Core.Flash040Context easyflashStateHigh;

	/** the jumper */
	private int easyflashJumper;

	/** writing back to crt enabled */
	private boolean easyflashCrtWrite;

	/** backup of the registers */
	private byte easyflashRegister00, easyflashRegister02;

	/**
	 * Decoding table of the modes.<BR>
	 * bit3 = jumper, bit2 = mode, bit1 = !exrom, bit0 = game
	 */
	private static final byte easyflashMemconfig[] = {

	/* jumper off, mode 0, trough 00,01,10,11 in game/exrom bits */
	3, /* exrom high, game low, jumper off */
	3, /* Reserved, don't use this */
	1, /* exrom low, game low, jumper off */
	1, /* Reserved, don't use this */

	/* jumper off, mode 1, trough 00,01,10,11 in game/exrom bits */
	2, 3, 0, 1,

	/* jumper on, mode 0, trough 00,01,10,11 in game/exrom bits */
	2, /* exrom high, game low, jumper on */
	3, /* Reserved, don't use this */
	0, /* exrom low, game low, jumper on */
	1, /* Reserved, don't use this */

	/* jumper on, mode 1, trough 00,01,10,11 in game/exrom bits */
	2, 3, 0, 1, };

	/** extra RAM */
	private byte[] easyflashRam = new byte[256];

	/** filename when attached */
	private String easyflashFilename;
	private int easyflashFiletype;

	private final String STRING_EASYFLASH = CARTRIDGE_NAME_EASYFLASH;

	/* --------------------------------------------------------------------- */

	protected void easyflashIO1Store(int addr, byte value) {
		switch (addr & 2) {
		case 0:
			/* bank register */
			easyflashRegister00 = (byte) (value & EASYFLASH_BANK_MASK);
			break;
		default:
			/* mode register we only remember led, mode, exrom, game */
			easyflashRegister02 = (byte) (value & 0x87);
			byte memMode = easyflashMemconfig[(easyflashJumper << 3)
					| (easyflashRegister02 & 0x07)];
			// TODO KSC: check me!
			// cart_config_changed_slotmain(memMode, memMode, CMODE_READ);
			/* bit3 = jumper, bit2 = mode, bit1 = !exrom, bit0 = game */
			pla.setGameExrom(true, true, (memMode & 0x01) == 0, (memMode & 0x02) != 0);
			/* change led */
			/* (value & 0x80) -> led on if true, led off if false */
		}
		cartRomhBankSetSlotmain(easyflashRegister00);
		cartRomlBankSetSlotmain(easyflashRegister00);
		cartPortConfigChangedSlotmain();
	}

	protected byte easyflashIO2Read(int addr) {
		return easyflashRam[addr & 0xff];
	}

	protected void easyflashIO2Store(int addr, byte value) {
		easyflashRam[addr & 0xff] = value;
	}

	protected byte easyflashIO1Peek(int addr) {
		return (addr & 2) != 0 ? easyflashRegister02 : easyflashRegister00;
	}

	protected void easyflashIO1Dump() {
		System.out.printf("Mode %d, LED %s, jumper %s\n",
				easyflashMemconfig[(easyflashJumper << 3)
						| (easyflashRegister02 & 0x07)],
				(easyflashRegister02 & 0x80) != 0 ? "on" : "off",
				easyflashJumper != 0 ? "on" : "off");
	}

	/* --------------------------------------------------------------------- */

	private boolean easyflashCheckEmpty(final byte[] data, int dataPos) {
		for (int i = 0; i < 0x2000; i++) {
			if ((data[dataPos + i] & 0xff) != 0xff) {
				return false;
			}
		}
		return true;
	}

	/**
	 * EasyFlash jumper.
	 * 
	 * @param val
	 *            EasyFlash jumper
	 */
	public void setEasyflashJumper(boolean val) {
		easyflashJumper = val ? 1 : 0;
	}

	/**
	 * Save to EasyFlash crt on detach?
	 * 
	 * @param val
	 *            save on detach
	 */
	public void setEasyflashCRTWrite(boolean val) {
		easyflashCrtWrite = val;
	}

	private void easyflashWriteChipIfNotEmpty(RandomAccessFile fd,
			final byte[] chipheader, final byte[] data, int dataPos)
			throws IOException {
		if (!easyflashCheckEmpty(data, dataPos)) {
			fd.write(chipheader, 0, 0x10);
			fd.write(data, dataPos, 0x2000);
		}
	}

	/* --------------------------------------------------------------------- */

	public byte easyflashRomlRead(int addr) {
		return core.flash040CoreRead(easyflashStateLow,
				((easyflashRegister00 & 0xff) << 13) + (addr & 0x1fff));
	}

	public void easyflashRomlStore(int addr, byte value) {
		core.flash040CoreStore(easyflashStateLow,
				((easyflashRegister00 & 0xff) << 13) + (addr & 0x1fff), value);
	}

	public byte easyflashRomhRead(int addr) {
		return core.flash040CoreRead(easyflashStateHigh,
				((easyflashRegister00 & 0xff) << 13) + (addr & 0x1fff));
	}

	public void easyflashRomhStore(int addr, byte value) {
		core.flash040CoreStore(easyflashStateHigh,
				((easyflashRegister00 & 0xff) << 13) + (addr & 0x1fff), value);
	}

	/* --------------------------------------------------------------------- */

	public void easyflashConfigInit() {
		easyflashIO1Store(0xde00, (byte) 0);
		easyflashIO1Store(0xde02, (byte) 0);
	}

	public void easyflashConfigSetup(byte[] rawcart) {
		easyflashStateLow = new Flash040Core.Flash040Context();
		easyflashStateHigh = new Flash040Core.Flash040Context();

		core.flash040coreInit(easyflashStateLow, pla.getCPU()
				.getEventScheduler(),
				Flash040Core.Flash040Type.FLASH040_TYPE_B, romlBanks);
		System.arraycopy(rawcart, 0, easyflashStateLow.flashData, 0, 0x80000);

		core.flash040coreInit(easyflashStateHigh, pla.getCPU()
				.getEventScheduler(),
				Flash040Core.Flash040Type.FLASH040_TYPE_B, romhBanks);
		System.arraycopy(rawcart, 0x80000, easyflashStateHigh.flashData, 0,
				0x80000);
	}

	/* --------------------------------------------------------------------- */

	private void easyflashCommonAttach(String filename) {
		easyflashFilename = filename;
	}

	public void easyflashBinAttach(String filename, byte[] rawcart)
			throws IOException {
		easyflashFiletype = 0;
		Arrays.fill(rawcart, 0, 0x100000, (byte) 0xff);

		try (RandomAccessFile fd = new RandomAccessFile(filename, "r")) {
			int low = 0;
			for (int i = 0; i < EASYFLASH_N_BANKS; i++, low += 0x2000) {
				fd.read(rawcart, low, 0x2000);
				fd.read(rawcart, low + 0x80000, 0x2000);
			}
		}
		easyflashFiletype = CARTRIDGE_FILETYPE_BIN;
		easyflashCommonAttach(filename);
	}

	public boolean easyflashCRTAttach(DataInputStream dis, byte[] rawcart,
			final String filename) throws IOException {
		byte[] chipheader = new byte[0x10];

		easyflashFiletype = 0;
		Arrays.fill(rawcart, 0, 0x100000, (byte) 0xff);

		while (true) {
			try {
				dis.readFully(chipheader);
			} catch (EOFException e) {
				break;
			}

			int bank = ((chipheader[0xa] & 0xff) << 8) | (chipheader[0xb] & 0xff);
			int offset = ((chipheader[0xc] & 0xff) << 8) | (chipheader[0xd] & 0xff);
			int length = ((chipheader[0xe] & 0xff) << 8) | (chipheader[0xf] & 0xff);

			if (length == 0x2000) {
				if (bank >= EASYFLASH_N_BANKS
						|| !(offset == 0x8000 || offset == 0xa000 || offset == 0xe000)) {
					return false;
				}
				dis.read(rawcart, (bank << 13)
						| (offset == 0x8000 ? 0 << 19 : 1 << 19), 0x2000);
			} else if (length == 0x4000) {
				if (bank >= EASYFLASH_N_BANKS || offset != 0x8000) {
					return false;
				}
				dis.read(rawcart, (bank << 13) | (0 << 19), 0x2000);
				dis.read(rawcart, (bank << 13) | (1 << 19), 0x2000);
			} else {
				return false;
			}
		}

		easyflashFiletype = CARTRIDGE_FILETYPE_CRT;
		easyflashCommonAttach(filename);
		return true;
	}

	public void easyflashDetach() throws IOException {
		if (easyflashCrtWrite) {
			easyflashFlushImage();
		}
		core.flash040CoreShutdown(easyflashStateLow);
		core.flash040CoreShutdown(easyflashStateHigh);
		easyflashStateLow = null;
		easyflashStateHigh = null;
		easyflashFilename = null;
	}

	public void easyflashFlushImage() throws IOException {
		if (easyflashFilename != null) {
			if (easyflashFiletype == CARTRIDGE_FILETYPE_BIN) {
				easyflashBINSave(easyflashFilename);
			} else if (easyflashFiletype == CARTRIDGE_FILETYPE_CRT) {
				easyflashCRTSave(easyflashFilename);
			}
		}
	}

	public void easyflashBINSave(final String filename) throws IOException {
		assert filename != null;

		try (RandomAccessFile fd = new RandomAccessFile(filename, "r")) {
			for (int i = 0; i < EASYFLASH_N_BANKS; i++) {
				fd.write(easyflashStateLow.flashData, i << 13, 0x2000);
				fd.write(easyflashStateHigh.flashData, i << 13, 0x2000);
			}
		}
	}

	public void easyflashCRTSave(final String filename) throws IOException {
		assert filename != null;

		try (RandomAccessFile fd = new RandomAccessFile(filename, "r")) {

			byte[] header = new byte[0x40];
			byte[] chipheader = new byte[0x10];

			System.arraycopy(CRT_HEADER.getBytes(US_ASCII), 0, header, 0,
					CRT_HEADER.getBytes(US_ASCII).length);

			header[0x13] = 0x40;
			header[0x14] = 0x01;
			header[0x17] = CARTRIDGE_EASYFLASH;
			header[0x18] = 0x01;
			System.arraycopy(STRING_EASYFLASH.getBytes(US_ASCII), 0, header,
					0x20, STRING_EASYFLASH.getBytes(US_ASCII).length);
			fd.write(header);

			System.arraycopy(CHIP_HEADER.getBytes(US_ASCII), 0, chipheader, 0,
					CHIP_HEADER.getBytes(US_ASCII).length);
			chipheader[0x06] = 0x20;
			chipheader[0x07] = 0x10;
			chipheader[0x09] = 0x02;
			chipheader[0x0e] = 0x20;
			for (int i = 0; i < EASYFLASH_N_BANKS; i++) {
				chipheader[0x0b] = (byte) i;
				chipheader[0x0c] = (byte) 0x80;
				easyflashWriteChipIfNotEmpty(fd, chipheader,
						easyflashStateLow.flashData, i << 13);
				chipheader[0x0c] = (byte) 0xa0;
				easyflashWriteChipIfNotEmpty(fd, chipheader,
						easyflashStateHigh.flashData, i << 13);
			}
		}
	}

	/* ---------------------------------------------------------------------*/ 
	
	protected static final Charset US_ASCII = Charset.forName("US-ASCII");
	/**
	 * see http://skoe.de/easyflash/
	 */
	private static String CARTRIDGE_NAME_EASYFLASH = "EasyFlash";
	private static int CARTRIDGE_FILETYPE_BIN = 1;
	private static int CARTRIDGE_FILETYPE_CRT = 2;
	private static String CRT_HEADER = "C64 CARTRIDGE   ";
	private static String CHIP_HEADER = "CHIP";
	private static byte CARTRIDGE_EASYFLASH = 32;

	private byte[] romlBanks = new byte[0x80000],
			romhBanks = new byte[0x80000];

	/** Expansion port ROML/ROMH/RAM banking. */
	int romlBankNum, romhBankNum;

	void cartRomhBankSetSlotmain(int bank) {
		romhBankNum = bank;
	}

	void cartRomlBankSetSlotmain(int bank) {
		romlBankNum = bank;
	}

	void cartPortConfigChangedSlotmain() {
		// TODO nothing to do?
		// mem_pla_config_changed();
		// ultimax_memptr_update();
	}

	public EasyFlash(final DataInputStream dis, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] rawcart = new byte[0x100000];
		// TODO: get filename for saveing
		easyflashCRTAttach(dis, rawcart, "d:/easyflash.crt"/* filename */);
		easyflashConfigSetup(rawcart);
	}

	@Override
	public void reset() {
		super.reset();
		easyflashConfigInit();
	}

	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int addr) {
			return pla.getDisconnectedBusBank().read(addr);
		}

		@Override
		public void write(int addr, byte value) {
			easyflashIO1Store(addr, value);
		}
	};

	private final Bank io2Bank = new Bank() {
		@Override
		public byte read(int addr) {
			return easyflashIO2Read(addr);
		}

		@Override
		public void write(int addr, byte value) {
			easyflashIO2Store(addr, value);
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

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int addr) {
			return easyflashRomlRead(addr);
		}

		@Override
		public void write(int addr, byte value) {
			easyflashRomlStore(addr, value);
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int addr) {
			return easyflashRomhRead(addr);
		}

		@Override
		public void write(int addr, byte value) {
			easyflashRomhStore(addr, value);
		}
	};

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getRomh() {
		return romhBank;
	}

}
