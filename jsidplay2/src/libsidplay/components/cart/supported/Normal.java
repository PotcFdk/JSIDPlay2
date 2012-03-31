package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

public class Normal extends Cartridge {
	protected byte[] roml, romh;

	private boolean exrom, game;
	
	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			if (roml != null) {
				return roml[address & 0x1fff];
			} else {
				return pla.getDisconnectedBusBank().read(address);
			}
		}
	};
	
	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			if (romh != null) {
				return romh[address & 0x1fff];
			} else {
				return pla.getDisconnectedBusBank().read(address);
			}
		}
	};
	
	public Normal(DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		byte[] header = new byte[0x10];
		
		dis.readFully(header);

		if (header[0xe] == 0x10) {
			/* This is ultimax cartridge. I'm expecting it to only have a single
			 * ROM file, which I'll mirror twice into the ROMH region. */
			byte[] data = new byte[0x1000];
			dis.readFully(data);
			romh = new byte[0x2000];
			System.arraycopy(data, 0, romh, 0,      0x1000);
			System.arraycopy(data, 0, romh, 0x1000, 0x1000);
			exrom = false;
			game = true;
		} else if (header[0xe] == 0x20) {
			roml = new byte[0x2000];
			dis.readFully(roml);
			game = true;
			exrom = false;
		} else if (header[0xe] == 0x40) {
			roml = new byte[0x2000];
			romh = new byte[0x2000];
			dis.readFully(roml);
			dis.readFully(romh);
			game = false;
			exrom = false;
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
		pla.setGameExrom(game, exrom);
	}
}
