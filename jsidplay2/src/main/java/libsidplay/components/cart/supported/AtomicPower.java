package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * This cartridge has a special A000 RAM mode, enabled by setting
 * the Action Replay control byte to 0x22 (ignoring the bank select lines).
 * 
 * I believe this is implemented using a bus snooping technique
 * where the cart takes note of R/W and address signals and in
 * that mode updates the RAM whenever a write to 0xa000-0xbfff occurs.
 *
 * It is unknown if the data also goes to the system RAM, or whether the
 * cart enables Ultimax mode to avoid touching the system RAM data. The
 * current implementation updates both the cart RAM and the system RAM.
 * 
 * @author Antti Lankila
 */
public class AtomicPower extends Cartridge {
	/* Atomic Power RAM hack. */
	protected boolean exportA000Ram;

	protected boolean exportRam;

	protected final byte[] ram = new byte[0x2000];

	protected boolean freezed = false;
	
	/**
	 * Currently active ROML bank.
	 */
	protected int currentRomBank;
	
	/**
	 * ROML banks 0..3 (each of size 0x2000).
	 */
	protected final byte[][] romLBanks;
	
	public AtomicPower(final DataInputStream dis, final PLA pla)
			throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romLBanks = new byte[4][0x2000];
		for (int i = 0; i < 4; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xc] != (byte) 0xa0 && chipHeader[0xe] != 0x40
					&& (chipHeader[0xb] & 0xff) > 3)
				throw new RuntimeException("Unexpected Chip header!");
			int bank = chipHeader[0xb] & 0xff;
			dis.readFully(romLBanks[bank]);
		}
	}
	
	protected final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			currentRomBank = ((value >> 3) & 3);

			if ((value & 0xe7) == 0x22) {
				value ^= 3;
		        exportA000Ram = true;
		        exportRam = false;
		    } else {
		    	exportA000Ram = false;
		    	exportRam = (value & 0x20) != 0;

		    	/* release freeze */
		    	if ((value & 0x40) != 0 && freezed) {
		    		pla.setNMI(false);
		    	}
		    }
			
			// exrom && !game == ultimax
		    pla.setGameExrom(true, true, (value & 1) == 0, (value & 2) == 2);
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
		    if (exportRam)
		        return ram[address & 0x1fff];

		    return romLBanks[currentRomBank][address & 0x1fff];
		}

		@Override
		public void write(int address, byte value) {
		    if (exportRam)
		        ram[address & 0x1fff] = value;
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
		    if (exportA000Ram) {
		    	return ram[address & 0x1fff];
		    }
		    
			return romLBanks[currentRomBank][address & 0x1fff];
		}
	};

	@Override
	public void installBankHooks(final Bank[] cpuReadMap, final Bank[] cpuWriteMap) {
		if (! exportA000Ram) {
			return;
		}
		for (int i = 10; i < 12; i ++) {
			final Bank origBank = cpuWriteMap[i];
			Bank writeHook = new Bank() {
				@Override
				public void write(int address, byte value) {
					ram[address & 0x1fff] = value;
					origBank.write(address, value);
				}
			};
			cpuWriteMap[i] = writeHook;
		}
	}
	
	@Override
	public Bank getRomh() {
		return romhBank;
	}

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public Bank getIO2() {
		return romlBank;
	}
	
	@Override
	public void reset() {
		super.reset();
		io1Bank.write(0xde00, (byte) 0);
		Arrays.fill(ram, (byte) 0);
	}
	
	private final Event newCartRomConfig = new Event("AtomicPower freeze") {
		@Override
		public void event() {
			io1Bank.write(0xde00, (byte) 3);
			freezed = true;
		}
	};
	
	@Override
	public void doFreeze() {
		pla.setNMI(true);
		pla.getCPU().getEventScheduler().schedule(newCartRomConfig, 3, Event.Phase.PHI1);
	}
}
