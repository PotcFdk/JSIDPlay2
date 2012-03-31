package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * <p>
 * 16 MB RAM Expansion Unit emulation. No REU of this size
 * was ever produced, but 1541U and VICE nevertheless can emulate one.
 * <p>
 * The real REUs that were manufactured had the following sizes:
 * <ul>
 * <li>1700: 128 kB
 * <li>1750: 512 kB
 * <li>1764: 256 kB
 * <li>1750 XL: 2 MB
 * </ul>
 * The 1750 REU uses 256 kb chips. The 1750 XL is a mod on 1750, where
 * a custom circuit detects the next 2 higher bits of the bank register
 * (bits 3 and 4 above 0-2), and uses these to do bank-switching between
 * four 512 kB banks. Consequently, it was not possible to read those bits
 * from the BANK register, and overflow of the REU RAM address can't switch
 * to next 512 kB bank, but loops back to the start of each 512 kB window.
 * <p>
 * We are not emulating the wrap-around features of the REU chips yet. We
 * are emulating a fictional REU with full 8-bit wide BANK register. We
 * do emulate the verify-related bugs.
 * <p>
 * REU images are pure RAM dumps with no internal structure.
 * 
 * @author AL
 */
public class REU extends Cartridge {
	private enum Command {
		TO_REU, FROM_REU, SWAP, VERIFY
	}

	private static final int REGISTER_STATUS = 0x0;
	private static final int REGISTER_COMMAND = 0x1;
	private static final int REGISTER_BASEADDR_LOW = 0x2;
	private static final int REGISTER_BASEADDR_HIGH = 0x3;
	private static final int REGISTER_RAMADDR_LOW = 0x4;
	private static final int REGISTER_RAMADDR_HIGH = 0x5;
	private static final int REGISTER_BANK = 0x6;
	private static final int REGISTER_BLOCKLEN_LOW = 0x7;
	private static final int REGISTER_BLOCKLEN_HIGH = 0x8;
	private static final int REGISTER_INTERRUPT = 0x9;
	private static final int REGISTER_ADDR_CONTROL = 0xa;
	
	private static final int REGISTER_INTERRUPT_UNUSED = 0x1f;
	private static final int REGISTER_ADDR_CONTROL_UNUSED = 0x3f;
	/**
	 * AND mask depends of the RAM expansion size.
	 */
	protected static int wrapAround;

	/** REU currently actively performing DMA */
	protected boolean dmaActive;
	
	/** REU RAM region (max. 16 MB) */
	protected byte[] ram;

	/** Current state of the bus available signal */
	protected boolean ba;

	/** Is FF00 REU DMA trigger enabled */
	protected boolean ff00;

	/** Misc. REU register */
	protected byte status, command, interrupt, addrControl;
	
	/** DMA operation C64 address */
	protected int baseAddr, shadowBaseAddr;

	/** DMA operation REU address */
	protected int ramAddr, shadowRamAddr;

	/** DMA operation length */
	protected short dmaLen, shadowDmaLen;

	/** Currently active command */
	protected Command reuOperation;
	
	protected REU(PLA pla) {
		super(pla);
	}
	
	/**
	 * REU interrupt enable/disable
	 */
	protected void interrupt() {
		int irq = 0x60 & interrupt & status;
		if (irq != 0 && (interrupt & 0x80) != 0) {
			status |= 0x80;
		}
		setIRQ((status & 0x80) != 0);
	}

	public static final Cartridge readImage(PLA pla, InputStream is, int sizeKB)
	throws IOException {
		assert sizeKB == 128 || sizeKB == 512 || sizeKB == 256
				|| sizeKB == 2 << 10 || sizeKB == 16 << 10;

		REU reu = new REU(pla);

		if (sizeKB == 0) {
			// empty file means maximum size!
			sizeKB = 16 << 10;
		}
		wrapAround = (sizeKB << 10) - 1;
		reu.ram = new byte[sizeKB << 10];
		Arrays.fill(reu.ram, (byte) 0);
		if (is != null) {
			DataInputStream dis = new DataInputStream(is);
			try {
				dis.readFully(reu.ram);
			}
			catch (EOFException e) {
				/* no problem, we'll just keep the rest uninitialized... */
			}
		}
		return reu;
	}
	
