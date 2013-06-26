/*
 * actionreplay.c - Cartridge handling, Action Replay cart.
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
package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class ActionReplay extends Cartridge {
	
	boolean isActive;

	/**
	 * Currently active ROML bank.
	 */
	protected int currentRomBank;
	
	/**
	 * ROML banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romLBanks;

	protected boolean exportRam;

	protected final byte[] ram = new byte[0x2000];

	public ActionReplay(final DataInputStream dis, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romLBanks = new byte[4][0x2000];
		for (int i = 0; i < 4; i++) {
			dis.readFully(chipHeader);
			if ((chipHeader[0xb] & 0xff) > 3)
				throw new RuntimeException("Unexpected Chip header!");
			int bank = chipHeader[0xb] & 0xff;
			dis.readFully(romLBanks[bank]);
		}
	}
	
	protected final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
		    if (isActive) {
			    pla.setGameExrom((value & 1) == 0, (value & 2) != 0);
		    	currentRomBank = (value >> 3) & 3;
		    	exportRam = (value & 0x20) != 0;
			    if ((value & 0x40) != 0) {
			    	setNMI(false);
			    }

			    if ((value & 0x4) != 0) {
			    	isActive = false;
			    }
		    }
		}
	};

	protected final Bank io2Bank = new Bank() {
		@Override
		public byte read(int address) {
		    if (!isActive) {
		    	return pla.getDisconnectedBusBank().read(address);
		    }
		    if (exportRam)
		        return ram[address & 0x1fff];

		    return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		    if (isActive) {
	            if (exportRam)
	                ram[address & 0x1fff] = value;
		    }
		}
	};

	protected final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
		    if (exportRam)
		        return ram[address & 0x1fff];

		    return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		    if (exportRam)
		        ram[address & 0x1fff] = value;
		}
	};

	protected final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
		    return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	@Override
	public Bank getRomh() {
		return romhBank;
	}

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}
	
	@Override
	public void reset() {
		super.reset();
		isActive = true;
		io1Bank.write(0xde00, (byte) 0);
		Arrays.fill(ram, (byte) 0);
	}
	
	@Override
	public void installBankHooks(Bank[] cpuReadMap, Bank[] cpuWriteMap) {
		if (! exportRam) {
			return;
		}
		
		for (int i = 8; i < 10; i ++) {
			final Bank origBank = cpuWriteMap[i];
			if (origBank == romlBank) {
				continue;
			}
			
			cpuWriteMap[i] = new Bank() {
				@Override
				public void write(int address, byte value) {
					origBank.write(address, value);
					romlBank.write(address, value);
				}
			};
		}
	}
	
	private final Event newCartRomConfig = new Event("ActionReplay freeze") {
		@Override
		public void event() {
			isActive = true;
			io1Bank.write(0xde00, (byte) 0x23);
		}
	};
	
	@Override
	public void doFreeze() {
		pla.setNMI(true);
		pla.getCPU().getEventScheduler().schedule(newCartRomConfig, 3, Event.Phase.PHI1);
	}
}
