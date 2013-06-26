package libsidplay.components.cart.supported.core;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

/**
 * flash040core.c - (AM)29F0[14]0(B) Flash emulation.
 * 
 * Written by Hannu Nuotio <hannu.nuotio@tut.fi>
 * 
 * This file is part of VICE, the Versatile Commodore Emulator. See README for
 * copyright notice.
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA.
 * 
 * @author Ken
 * 
 */
public abstract class Flash040Core {
	private Logger FLASH_DEBUG = Logger.getLogger(Flash040Core.class.getName());
	
	public enum Flash040Type {
		/* 29F040 */
		FLASH040_TYPE_NORMAL,
		/* 29F040B */
		FLASH040_TYPE_B,
		/* 29F010 */
		FLASH040_TYPE_010,
		/* 29F032B, A0/1 swapped */
		FLASH040_TYPE_032B_A0_1_SWAP, FLASH040_TYPE_NUM
	};

	private enum Flash040State {
		FLASH040_STATE_READ,
		FLASH040_STATE_MAGIC_1,
		FLASH040_STATE_MAGIC_2,
		FLASH040_STATE_AUTOSELECT,
		FLASH040_STATE_BYTE_PROGRAM,
		FLASH040_STATE_BYTE_PROGRAM_ERROR,
		FLASH040_STATE_ERASE_MAGIC_1,
		FLASH040_STATE_ERASE_MAGIC_2,
		FLASH040_STATE_ERASE_SELECT,
		FLASH040_STATE_CHIP_ERASE,
		FLASH040_STATE_SECTOR_ERASE,
		FLASH040_STATE_SECTOR_ERASE_TIMEOUT,
		FLASH040_STATE_SECTOR_ERASE_SUSPEND
	};

	private static final int FLASH040_ERASE_MASK_SIZE = 8;

	public static class Flash040Context {
		public byte[] flashData;
		protected Flash040State flashState;
		protected Flash040State flashBaseState;

		protected byte programByte;
		protected byte[] eraseMask = new byte[FLASH040_ERASE_MASK_SIZE];
		protected int flashDirty;

		protected Flash040Type flashType;

		protected byte lastRead;
		protected Event eraseAlarm;
	}

	/** Timeout after sector erase command (datasheet states 50us) */
	private static int ERASE_SECTOR_TIMEOUT_CYCLES = 50;
	/** Time taken by sector & chip erase (FIXME: numbers pulled from a hat) */
	private static int ERASE_SECTOR_CYCLES = 1012;
	private static int ERASE_CHIP_CYCLES = 8192;

	protected static class FlashTypes {
		protected FlashTypes(byte manufacturer, byte device, int sz,
				int secMask, int secSize, int secShift, int mag1Addr,
				int mag2Addr, int mag1Mask, int mag2Mask, byte status) {
			manufacturerID = manufacturer;
			deviceID = device;
			size = sz;
			sectorMask = secMask;
			sectorSize = secSize;
			sectorShift = secShift;
			magic1Addr = mag1Addr;
			magic2Addr = mag2Addr;
			magic1Mask = mag1Mask;
			magic2Mask = mag2Mask;
			statusToggleBits = status;
		}

		protected byte manufacturerID;
		protected byte deviceID;
		protected int size;
		protected int sectorMask;
		protected int sectorSize;
		protected int sectorShift;
		protected int magic1Addr;
		protected int magic2Addr;
		protected int magic1Mask;
		protected int magic2Mask;
		protected byte statusToggleBits;
	};

	protected static FlashTypes FlashTypes[] = {
			/* 29F040 */
			new FlashTypes((byte) 0x01, (byte) 0xa4, 0x80000, 0x70000, 0x10000,
					16, 0x5555, 0x2aaa, 0x7fff, 0x7fff, (byte) 0x40),
			/* 29F040B */
			new FlashTypes((byte) 0x01, (byte) 0xa4, 0x80000, 0x70000, 0x10000,
					16, 0x555, 0x2aa, 0x7ff, 0x7ff, (byte) 0x40),
			/* 29F010 */
			new FlashTypes((byte) 0x01, (byte) 0x20, 0x20000, 0x1c000, 0x04000,
					14, 0x5555, 0x2aaa, 0x7fff, 0x7fff, (byte) 0x40),
			/* 29F032B with A0/1 swap */
			new FlashTypes((byte) 0x01, (byte) 0x41, 0x400000, 0x3f0000,
					0x10000, 16, 0x556, 0x2a9, 0x7ff, 0x7ff, (byte) 0x44), };

	/* -------------------------------------------------------------------------- */ 
	
