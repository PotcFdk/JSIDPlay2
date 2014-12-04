package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * <PRE>
 *     "Magic Desk" Cartridge
 * 
 *     - this cart comes in 3 sizes, 32Kb, 64Kb and 128Kb.
 *     - ROM is always mapped in at $8000-$9FFF.
 * 
 *     - 1 register at io1 / de00:
 * 
 *     bit 0-5   bank number
 *     bit 7     exrom (1 = cart disabled)
 * </PRE>
 * 
 * @author Ken Händel
 *
 */
public class MagicDesk extends Cartridge {

	/**
	 * Currently active ROM bank.
	 */
	protected int currentRomBank;

	/**
	 * ROML banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romLBanks;

	public MagicDesk(final DataInputStream dis, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romLBanks = new byte[16][0x2000];
		for (int i = 0; i < 16 && dis.available() > 0; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xb] >= (byte) 0x40
					|| (chipHeader[0xc] & 0xff) != 0x80
					&& (chipHeader[0xc] & 0xff) != 0xa0)
				throw new RuntimeException("Unexpected Chip header!");
			int bank = chipHeader[0xb] & 0xff;
			dis.readFully(romLBanks[bank]);
		}
	}

	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			if (address == 0xde00) {
				currentRomBank = value & 0x1f;
				pla.setGameExrom(true, (value & 0x80) != 0);
			}
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			return romLBanks[currentRomBank][(address & 0x1fff)];
		}
	};

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public void reset() {
		super.reset();
		io1Bank.write(0xde00, (byte) 0x00);
		pla.setGameExrom(true, false);
	}

}
