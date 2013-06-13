package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class Comal80 extends Cartridge {

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
	
	/**
	 * Load a Comal80 cartridge.
	 * 
	 * @param dis
	 *            stream to load from
	 * @param pla
	 * @throws IOException
	 *             load error
	 */
	public Comal80(final DataInputStream dis, final PLA pla) throws IOException {
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

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			return romLBanks[currentRomBank][(address & 0x1fff)];
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return romHBanks[currentRomBank][(address & 0x1fff)];
		}
	};

	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			int iv = value & 0xff;
			if (iv >= 0x80 && iv <= 0x83) {
				currentRomBank = (value & 3);
			}
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
	public void reset() {
		super.reset();
		io1Bank.write(0xde00, (byte) 0x80);
		pla.setGameExrom(false, false);
	}
}
