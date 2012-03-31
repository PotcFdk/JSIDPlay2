package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class MikroAss extends Cartridge {
	protected final byte[] romL;

	public MikroAss(final DataInputStream dis, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];
		dis.readFully(chipHeader);

		int bankLen = (chipHeader[0xe] & 0xff) << 8;
		romL = new byte[bankLen];
		dis.readFully(romL);
	}

	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			return romL[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	private final Bank io2Bank = new Bank() {
		@Override
		public byte read(int address) {
			return romL[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
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
	public void reset() {
		super.reset();
		pla.setGameExrom(true, false);
	}
}
