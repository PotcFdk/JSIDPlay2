package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class Rex extends Cartridge {
	protected final byte[] romL;

	public Rex(final DataInputStream is, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];
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
	        if ((address & 0xff) < 0xc0)
	        	pla.setGameExrom(true, true);
	        else
	        	pla.setGameExrom(true, false);
	        
	        return 0;
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
		io2Bank.read(0xdeff);
	}
}
