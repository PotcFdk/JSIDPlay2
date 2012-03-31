package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class Zaxxon extends Cartridge {
	protected final byte[] roml;
	
	protected final byte[][] romh;

	protected byte[] romhActive;
	
	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			romhActive = romh[(address & 0x1000) >> 12];
			return roml[address & 0xfff];
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return romhActive[address & 0x1fff];
		}
	};

	/**
	 * Load a Zaxxon cartridge.
	 * 
	 * @param is
	 *            stream to load from
	 * @throws IOException
	 *             load error
	 */
	public Zaxxon(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		// first chip header contains ROM_L
		dis.readFully(chipHeader);

		// the reason both 0x10 and 0x20 are OK is that the first 0x10 is only
		// used, and the second part is probably just a mirror of the first.
		if (chipHeader[0xc] != (byte) 0x80
				|| (chipHeader[0xe] != 0x10 && chipHeader[0xe] != 0x20))
			throw new IOException("Unexpected Chip header!");

		int bankLen = (chipHeader[0xe] & 0xff) << 8;
		roml = new byte[bankLen];
		dis.readFully(roml);

		// second chip header contains ROM_H0
		// third chip header contains ROM_H1
		romh = new byte[2][0x2000];
		for (int i = 0; i < 2; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xc] != (byte) 0xa0 || chipHeader[0xe] != 0x20)
				throw new RuntimeException("Unexpected Chip header!");
			byte bankNum = chipHeader[0xb];
			dis.readFully(romh[bankNum]);
		}
	}

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getRomh() {
		return romhBank;
	}
	
	@Override
	public void reset() {
		super.reset();
		romlBank.read(0x8000);
		pla.setGameExrom(false, false);
	}
}