	private final Bank io2Bank = new Bank() {
		@Override
		public byte read(int address) {
			if (dmaActive) {
				return pla.getDisconnectedBusBank().read(address);
			}

			address &= 0x1f;
			
			switch (address) {
			case REGISTER_STATUS: {
				byte value = status;
				status &= 0x1f;
				interrupt();
				return value;
			}
			
			case REGISTER_COMMAND:
				return command;
			
			case REGISTER_BASEADDR_HIGH:
				return (byte) (baseAddr >> 8);

			case REGISTER_BASEADDR_LOW:
				return (byte) (baseAddr);
				
			case REGISTER_RAMADDR_HIGH:
				return (byte) (ramAddr >> 8);

			case REGISTER_RAMADDR_LOW:
				return (byte) (ramAddr);
				
			case REGISTER_BANK:
				return (byte) (ramAddr >> 16);

			case REGISTER_BLOCKLEN_HIGH:
				return (byte) (dmaLen >> 8);

			case REGISTER_BLOCKLEN_LOW:
				return (byte) (dmaLen);

			case REGISTER_INTERRUPT: {
				byte value = interrupt;
				value |= REGISTER_INTERRUPT_UNUSED;
				return value;
			}

			case REGISTER_ADDR_CONTROL: {
				byte value = addrControl;
				value |= REGISTER_ADDR_CONTROL_UNUSED;
				return value;
			}
			
			default:
				return (byte) 0xff;
			}
		}
		
		@Override
		public void write(int address, byte value) {
			if (dmaActive) {
				return;
			}
			
			address &= 0x1f;

			/*
			 * How does baseAddress etc. work? The internal register within
			 * the REU is 16 bits wide. Every CPU write to bank, base and
			 * ram addresses are backed by a shadow register that holds the
			 * reference value that is used during the AUTOLOAD mode of
			 * command.
			 * 
			 * Whenever a write occurs, it goes into the shadow first, and is
			 * then copied to the register used for DMA operations. However,
			 * this means that writing low 8 bits will copy the high 8 bits
			 * as well, and vice versa.
			 * 
			 * The bank is a separate register, and the REU tracks overflows
			 * of the ramAddr and increments bank on overflow. I have chosen
			 * to store bank on the ramAddr high bits. That is why the bank
			 * is modified differently from the other modifications.
			 */
			switch (address) {
			case REGISTER_STATUS: {
				return;
			}
			
			case REGISTER_COMMAND:
				command = value;
				reuOperation = Command.values()[value & 3];
				/* prime ff00 when execute & ff00 */
				ff00 = (value & 0x90) == 0x80;
				if ((value & 0x90) == 0x90) {
					/* execute & no ff00 */
					beginDma();
				}
				return;
	
			case REGISTER_BASEADDR_HIGH:
				shadowBaseAddr &= 0x00ff;
				shadowBaseAddr |= (value & 0xff) << 8;
				baseAddr = shadowBaseAddr;
				return;

			case REGISTER_BASEADDR_LOW:
				shadowBaseAddr &= 0xff00;
				shadowBaseAddr |= (value & 0xff);
				baseAddr = shadowBaseAddr;
				return;
				
			case REGISTER_RAMADDR_HIGH:
				shadowRamAddr &= 0xff00ff;
				shadowRamAddr |= (value & 0xff) << 8;
				/* copy bits, keep Bank */
				ramAddr &= wrapAround & 0xff0000;
				ramAddr |= shadowRamAddr & 0xffff;
				return;

			case REGISTER_RAMADDR_LOW:
				shadowRamAddr &= 0xffff00;
				shadowRamAddr |= (value & 0xff);
				/* copy bits, keep Bank */
				ramAddr &= wrapAround & 0xff0000;
				ramAddr |= shadowRamAddr & 0xffff;
				return;
				
			case REGISTER_BANK:
				/* Modify bank and shadow copy of bank,
				 * kept on the high bits of ramAddr, which
				 * is a deviation from hardware's behavior. */
				ramAddr &= 0xffff;
				ramAddr |= (value & 0xff) << 16;
				shadowRamAddr &= 0xffff;
				shadowRamAddr |= (value & 0xff) << 16;
				return;

			case REGISTER_BLOCKLEN_HIGH:
				shadowDmaLen &= 0x00ff;
				shadowDmaLen |= (value & 0xff) << 8;
				dmaLen = shadowDmaLen;
				return;

			case REGISTER_BLOCKLEN_LOW:
				shadowDmaLen &= 0xff00;
				shadowDmaLen |= (value & 0xff);
				dmaLen = shadowDmaLen;
				return;

			case REGISTER_INTERRUPT:
				interrupt = value;
				interrupt();
				return;

			case REGISTER_ADDR_CONTROL:
				addrControl = value;
				return;
			}
		}
	};
	
	@Override
	public Bank getIO2() {
		return io2Bank;
	}
	
