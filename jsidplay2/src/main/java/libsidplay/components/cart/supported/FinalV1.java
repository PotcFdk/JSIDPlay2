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
public class FinalV1 extends Cartridge {
	protected final byte[] roml = new byte[0x2000];
	protected final byte[] romh = new byte[0x2000];

	public FinalV1(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];
		dis.readFully(chipHeader);
		if (chipHeader[0xc] != (byte) 0x80 || chipHeader[0xe] != 0x40)
			throw new RuntimeException("Unexpected Chip header!");
		dis.readFully(roml);
		dis.readFully(romh);
	}
	
	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			FinalV1.this.setNMI(false);
			pla.setGameExrom(true, true);
			return roml[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
			FinalV1.this.setNMI(false);
			pla.setGameExrom(true, true);
		}
	};

	private final Bank io2Bank = new Bank() {
		@Override
		public byte read(int address) {
			pla.setGameExrom(false, false);
			return roml[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
			pla.setGameExrom(false, false);
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
		    return roml[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return romh[address & 0x1fff];
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
		return io1Bank;
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
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
		pla.setGameExrom(true, true);
	}
}
