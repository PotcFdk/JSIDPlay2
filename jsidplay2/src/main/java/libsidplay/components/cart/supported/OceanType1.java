package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * <PRE>
 *     "Ocean Type 1" Cartridge
 *
 *     - 32KiB, 128KiB, 256KiB or 512KiB sizes (4, 16, 32 or 64 banks of 8KiB)
 *     - ROM is always mapped in at $8000-$9FFF.
 *
 *     -   The 32KiB type of cart has 4 banks of 8KiB ($2000), banked in at $8000-$9FFF.
 *     -   The 128KiB type of cart has 16 banks of 8KiB ($2000), banked in at $8000-$9FFF.
 *     -   The 256KiB type of cart has 32 banks of 8KiB ($2000), 16 banked in at $8000-$9FFF, and 16 banked in at $A000-$BFFF.
 *     -   The 512KiB type of cart has 64 banks of 8KiB ($2000), banked in at $8000-$9FFF.
 *     -   Bank switching is done by writing to $DE00.
 *     -   The lower six bits give the bank number (ranging from 0-63). Bit 7 in this selection word is always set.
 *     
 *     Example dontdisturbme.crt
 * </PRE>
 *
 * @author Ken Händel
 *
 */
public class OceanType1 extends Cartridge {

	/**
	 * Currently active ROM bank.
	 */
	protected int currentRomBank;

	/**
	 * ROML banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romLBanks;

	public OceanType1(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romLBanks = new byte[64][0x2000];
		for (int i = 0; i < 64 && dis.available() > 0; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xb] >= (byte) 0x40
					|| (chipHeader[0xc] & 0xff) != 0x80 && (chipHeader[0xc] & 0xff) != 0xa0) {
				throw new RuntimeException("Unexpected Chip header!");
			}
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
				currentRomBank = value & 0x3f;
				pla.setGameExrom(false, (value & 0x80) != 0);
			}
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			return romLBanks[currentRomBank][address & 0x1fff];
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
	}

}