	private boolean flashMagic1(Flash040Context flash040Context, int addr) {
		return ((addr & FlashTypes[flash040Context.flashType.ordinal()].magic1Mask) == FlashTypes[flash040Context.flashType
				.ordinal()].magic1Addr);
	}

	private boolean flashMagic2(Flash040Context flash040Context, int addr) {
		return ((addr & FlashTypes[flash040Context.flashType.ordinal()].magic2Mask) == FlashTypes[flash040Context.flashType
				.ordinal()].magic2Addr);
	}

	private void flashClearEraseMask(Flash040Context flash040Context) {
		for (int i = 0; i < FLASH040_ERASE_MASK_SIZE; ++i) {
			flash040Context.eraseMask[i] = 0;
		}
	}

	private int flashSectorToAddr(Flash040Context flash040Context, int sector) {
		int sectorSize = FlashTypes[flash040Context.flashType.ordinal()].sectorSize;

		return sector * sectorSize;
	}

	private int flashAddrToSectorNumber(Flash040Context flash040Context,
			int addr) {
		int sectorAddr = FlashTypes[flash040Context.flashType.ordinal()].sectorMask
				& addr;
		int sectorShift = FlashTypes[flash040Context.flashType.ordinal()].sectorShift;

		return sectorAddr >> sectorShift;
	}

	private void flashAddSectorToEraseMask(Flash040Context flash040Context,
			int addr) {
		int sectorNum = flashAddrToSectorNumber(flash040Context, addr);

		flash040Context.eraseMask[sectorNum >> 3] |= (byte) (1 << (sectorNum & 0x7));
	}

	private void flashEraseSector(Flash040Context flash040Context, int sector) {
		int sectorSize = FlashTypes[flash040Context.flashType.ordinal()].sectorSize;
		int sectorAddr = flashSectorToAddr(flash040Context, sector);

		FLASH_DEBUG.fine(String.format("Erasing 0x%x - 0x%x", sectorAddr,
				sectorAddr + sectorSize - 1));
		Arrays.fill(flash040Context.flashData, sectorAddr, sectorSize,
				(byte) 0xff);
		flash040Context.flashDirty = 1;
	}

	private void flashEraseChip(Flash040Context flash040Context) {
		FLASH_DEBUG.fine(("Erasing chip"));
		Arrays.fill(flash040Context.flashData, 0,
				FlashTypes[flash040Context.flashType.ordinal()].size,
				(byte) 0xff);
		flash040Context.flashDirty = 1;
	}

	private boolean flashProgramByte(Flash040Context flash040Context, int addr,
			byte byt) {
		byte oldData = flash040Context.flashData[addr];
		byte newData = (byte) (oldData & byt);

		FLASH_DEBUG.fine(String.format(
				"Programming 0x%05x with 0x%02x (%02x->%02x)", addr, byt,
				oldData, oldData & byt));
		flash040Context.programByte = byt;
		flash040Context.flashData[addr] = newData;
		flash040Context.flashDirty = 1;

		return (newData == byt);
	}

	private byte flashWriteOperationStatus(Flash040Context flash040Context) {
		return (byte) (((flash040Context.programByte ^ 0x80) & 0x80) /*
															 * DQ7 = inverse of
															 * programmed data
															 */
				| ((maincpuClk() & 2) << 5) /*
												 * DQ6 = toggle bit (2 us)
												 */
				| (1 << 5) /* DQ5 = timeout */)
		;
	}

	private byte flashEraseOperationStatus(Flash040Context flash040Context) {
		/* DQ6 = toggle bit */
		byte v = flash040Context.programByte;

		/* toggle the toggle bit(s) */
		/* FIXME better toggle bit II emulation */
		flash040Context.programByte ^= FlashTypes[flash040Context.flashType
				.ordinal()].statusToggleBits;

		/* DQ3 = sector erase timer */
		if (flash040Context.flashState != Flash040State.FLASH040_STATE_SECTOR_ERASE_TIMEOUT) {
			v |= 0x08;
		}

		return v;
	}

	/* -------------------------------------------------------------------------- */ 
	
