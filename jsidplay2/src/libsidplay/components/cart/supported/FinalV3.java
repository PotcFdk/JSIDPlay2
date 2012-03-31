package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * This cartridge has a freeze button. It is assumed that the freeze
 * occurs by triggering NMI in the CPU. It is not known whether the NMI state is
 * held low while in cartridge. Releasing the freeze doesn't seem to really work,
 * either.
 */
public class FinalV3 extends Cartridge {
	/**
	 * Currently active ROM bank.
	 */
	protected int currentRomBank;

	/**
	 * ROML banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romLBanks;

	/**
	 * ROMH banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romHBanks;
	
	protected boolean controlRegAvailable;
	
	public FinalV3(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romLBanks = new byte[4][0x2000];
		romHBanks = new byte[4][0x2000];
		for (int i = 0; i < 4; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xc] != (byte) 0xa0 && chipHeader[0xe] != 0x40
					&& chipHeader[0xb] > 3)
				throw new RuntimeException("Unexpected Chip header!");
			int bank = chipHeader[0xb] & 0xff;
			dis.readFully(romLBanks[bank]);
			dis.readFully(romHBanks[bank]);
		}
	}
	
	private final Bank ioBank = new Bank() {
		@Override
		public byte read(int address) {
			return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		    if (controlRegAvailable && address == 0xdfff) {
		        currentRomBank = value & 3;
		        pla.setGameExrom(true, true, (value & 0x20) != 0, (value & 0x10) != 0);
		        setNMI((value & 0x40) == 0);
		        if ((value & 0x80) != 0) {
		        	controlRegAvailable = false;
		        }
		    }
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
		    return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return romHBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
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

	@Override
	public Bank getIO1() {
		return ioBank;
	}

	@Override
	public Bank getIO2() {
		return ioBank;
	}

	@Override
	protected void doFreeze() {
		setNMI(true);
		pla.getCPU().getEventScheduler().schedule(new Event("Freeze") {
			@Override
			public void event() {
				pla.setGameExrom(false, true);
			}
		}, 3, Phase.PHI1);
	}
	
	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(false, false);
		currentRomBank = 0;
		controlRegAvailable = true;
	}
}
