package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * We emulate this cartridge in the ON mode.
 * 
 * Electrically, the cartridge can only switch between ultimax and no cartridge mode.
 * 
 * However, the ultimax mode is only enabled when accesses to the region 0x8000-0x9fff
 * and 0xe000-0xffff occur. This is why the other regions like IO and RAM still work.
 * 
 * @author AL
 */
public class Expert extends Cartridge {

	protected final byte[] ram;

	protected boolean ultimaxHackOn;
	
	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			ultimaxHackOn = false;
			pla.setGameExrom(true, true);
			return (byte) 0;
		}

		@Override
		public void write(int address, byte value) {
			ultimaxHackOn = false;
			pla.setGameExrom(true, true);
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			return ram[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
			ram[address & 0x1fff] = value;
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return ram[address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		}
	};

	@Override
	public void changedNMI(boolean state) {
		if (state) {
			ultimaxHackOn = true;
			pla.setGameExrom(true, true);
		}
	}

	public Expert(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		// first chip header contains ROM_L
		dis.readFully(chipHeader);

		int bankLen = (chipHeader[0xe] & 0xff) << 8;
		ram = new byte[bankLen];
		dis.readFully(ram);
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
	public void installBankHooks(Bank[] cpuReadMap, Bank[] cpuWriteMap) {
		if (! ultimaxHackOn) {
			return;
		}
		cpuReadMap[0x8] = cpuWriteMap[0x8] = romlBank;
		cpuReadMap[0x9] = cpuWriteMap[0x9] = romlBank;
		cpuReadMap[0xe] = cpuWriteMap[0xe] = romhBank;
		cpuReadMap[0xf] = cpuWriteMap[0xf] = romhBank;
	}
	
	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public void reset() {
		super.reset();
		ultimaxHackOn = false;
		pla.setGameExrom(true, true);
	}
}