	@Override
	public void reset() {
		super.reset();

		status = 0x10;
		command = 0x10;
		baseAddr = shadowBaseAddr = 0;
		ramAddr = shadowRamAddr = 0;
		dmaLen = shadowDmaLen = (short) 0xffff;
		interrupt = 0;
		addrControl = 0;
		
		dmaActive = false;
	}

	@Override
	public void changedBA(boolean state) {
		ba = state;	
		if (! dmaActive) {
			return;
		}
		
		if (ba) {
			pla.getCPU().getEventScheduler().schedule(dmaEvent, 0, Event.Phase.PHI2);
		} else {
			pla.getCPU().getEventScheduler().cancel(dmaEvent);
		}
	}
	
	@Override
	public void installBankHooks(Bank[] cpuReadMap, Bank[] cpuWriteMap) {
		final Bank rom = cpuWriteMap[0xf];
		cpuWriteMap[0xf] = new Bank() {
			@Override
			public void write(int address, byte value) {
				rom.write(address, value);
				/* when primed, execute DMA on write to 0xff00 */
				if (ff00 && address == 0xff00) {
					ff00 = false;
					beginDma();
				}
			}
		};
	}
	
	protected void beginDma() {
		pla.getCPU().getEventScheduler().schedule(dmaBeginEvent, 0, Event.Phase.PHI1);
	}

	private final Event dmaBeginEvent = new Event("REU DMA Begin") {
		@Override
		public void event() {
			pla.setDMA(true);
			dmaActive = true;

			dmaEvent.reset();

			/* Schedule DMA operation to begin on the next PHI2 */
			if (ba) {
				pla.getCPU().getEventScheduler().schedule(dmaEvent, 0, Event.Phase.PHI2);
			}
		}
	};	

	protected class DMAEvent extends Event {
		/** Command.SWAP in "read c64" phase */
		private boolean swapReadPhase;
		
		/** Command.SWAP read data during "read c64" phase */
		private byte swapData;

		/** Verify Error exit states */
		private int verifyError;
		
		protected DMAEvent() {
			super("REU DMA Active");
		}
		
		protected void reset() {
			swapReadPhase = true;
			verifyError = 0;
		}

		protected void finish() {
			/* Execute off, FF00 off */
			command &= 0x7f;
			command |= 0x10;
			
			/* Autoload? */
			if ((command & 0x20) != 0) {
				baseAddr = shadowBaseAddr;
				ramAddr = wrapAround & shadowRamAddr;
				dmaLen = shadowDmaLen;
			}
			
			pla.getCPU().getEventScheduler().schedule(dmaEndEvent, 0, Event.Phase.PHI1);
		}
		
		@Override
		public void event() {
			int oldVerifyError = verifyError;
			
			switch (reuOperation) {
			case TO_REU:
				ram[ramAddr] = pla.cpuRead(baseAddr);
				break;
				
			case FROM_REU:
				pla.cpuWrite(baseAddr, ram[ramAddr]);
				break;
				
			case SWAP:
				if (swapReadPhase) {
					swapReadPhase = false;
					swapData = pla.cpuRead(baseAddr);
					pla.getCPU().getEventScheduler().schedule(this, 1, Event.Phase.PHI2);
					return;
				} else {
					swapReadPhase = true;
					pla.cpuWrite(baseAddr, ram[ramAddr]);
					ram[ramAddr] = swapData;
				}
				break;
					
			case VERIFY:
				if (pla.cpuRead(baseAddr) != ram[ramAddr]) {
					status |= 0x20;
					interrupt();
					verifyError ++;
				}
				break;
			}

			/* INC stops on first verify error, but the last byte does get compared */
			if (oldVerifyError == 0) {
				/* Fixed C64? */
				if ((addrControl & 0x80) == 0) {
					baseAddr = baseAddr + 1 & 0xffff;
				}
				/* Fixed REU? */
				if ((addrControl & 0x40) == 0) {
					ramAddr = ramAddr + 1 & wrapAround;
				}
			}

			/* REU reschedules or exits */
			if (dmaLen != 1) {
				dmaLen --;
				/* If no verify errors yet, continue */
				if (oldVerifyError == 0) {
					pla.getCPU().getEventScheduler().schedule(this, 1, Event.Phase.PHI2);
				} else {
					finish();
				}
			} else {
				/* Set the block finished flag only if the last 2 bytes weren't
				 * both bad */
				if (verifyError != 2) {
					status |= 0x40;
					interrupt();
				}
				finish();
			}
		}
	}
	
	protected final DMAEvent dmaEvent = new DMAEvent();
	
	protected final Event dmaEndEvent = new Event("REU DMA End") {
		@Override
		public void event() {
			dmaActive = false;
			pla.setDMA(false);
		}
	};
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + " (" + (ram.length >> 10) + " KB)";
	}

}