	protected void eraseAlarmHandler(Flash040Context flash040Context) {
		alarmUnset(flash040Context.eraseAlarm);

		FLASH_DEBUG.fine(String.format("Erase alarm, state %s",
				flash040Context.flashState));

		switch (flash040Context.flashState) {
		case FLASH040_STATE_SECTOR_ERASE_TIMEOUT:
		case FLASH040_STATE_SECTOR_ERASE:
			for (int i = 0; i < (8 * FLASH040_ERASE_MASK_SIZE); ++i) {
				int j = i >> 3;
				byte m = (byte) (1 << (i & 0x7));
				if ((flash040Context.eraseMask[j] & m) != 0) {
					flashEraseSector(flash040Context, i);
					flash040Context.eraseMask[j] &= (byte) ~m;
					break;
				}
			}

			byte m = 0;
			for (int i = 0; i < FLASH040_ERASE_MASK_SIZE; ++i) {
				m |= flash040Context.eraseMask[i];
			}

			if (m != 0) {
				alarmSet(flash040Context.eraseAlarm, maincpuClk()
						+ ERASE_SECTOR_CYCLES);
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_CHIP_ERASE:
			flashEraseChip(flash040Context);
			flash040Context.flashState = flash040Context.flashBaseState;
			break;

		default:
			FLASH_DEBUG.fine(String.format(
					"Erase alarm - error, state %s unhandled!",
					flash040Context.flashState));
			break;
		}

	}

	/* -------------------------------------------------------------------------- */
	 
	public void flash040CoreStore(Flash040Context flash040Context, int addr,
			byte byt) {
		Flash040State oldState = flash040Context.flashState;
		Flash040State oldBaseState = flash040Context.flashBaseState;

		switch (flash040Context.flashState) {
		case FLASH040_STATE_READ:
			if (flashMagic1(flash040Context, addr) && (byt == (byte) 0xaa)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_MAGIC_1;
			}
			break;

		case FLASH040_STATE_MAGIC_1:
			if (flashMagic2(flash040Context, addr) && (byt == 0x55)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_MAGIC_2;
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_MAGIC_2:
			if (flashMagic1(flash040Context, addr)) {
				switch (byt & 0xff) {
				case 0x90:
					flash040Context.flashState = Flash040State.FLASH040_STATE_AUTOSELECT;
					flash040Context.flashBaseState = Flash040State.FLASH040_STATE_AUTOSELECT;
					break;
				case 0xf0:
					flash040Context.flashState = Flash040State.FLASH040_STATE_READ;
					flash040Context.flashBaseState = Flash040State.FLASH040_STATE_READ;
					break;
				case 0xa0:
					flash040Context.flashState = Flash040State.FLASH040_STATE_BYTE_PROGRAM;
					break;
				case 0x80:
					flash040Context.flashState = Flash040State.FLASH040_STATE_ERASE_MAGIC_1;
					break;
				default:
					flash040Context.flashState = flash040Context.flashBaseState;
					break;
				}
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_BYTE_PROGRAM:
			if (flashProgramByte(flash040Context, addr, byt)) {
				/* The byte program time is short enough to ignore */
				flash040Context.flashState = flash040Context.flashBaseState;
			} else {
				flash040Context.flashState = Flash040State.FLASH040_STATE_BYTE_PROGRAM_ERROR;
			}
			break;

		case FLASH040_STATE_ERASE_MAGIC_1:
			if (flashMagic1(flash040Context, addr) && (byt == (byte) 0xaa)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_ERASE_MAGIC_2;
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_ERASE_MAGIC_2:
			if (flashMagic2(flash040Context, addr) && (byt == 0x55)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_ERASE_SELECT;
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_ERASE_SELECT:
			if (flashMagic1(flash040Context, addr) && (byt == 0x10)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_CHIP_ERASE;
				flash040Context.programByte = 0;
				alarmSet(flash040Context.eraseAlarm, maincpuClk()
						+ ERASE_CHIP_CYCLES);
			} else if (byt == 0x30) {
				flashAddSectorToEraseMask(flash040Context, addr);
				flash040Context.programByte = 0;
				flash040Context.flashState = Flash040State.FLASH040_STATE_SECTOR_ERASE_TIMEOUT;
				alarmSet(flash040Context.eraseAlarm, maincpuClk()
						+ ERASE_SECTOR_TIMEOUT_CYCLES);
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
			}
			break;

		case FLASH040_STATE_SECTOR_ERASE_TIMEOUT:
			if (byt == 0x30) {
				flashAddSectorToEraseMask(flash040Context, addr);
			} else {
				flash040Context.flashState = flash040Context.flashBaseState;
				flashClearEraseMask(flash040Context);
				alarmUnset(flash040Context.eraseAlarm);
			}
			break;

		case FLASH040_STATE_SECTOR_ERASE:
			/* TODO not all models support suspending */
			if (byt == (byte) 0xb0) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_SECTOR_ERASE_SUSPEND;
				alarmUnset(flash040Context.eraseAlarm);
			}
			break;

		case FLASH040_STATE_SECTOR_ERASE_SUSPEND:
			if (byt == 0x30) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_SECTOR_ERASE;
				alarmSet(flash040Context.eraseAlarm, maincpuClk()
						+ ERASE_SECTOR_CYCLES);
			}
			break;

		case FLASH040_STATE_BYTE_PROGRAM_ERROR:
		case FLASH040_STATE_AUTOSELECT:
			if (flashMagic1(flash040Context, addr) && (byt == (byte) 0xaa)) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_MAGIC_1;
			}
			if (byt == (byte) 0xf0) {
				flash040Context.flashState = Flash040State.FLASH040_STATE_READ;
				flash040Context.flashBaseState = Flash040State.FLASH040_STATE_READ;
			}
			break;

		case FLASH040_STATE_CHIP_ERASE:
		default:
			break;
		}

		FLASH_DEBUG.fine(String.format(
				"Write %02x to %05x, state %s->%s (base state %s->%s)", byt,
				addr, oldState, flash040Context.flashState, oldBaseState,
				flash040Context.flashBaseState));
	}

	public byte flash040CoreRead(Flash040Context flash040Context, int addr) {
		byte value;
		Flash040State oldState = flash040Context.flashState;

		switch (flash040Context.flashState) {
		case FLASH040_STATE_AUTOSELECT:
			if (flash040Context.flashType == Flash040Type.FLASH040_TYPE_032B_A0_1_SWAP) {
				if ((addr & 0xff) < 4) {
					// TODO Debug and check me
					addr = "\0\2\1\3".charAt(addr & 0x3);
				}
			}

			switch (addr & 0xff) {
			case 0x00:
				value = FlashTypes[flash040Context.flashType.ordinal()].manufacturerID;
				break;
			case 0x01:
				value = FlashTypes[flash040Context.flashType.ordinal()].deviceID;
				break;
			case 0x02:
				value = 0;
				break;
			default:
				value = flash040Context.flashData[addr];
				break;
			}
			break;

		case FLASH040_STATE_BYTE_PROGRAM_ERROR:
			value = flashWriteOperationStatus(flash040Context);
			break;

		case FLASH040_STATE_SECTOR_ERASE_SUSPEND:
		case FLASH040_STATE_CHIP_ERASE:
		case FLASH040_STATE_SECTOR_ERASE:
		case FLASH040_STATE_SECTOR_ERASE_TIMEOUT:
			value = flashEraseOperationStatus(flash040Context);
			break;

		default:
			/*
			 * The state doesn't reset if a read occurs during a command
			 * sequence
			 */
			/* fall through */
		case FLASH040_STATE_READ:
			value = flash040Context.flashData[addr];
			break;
		}

		if (FLASH_DEBUG.isLoggable(Level.FINE)
				&& oldState != Flash040State.FLASH040_STATE_READ) {
			FLASH_DEBUG.fine(String.format("Read %02x from %05x, state %s.%s",
					value, addr, oldState, flash040Context.flashState));
		}

		flash040Context.lastRead = value;
		return value;
	}

	public byte flash040CorePeek(Flash040Context flash040Context, int addr) {
		return flash040Context.flashData[addr];
	}

	public void flash040CoreReset(Flash040Context flash040Context) {
		FLASH_DEBUG.fine(("Reset"));
		flash040Context.flashState = Flash040State.FLASH040_STATE_READ;
		flash040Context.flashBaseState = Flash040State.FLASH040_STATE_READ;
		flash040Context.programByte = 0;
		flashClearEraseMask(flash040Context);
		alarmUnset(flash040Context.eraseAlarm);
	}

	public void flash040coreInit(final Flash040Context flash040Context,
			EventScheduler alarmContext, Flash040Type type, byte[] data) {
		FLASH_DEBUG.fine(("Init"));
		flash040Context.flashData = data;
		flash040Context.flashType = type;
		flash040Context.flashState = Flash040State.FLASH040_STATE_READ;
		flash040Context.flashBaseState = Flash040State.FLASH040_STATE_READ;
		flash040Context.programByte = 0;
		flashClearEraseMask(flash040Context);
		flash040Context.flashDirty = 0;
		flash040Context.eraseAlarm = new Event("Flash040Alarm") {

			@Override
			public void event() throws InterruptedException {
				eraseAlarmHandler(flash040Context);
			}
		};
	}

	public void flash040CoreShutdown(Flash040Context flash040Context) {
		FLASH_DEBUG.fine(("Shutdown"));
	}

	/* -------------------------------------------------------------------------- */ 
	
	protected abstract long maincpuClk();

	protected abstract void alarmUnset(Event erase_alarm);

	protected abstract void alarmSet(Event erase_alarm, long i);

}