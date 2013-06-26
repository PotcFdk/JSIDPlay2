package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class EpyxFastLoad extends Cartridge {
	/**
	 * Currently active ROML bank.
	 */
	protected final byte[] romL;

	public EpyxFastLoad(final DataInputStream is, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		// first chip header contains ROM_L
		is.readFully(chipHeader);

		int bankLen = (chipHeader[0xe] & 0xff) << 8;
		romL = new byte[bankLen];
		is.readFully(romL);
	}

	private final Bank romL_Bank = new Bank() {
		@Override
		public byte read(int address) {
			return romL[address & 0x1fff];
		}
	};

	private final Bank io2Bank = new Bank() {
		@Override
		public byte read(int address) {
		    if (address == 0xdf18) {
		    	pla.setGameExrom(true, false);
		    }
		    if (address == 0xdf38) {
		    	pla.setGameExrom(true, true);
		    }
		    return romL[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	@Override
	public Bank getRoml() {
		return romL_Bank;
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}
	
	@Override
	public void reset() {
		super.reset();
		io2Bank.read(0xdf18);
	}
}
