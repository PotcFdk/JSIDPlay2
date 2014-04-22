/**
 *                             Cycle Accurate 6510 Emulation
 *                             -----------------------------
 *  begin                : Thu May 11 06:22:40 BST 2000
 *  copyright            : (C) 2000 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package libsidplay.components.mos6510;

import static libsidplay.components.mos6510.IOpCode.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;

/**
 * Cycle-exact 6502/6510 emulation core.
 * 
 * Code is based on work by Simon A. White <sidplay2@yahoo.com>. Original Java
 * port by Ken Händel. Later on, it has been hacked to improve compatibility
 * with Lorenz suite on VICE's test suite.
 * 
 * @author alankila
 */
public abstract class MOS6510 {
	/** Status register interrupt bit. */
	public static final int SR_INTERRUPT = 2;

	/** Stack page location */
	protected static final byte SP_PAGE = 0x01;

	/** Logger for MOS6510 class */
	protected static final Logger MOS6510 = Logger.getLogger(MOS6510.class
			.getName());

	/**
	 * IRQ/NMI magic limit values. Need to be larger than about 0x103 << 3, but
	 * can't be min/max for Integer type.
	 */
	private static final int MAX = 65536;

	/** Our event context copy. */
	protected final EventScheduler context;

	/** RDY pin state (stop CPU on read) */
	private boolean rdy;

	/**
	 * Represents an instruction subcycle that writes. Whereas pure Runnables
	 * represent an instruction subcycle that reads. Implementation of the
	 * operation during this cpu instruction subcycle.
	 */
	private interface ProcessorCycleNoSteal extends Runnable {
	}

	/** Table of CPU opcode implementations */
	protected final Runnable[] instrTable = new Runnable[0x101 << 3];

	/** Current instruction and subcycle within instruction */
	protected int cycleCount;

	/** Data regarding current instruction */
	protected int Cycle_EffectiveAddress;
	protected int Cycle_HighByteWrongEffectiveAddress;
	protected int Register_ProgramCounter;
	protected int Cycle_Pointer;

	protected byte Cycle_Data;
	protected byte Register_Accumulator;
	protected byte Register_X;
	protected byte Register_Y;
	protected byte Register_StackPointer;

	protected boolean flagN;
	protected boolean flagC;
	protected boolean flagD;
	protected boolean flagZ;
	protected boolean flagV;
	protected boolean flagI;
	protected boolean flagU;
	protected boolean flagB;

	/** Debug info */
	protected int instrStartPC, instrOperand;

	/** IRQ asserted on CPU */
	protected boolean irqAssertedOnPin;

	/**
	 * When IRQ was triggered. -MAX means "during some previous instruction",
	 * MAX means "no IRQ"
	 */
	protected int interruptCycle;

	/** NMI requested? */
	protected boolean nmiFlag;

	/** RST requested? */
	protected boolean rstFlag;

	/**
	 * Evaluate when to execute an interrupt. Calling this method can also
	 * result in the decision that no interrupt at all needs to be scheduled.
	 */
	protected void calculateInterruptTriggerCycle() {
		/* Interrupt cycle not going to trigger? */
		if (interruptCycle == MAX) {
			if (rstFlag || nmiFlag || (!flagI && irqAssertedOnPin)) {
				interruptCycle = cycleCount;
			}
		}
	}

	/** When AEC signal is high, no stealing is possible */
	private final Event eventWithoutSteals = new Event("CPU-nosteal") {
		/** Run CPU until AEC goes low. */
		@Override
		public void event() {
			instrTable[cycleCount++].run();
			context.schedule(this, 1);
		}
	};

	/** When AEC signal is low, steals permitted */
	private final Event eventWithSteals = new Event("CPU-steal") {
		/** Stall CPU when no more cycles are executable. */
		@Override
		public void event() {
			if (instrTable[cycleCount] instanceof ProcessorCycleNoSteal) {
				instrTable[cycleCount++].run();
				context.schedule(this, 1);
			} else {
				/*
				 * Even while stalled, the CPU can still process first clock of
				 * interrupt delay, but only the first one.
				 */
				if (interruptCycle == cycleCount) {
					interruptCycle--;
				}
			}
		}
	};

	/** Opcode stringifier */
	protected IMOS6510Disassembler disassembler;

	/**
	 * Initialize CPU Emulation (Registers)
	 */
	private void Initialise() {
		// Reset stack
		Register_StackPointer = (byte) 0xff;

		// Reset Status Register
		flagU = flagB = true;
		flagN = flagC = flagD = flagV = flagZ = flagI = false;

		// Set PC to some value
		Register_ProgramCounter = 0;

		// IRQs pending check
		irqAssertedOnPin = false;
		nmiFlag = false;
		rstFlag = false;
		interruptCycle = MAX;

		// Signals
		rdy = true;
		context.schedule(eventWithoutSteals, 0, Phase.PHI2);
	}

	protected void interruptsAndNextOpcode() {
		if (cycleCount > interruptCycle + 2) {
			if (disassembler != null && MOS6510.isLoggable(Level.FINE)) {
				final long cycles = context.getTime(Phase.PHI2);
				MOS6510.fine("****************************************************");
				MOS6510.fine(String.format(" interrupt (%d)", cycles));
				MOS6510.fine("****************************************************");
				MOS6510Debug.dumpState(cycles, this);
			}

			cpuRead(Register_ProgramCounter);
			cycleCount = BRKn << 3;
			flagB = false;
			interruptCycle = MAX;
		} else {
			fetchNextOpcode();
		}
	}

	protected void fetchNextOpcode() {
		if (disassembler != null && MOS6510.isLoggable(Level.FINE)) {
			MOS6510Debug.dumpState(context.getTime(Phase.PHI2), this);
		}
		// Next line used for Debug
		instrStartPC = Register_ProgramCounter;

		cycleCount = (cpuRead(Register_ProgramCounter) & 0xff) << 3;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;

		if (!rstFlag && !nmiFlag && !(!flagI && irqAssertedOnPin)) {
			interruptCycle = MAX;
		}
		if (interruptCycle != MAX) {
			interruptCycle = -MAX;
		}
	}

	/**
	 * Fetch low address byte, increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Stack Manipulation
	 * <LI>Absolute
	 * <LI>Zero Page
	 * <LI>Zero Page Indexed
	 * <LI>Absolute Indexed
	 * <LI>Absolute Indirect
	 * </UL>
	 */
	protected void FetchLowAddr() {
		Cycle_EffectiveAddress = cpuRead(Register_ProgramCounter) & 0xff;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;
	}

	/**
	 * Read from address, add index register X to it<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Zero Page Indexed
	 * </UL>
	 */
	protected void FetchLowAddrX() {
		FetchLowAddr();
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + Register_X & 0xFF;
	}

	/**
	 * Read from address, add index register Y to it<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Zero Page Indexed
	 * </UL>
	 */
	protected void FetchLowAddrY() {
		FetchLowAddr();
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + Register_Y & 0xFF;
	}

	/**
	 * Fetch high address byte, increment PC (Absolute Addressing)<BR>
	 * 
	 * Low byte must have been obtained first!<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute
	 * </UL>
	 */
	protected void FetchHighAddr() {
		// Get the high byte of an address from memory
		Cycle_EffectiveAddress |= (cpuRead(Register_ProgramCounter) & 0xff) << 8;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;

		// Next line used for Debug
		instrOperand = Cycle_EffectiveAddress;
	}

	/**
	 * Fetch high byte of address, add index register X to low address byte,<BR>
	 * 
	 * increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute Indexed
	 * </UL>
	 */
	protected void FetchHighAddrX() {
		FetchHighAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00
				| Cycle_EffectiveAddress + Register_X & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_X & 0xff)
				& 0xffff;
	}

	/**
	 * Fetch high byte of address, add index register Y to low address byte,<BR>
	 * 
	 * increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute Indexed
	 * </UL>
	 */
	protected void FetchHighAddrY() {
		FetchHighAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00
				| Cycle_EffectiveAddress + Register_Y & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_Y & 0xff)
				& 0xffff;
	}

	/**
	 * Fetch effective address low<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Indirect
	 * <LI>Indexed Indirect (pre X)
	 * <LI>Indirect indexed (post Y)
	 * </UL>
	 */
	protected void FetchLowEffAddr() {
		Cycle_EffectiveAddress = cpuRead(Cycle_Pointer) & 0xff;
	}

	/**
	 * Fetch effective address high<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Indirect
	 * <LI>Indexed Indirect (pre X)
	 * </UL>
	 */
	protected void FetchHighEffAddr() {
		Cycle_Pointer = Cycle_Pointer & 0xff00 | Cycle_Pointer + 1 & 0xff;
		Cycle_EffectiveAddress |= (cpuRead(Cycle_Pointer) & 0xff) << 8;
	}

	/**
	 * Fetch effective address high, add Y to low byte of effective address<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Indirect indexed (post Y)
	 * <UL>
	 */
	protected void FetchHighEffAddrY() {
		FetchHighEffAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00
				| Cycle_EffectiveAddress + Register_Y & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_Y & 0xff)
				& 0xffff;
	}

	/**
	 * Fetch pointer address low, increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute Indirect
	 * <LI>Indirect indexed (post Y)
	 * </UL>
	 */
	protected void FetchLowPointer() {
		Cycle_Pointer = cpuRead(Register_ProgramCounter) & 0xff;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;
		instrOperand = Cycle_Pointer;
	}

	/**
	 * Fetch pointer address high, increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute Indirect
	 * </UL>
	 */
	protected void FetchHighPointer() {
		Cycle_Pointer |= (cpuRead(Register_ProgramCounter) & 0xff) << 8;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;
		instrOperand = Cycle_Pointer;
	}

	/**
	 * Write Cycle_Data to effective address.
	 */
	protected void PutEffAddrDataByte() {
		cpuWrite(Cycle_EffectiveAddress, Cycle_Data);
	}

	/**
	 * Push Program Counter Low Byte on stack, decrement S
	 */
	protected void PushLowPC() {
		cpuWrite(SP_PAGE << 8 | Register_StackPointer & 0xff,
				(byte) (Register_ProgramCounter & 0xff));
		Register_StackPointer--;
	}

	/**
	 * Push Program Counter High Byte on stack, decrement S
	 */
	protected void PushHighPC() {
		cpuWrite(SP_PAGE << 8 | Register_StackPointer & 0xff,
				(byte) (Register_ProgramCounter >> 8));
		Register_StackPointer--;
	}

	/**
	 * Push P on stack, decrement S
	 */
	protected void PushSR() {
		cpuWrite(SP_PAGE << 8 | Register_StackPointer & 0xff,
				getStatusRegister());
		Register_StackPointer--;
	}

	/**
	 * Increment stack and pull program counter low byte from stack,
	 */
	protected void PopLowPC() {
		Register_StackPointer++;
		Cycle_EffectiveAddress = cpuRead(SP_PAGE << 8 | Register_StackPointer
				& 0xff) & 0xff;
	}

	/**
	 * Increment stack and pull program counter high byte from stack,
	 */
	protected void PopHighPC() {
		Register_StackPointer++;
		Cycle_EffectiveAddress |= (cpuRead(SP_PAGE << 8 | Register_StackPointer
				& 0xff) & 0xff) << 8;
	}

	/**
	 * increment S, Pop P off stack
	 */
	protected void PopSR() {
		// Get status register off stack
		Register_StackPointer++;
		setStatusRegister(cpuRead(SP_PAGE << 8 | Register_StackPointer & 0xff));
		flagB = flagU = true;
	}

	/**
	 * Acquire the value of V flag.
	 * 
	 * @return The V flag value.
	 */
	public boolean getFlagV() {
		return flagV;
	}

	/**
	 * Set the value of V flag (often related to the SO pin)
	 * 
	 * @param flag
	 *            new V flag state
	 */
	public void setFlagV(boolean flag) {
		flagV = flag;
	}

	/** BCD adding */
	protected void doADC() {
		final int C = flagC ? 1 : 0;
		final int A = Register_Accumulator & 0xff;
		final int s = Cycle_Data & 0xff;
		final int regAC2 = A + s + C;

		if (flagD) {
			// BCD mode
			int lo = (A & 0x0f) + (s & 0x0f) + C;
			int hi = (A & 0xf0) + (s & 0xf0);
			if (lo > 0x09) {
				lo += 0x06;
			}
			if (lo > 0x0f) {
				hi += 0x10;
			}

			flagZ = (regAC2 & 0xff) == 0;
			flagN = (hi & 0x80) != 0;
			setFlagV(((hi ^ A) & 0x80) != 0 && ((A ^ s) & 0x80) == 0);
			if (hi > 0x90) {
				hi += 0x60;
			}

			flagC = hi > 0xff;
			Register_Accumulator = (byte) (hi & 0xf0 | lo & 0x0f);
		} else {
			// Binary mode
			flagC = regAC2 > 0xff;
			setFlagV(((regAC2 ^ A) & 0x80) != 0 && ((A ^ s) & 0x80) == 0);
			setFlagsNZ(Register_Accumulator = (byte) regAC2);
		}
	}

	/** BCD subtracting */
	protected void doSBC() {
		final int C = flagC ? 0 : 1;
		final int A = Register_Accumulator & 0xff;
		final int s = Cycle_Data & 0xff;
		final int regAC2 = A - s - C;

		flagC = regAC2 >= 0;
		setFlagV(((regAC2 ^ A) & 0x80) != 0 && ((A ^ s) & 0x80) != 0);
		setFlagsNZ((byte) regAC2);

		if (flagD) {
			// BCD mode
			int lo = (A & 0x0f) - (s & 0x0f) - C;
			int hi = (A & 0xf0) - (s & 0xf0);
			if ((lo & 0x10) != 0) {
				lo -= 0x06;
				hi -= 0x10;
			}
			if ((hi & 0x100) != 0) {
				hi -= 0x60;
			}
			Register_Accumulator = (byte) (hi & 0xf0 | lo & 0x0f);
		} else {
			// Binary mode
			Register_Accumulator = (byte) regAC2;
		}
	}

	/** Override doJSR() to catch cpu JSR instructions. */
	protected void doJSR() {
		Register_ProgramCounter = Cycle_EffectiveAddress;
	}

	private static enum AccessMode {
		WRITE, READ
	}

	/**
	 * Create new CPU emu
	 * 
	 * @param context
	 *            The Event Context
	 */
	public MOS6510(final EventScheduler context) {
		this.context = context;

		// Initialize Processor Registers
		Register_Accumulator = 0;
		Register_X = 0;
		Register_Y = 0;

		Cycle_EffectiveAddress = 0;
		Cycle_Data = 0;

		buildInstr();
	}

	private void buildInstr() {
		final Runnable wastedStealable = () -> {
		};

		/* issue throw-away read. Some people use these to ACK CIA IRQs. */
		final Runnable throwAwayReadStealable = () -> cpuRead(Cycle_HighByteWrongEffectiveAddress);

		final Runnable writeToEffectiveAddress = new ProcessorCycleNoSteal() {
			public void run() {
				PutEffAddrDataByte();
			}
		};

		// ----------------------------------------------------------------------
		// Build up the processor instruction table
		for (int i = 0; i < 0x100; i++) {
			int buildCycle = i << 3;

			/*
			 * So: what cycles are marked as stealable? Rules are:
			 * 
			 * - CPU performs either read or write at every cycle. Reads are
			 * always stealable. Writes are rare.
			 * 
			 * - Every instruction begins with a sequence of reads. Writes, if
			 * any, are at the end for most instructions.
			 */

			AccessMode access = AccessMode.WRITE;
			boolean legalMode = true;
			boolean legalInstr = true;

			switch (i) {
			// Accumulator or Implied addressing
			case ASLn:
			case CLCn:
			case CLDn:
			case CLIn:
			case CLVn:
			case DEXn:
			case DEYn:
			case INXn:
			case INYn:
			case LSRn:
			case NOPn:
			case NOPn_1:
			case NOPn_2:
			case NOPn_3:
			case NOPn_4:
			case NOPn_5:
			case NOPn_6:
			case PHAn:
			case PHPn:
			case PLAn:
			case PLPn:
			case ROLn:
			case RORn:
			case SECn:
			case SEDn:
			case SEIn:
			case TAXn:
			case TAYn:
			case TSXn:
			case TXAn:
			case TXSn:
			case TYAn:
				/* read the next opcode byte from memory (and throw it away) */
				instrTable[buildCycle++] = () -> cpuRead(Register_ProgramCounter);
				break;

			// Immediate and Relative Addressing Mode Handler
			case ADCb:
			case ANDb:
			case ANCb:
			case ANCb_1:
			case ANEb:
			case ASRb:
			case ARRb:
			case BCCr:
			case BCSr:
			case BEQr:
			case BMIr:
			case BNEr:
			case BPLr:
			case BRKn:
			case BVCr:
			case BVSr:
			case CMPb:
			case CPXb:
			case CPYb:
			case EORb:
			case LDAb:
			case LDXb:
			case LDYb:
			case LXAb:
			case NOPb:
			case NOPb_1:
			case NOPb_2:
			case NOPb_3:
			case NOPb_4:
			case ORAb:
			case RTIn:
			case RTSn:
			case SBCb:
			case SBCb_1:
			case SBXb:
				instrTable[buildCycle++] = () -> {
					Cycle_Data = cpuRead(Register_ProgramCounter);
					if (flagB) {
						Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;
					}
				};
				break;

			// Zero Page Addressing Mode Handler - Read & RMW
			case ADCz:
			case ANDz:
			case BITz:
			case CMPz:
			case CPXz:
			case CPYz:
			case EORz:
			case LAXz:
			case LDAz:
			case LDXz:
			case LDYz:
			case ORAz:
			case NOPz:
			case NOPz_1:
			case NOPz_2:
			case SBCz:
			case ASLz:
			case DCPz:
			case DECz:
			case INCz:
			case ISBz:
			case LSRz:
			case ROLz:
			case RORz:
			case SREz:
			case SLOz:
			case RLAz:
			case RRAz:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case SAXz:
			case STAz:
			case STXz:
			case STYz:
				instrTable[buildCycle++] = () -> FetchLowAddr();
				break;

			// Zero Page with X and Y Offset Addressing Mode Handler
			// these issue extra reads on the 0 page, but we don't care about it
			// because there are no detectable effects from them. These reads
			// occur during the "wasted" cycle.
			case ADCzx:
			case ANDzx:
			case CMPzx:
			case EORzx:
			case LDAzx:
			case LDYzx:
			case NOPzx:
			case NOPzx_1:
			case NOPzx_2:
			case NOPzx_3:
			case NOPzx_4:
			case NOPzx_5:
			case ORAzx:
			case SBCzx:
			case ASLzx:
			case DCPzx:
			case DECzx:
			case INCzx:
			case ISBzx:
			case LSRzx:
			case RLAzx:
			case ROLzx:
			case RORzx:
			case RRAzx:
			case SLOzx:
			case SREzx:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case STAzx:
			case STYzx:
				instrTable[buildCycle++] = () -> FetchLowAddrX();

				// operates on 0 page in read mode. Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;
				break;

			// Zero Page with Y Offset Addressing Mode Handler
			case LDXzy:
			case LAXzy:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case STXzy:
			case SAXzy:
				instrTable[buildCycle++] = () -> FetchLowAddrY();

				// operates on 0 page in read mode. Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;
				break;

			// Absolute Addressing Mode Handler
			case ADCa:
			case ANDa:
			case BITa:
			case CMPa:
			case CPXa:
			case CPYa:
			case EORa:
			case LAXa:
			case LDAa:
			case LDXa:
			case LDYa:
			case NOPa:
			case ORAa:
			case SBCa:
			case ASLa:
			case DCPa:
			case DECa:
			case INCa:
			case ISBa:
			case LSRa:
			case ROLa:
			case RORa:
			case SLOa:
			case SREa:
			case RLAa:
			case RRAa:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case JMPw:
			case SAXa:
			case STAa:
			case STXa:
			case STYa:
				instrTable[buildCycle++] = () -> FetchLowAddr();

				instrTable[buildCycle++] = () -> FetchHighAddr();
				break;

			case JSRw:
				instrTable[buildCycle++] = () -> FetchLowAddr();
				break;

			// Absolute With X Offset Addressing Mode Handler (Read)
			case ADCax:
			case ANDax:
			case CMPax:
			case EORax:
			case LDAax:
			case LDYax:
			case NOPax:
			case NOPax_1:
			case NOPax_2:
			case NOPax_3:
			case NOPax_4:
			case NOPax_5:
			case ORAax:
			case SBCax:
				access = AccessMode.READ;

				instrTable[buildCycle++] = () -> FetchLowAddr();
				instrTable[buildCycle++] = () -> {
					FetchHighAddrX();
					// Handle page boundary crossing
					if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
						cycleCount++;
					}
				};

				// this cycle is skipped if the address is already correct.
				// otherwise, it will be read and ignored.
				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			// Absolute X (RMW; no page crossing handled, always reads before
			// writing)
			case ASLax:
			case DCPax:
			case DECax:
			case INCax:
			case ISBax:
			case LSRax:
			case RLAax:
			case ROLax:
			case RORax:
			case RRAax:
			case SLOax:
			case SREax:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case SHYax:
			case STAax:
				instrTable[buildCycle++] = () -> FetchLowAddr();

				instrTable[buildCycle++] = () -> FetchHighAddrX();

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			// Absolute With Y Offset Addressing Mode Handler (Read)
			case ADCay:
			case ANDay:
			case CMPay:
			case EORay:
			case LASay:
			case LAXay:
			case LDAay:
			case LDXay:
			case ORAay:
			case SBCay:
				access = AccessMode.READ;

				instrTable[buildCycle++] = () -> FetchLowAddr();

				instrTable[buildCycle++] = () -> {
					FetchHighAddrY();
					if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
						cycleCount++;
					}
				};

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			// Absolute Y (No page crossing handled)
			case DCPay:
			case ISBay:
			case RLAay:
			case RRAay:
			case SLOay:
			case SREay:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case SHAay:
			case SHSay:
			case SHXay:
			case STAay:
				instrTable[buildCycle++] = () -> FetchLowAddr();

				instrTable[buildCycle++] = () -> FetchHighAddrY();

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			// Absolute Indirect Addressing Mode Handler
			case JMPi:
				instrTable[buildCycle++] = () -> FetchLowPointer();

				instrTable[buildCycle++] = () -> FetchHighPointer();

				instrTable[buildCycle++] = () -> FetchLowEffAddr();

				instrTable[buildCycle++] = () -> FetchHighEffAddr();
				break;

			// Indexed with X Preinc Addressing Mode Handler
			case ADCix:
			case ANDix:
			case CMPix:
			case EORix:
			case LAXix:
			case LDAix:
			case ORAix:
			case SBCix:
			case DCPix:
			case ISBix:
			case SLOix:
			case SREix:
			case RLAix:
			case RRAix:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case SAXix:
			case STAix:
				instrTable[buildCycle++] = () -> FetchLowPointer();

				instrTable[buildCycle++] = () -> Cycle_Pointer = Cycle_Pointer
						+ Register_X & 0xFF;

				instrTable[buildCycle++] = () -> FetchLowEffAddr();

				instrTable[buildCycle++] = () -> FetchHighEffAddr();
				break;

			// Indexed with Y Postinc Addressing Mode Handler (Read)
			case ADCiy:
			case ANDiy:
			case CMPiy:
			case EORiy:
			case LAXiy:
			case LDAiy:
			case ORAiy:
			case SBCiy:
				access = AccessMode.READ;

				instrTable[buildCycle++] = () -> FetchLowPointer();

				instrTable[buildCycle++] = () -> FetchLowEffAddr();

				instrTable[buildCycle++] = () -> {
					FetchHighEffAddrY();
					if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
						cycleCount++;
					}
				};

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			// Indexed Y (No page crossing handled)
			case DCPiy:
			case ISBiy:
			case RLAiy:
			case RRAiy:
			case SLOiy:
			case SREiy:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case SHAiy:
			case STAiy:
				instrTable[buildCycle++] = () -> FetchLowPointer();

				instrTable[buildCycle++] = () -> FetchLowEffAddr();

				instrTable[buildCycle++] = () -> FetchHighEffAddrY();

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;

			default:
				legalMode = false;
				break;
			}

			if (access == AccessMode.READ) {
				instrTable[buildCycle++] = () -> Cycle_Data = cpuRead(Cycle_EffectiveAddress);
			}

			// ---------------------------------------------------------------------------------------
			// Addressing Modes Finished, other cycles are instruction
			// dependent
			switch (i) {
			case ADCz:
			case ADCzx:
			case ADCa:
			case ADCax:
			case ADCay:
			case ADCix:
			case ADCiy:
			case ADCb:
				instrTable[buildCycle++] = () -> {
					doADC();
					interruptsAndNextOpcode();
				};
				break;

			case ANCb:
			case ANCb_1:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator &= Cycle_Data);
					flagC = flagN;
					interruptsAndNextOpcode();
				};
				break;

			case ANDz:
			case ANDzx:
			case ANDa:
			case ANDax:
			case ANDay:
			case ANDix:
			case ANDiy:
			case ANDb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator &= Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case ANEb: // Also known as XAA
				instrTable[buildCycle++] = () -> {
					/**
					 * The result of the ANE opcode is A = ((A | CONST) & X &
					 * IMM), with CONST apparently being both chip- and
					 * temperature dependent.
					 * 
					 * The commonly used value for CONST in various documents is
					 * 0xee, which is however not to be taken for granted (as it
					 * is unstable). see here: http://visual6502
					 * .org/wiki/index.php?title=6502_Opcode_8B_(XAA,_ANE)
					 * 
					 * as seen in the list, there are several possible values,
					 * and its origin is still kinda unknown. instead of the
					 * commonly used 0xee we use 0xff here, since this will make
					 * the only known occurance of this opcode in actual code
					 * work. see here: https://sourceforge
					 * .net/tracker/?func=detail&aid=2110948
					 * &group_id=223021&atid=1057617
					 * 
					 * FIXME: in the unlikely event that other code surfaces
					 * that depends on another CONST value, it probably has to
					 * be made configureable somehow if no value can be found
					 * that works for both.
					 */
					setFlagsNZ(Register_Accumulator = (byte) ((Register_Accumulator | 0xff)
							& Register_X & Cycle_Data));
					interruptsAndNextOpcode();
				};
				break;

			case ARRb:
				instrTable[buildCycle++] = () -> {
					final int data = Cycle_Data & Register_Accumulator & 0xff;
					Register_Accumulator = (byte) (data >> 1);
					if (flagC) {
						Register_Accumulator |= 0x80;
					}

					if (flagD) {
						flagN = flagC;
						flagZ = Register_Accumulator == 0;
						setFlagV(((data ^ Register_Accumulator) & 0x40) != 0);

						if ((data & 0x0f) + (data & 0x01) > 5) {
							Register_Accumulator = (byte) (Register_Accumulator & 0xf0 | Register_Accumulator + 6 & 0x0f);
						}
						flagC = (data + (data & 0x10) & 0x1f0) > 0x50;
						if (flagC) {
							Register_Accumulator = (byte) (Register_Accumulator + 0x60 & 0xff);
						}
					} else {
						setFlagsNZ(Register_Accumulator);
						flagC = (Register_Accumulator & 0x40) != 0;
						setFlagV((Register_Accumulator & 0x40 ^ (Register_Accumulator & 0x20) << 1) != 0);
					}
					interruptsAndNextOpcode();
				};
				break;

			case ASLn:
				instrTable[buildCycle++] = () -> {
					flagC = Register_Accumulator < 0;
					setFlagsNZ(Register_Accumulator <<= 1);
					interruptsAndNextOpcode();
				};
				break;

			case ASLz:
			case ASLzx:
			case ASLa:
			case ASLax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						flagC = Cycle_Data < 0;
						setFlagsNZ(Cycle_Data <<= 1);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case ASRb: // Also known as ALR
				instrTable[buildCycle++] = () -> {
					Register_Accumulator &= Cycle_Data;
					flagC = (Register_Accumulator & 0x01) != 0;
					Register_Accumulator >>= 1;
					Register_Accumulator &= 0x7f;
					setFlagsNZ(Register_Accumulator);
					interruptsAndNextOpcode();
				};
				break;

			case BCCr:
			case BCSr:
			case BEQr:
			case BMIr:
			case BNEr:
			case BPLr:
			case BVCr:
			case BVSr: {
				final int _i = i;
				instrTable[buildCycle++] = () -> {
					final boolean condition;
					switch (_i) {
					case BCSr:
						condition = flagC;
						break;
					case BCCr:
						condition = !flagC;
						break;
					case BEQr:
						condition = flagZ;
						break;
					case BNEr:
						condition = !flagZ;
						break;
					case BMIr:
						condition = flagN;
						break;
					case BPLr:
						condition = !flagN;
						break;
					case BVCr:
						condition = !getFlagV();
						break;
					case BVSr:
						condition = getFlagV();
						break;
					default:
						throw new RuntimeException("non-branch opcode: " + _i);
					}

					/*
					 * 2 cycles spent before arriving here. spend 0 - 2 cycles
					 * here; - condition false: Continue immediately to
					 * FetchNextInstr (return true).
					 * 
					 * Otherwise read the byte following the opcode (which is
					 * already scheduled to occur on this cycle). This effort is
					 * wasted. Then calculate address of the branch target. If
					 * branch is on same page, then continue at that insn on
					 * next cycle (this delays IRQs by 1 clock for some reason,
					 * allegedly).
					 * 
					 * If the branch is on different memory page, issue a
					 * spurious read with wrong high byte before continuing at
					 * the correct address.
					 */
					if (condition) {
						/* issue the spurious read for next insn here. */
						cpuRead(Register_ProgramCounter);

						Cycle_HighByteWrongEffectiveAddress = Register_ProgramCounter
								& 0xff00
								| Register_ProgramCounter
								+ Cycle_Data
								& 0xff;
						Cycle_EffectiveAddress = Register_ProgramCounter
								+ Cycle_Data & 0xffff;
						if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
							cycleCount += 1;
							/*
							 * Hack: delay the interrupt past this instruction.
							 */
							if (interruptCycle >> 3 == cycleCount >> 3) {
								interruptCycle += 2;
							}
						}
						Register_ProgramCounter = Cycle_EffectiveAddress;
					} else {
						/*
						 * branch not taken: skip the following spurious read
						 * insn and go to FetchNextInstr immediately.
						 */
						interruptsAndNextOpcode();
					}
				};

				instrTable[buildCycle++] = throwAwayReadStealable;
				break;
			}

			case BITz:
			case BITa:
				instrTable[buildCycle++] = () -> {
					flagZ = (Register_Accumulator & Cycle_Data) == 0;
					flagN = Cycle_Data < 0;
					setFlagV((Cycle_Data & 0x40) != 0);
					interruptsAndNextOpcode();
				};
				break;

			case BRKn:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PushHighPC();
					}
				};

				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PushLowPC();
						if (rstFlag) {
							/* rst = %10x */
							Cycle_EffectiveAddress = 0xfffc;
						} else if (nmiFlag) {
							/* nmi = %01x */
							Cycle_EffectiveAddress = 0xfffa;
						} else {
							/* irq = %11x */
							Cycle_EffectiveAddress = 0xfffe;
						}
						rstFlag = false;
						nmiFlag = false;
						calculateInterruptTriggerCycle();
					}
				};

				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PushSR();
						flagB = true;
						flagI = true;
					}
				};

				instrTable[buildCycle++] = () -> Register_ProgramCounter = cpuRead(Cycle_EffectiveAddress) & 0xff;

				instrTable[buildCycle++] = () -> Register_ProgramCounter |= (cpuRead(Cycle_EffectiveAddress + 1) & 0xff) << 8;

				instrTable[buildCycle++] = () -> fetchNextOpcode();
				break;

			case CLCn:
				instrTable[buildCycle++] = () -> {
					flagC = false;
					interruptsAndNextOpcode();
				};
				break;

			case CLDn:
				instrTable[buildCycle++] = () -> {
					flagD = false;
					interruptsAndNextOpcode();
				};
				break;

			case CLIn:
				instrTable[buildCycle++] = () -> {
					flagI = false;
					calculateInterruptTriggerCycle();
					interruptsAndNextOpcode();
				};
				break;

			case CLVn:
				instrTable[buildCycle++] = () -> {
					setFlagV(false);
					interruptsAndNextOpcode();
				};
				break;

			case CMPz:
			case CMPzx:
			case CMPa:
			case CMPax:
			case CMPay:
			case CMPix:
			case CMPiy:
			case CMPb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ((byte) (Register_Accumulator - Cycle_Data));
					flagC = (Register_Accumulator & 0xff) >= (Cycle_Data & 0xff);
					interruptsAndNextOpcode();
				};
				break;

			case CPXz:
			case CPXa:
			case CPXb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ((byte) (Register_X - Cycle_Data));
					flagC = (Register_X & 0xff) >= (Cycle_Data & 0xff);
					interruptsAndNextOpcode();
				};
				break;

			case CPYz:
			case CPYa:
			case CPYb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ((byte) (Register_Y - Cycle_Data));
					flagC = (Register_Y & 0xff) >= (Cycle_Data & 0xff);
					interruptsAndNextOpcode();
				};
				break;

			case DCPz:
			case DCPzx:
			case DCPa:
			case DCPax:
			case DCPay:
			case DCPix:
			case DCPiy: // Also known as DCM
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						Cycle_Data--;
						setFlagsNZ((byte) (Register_Accumulator - Cycle_Data));
						flagC = (Register_Accumulator & 0xff) >= (Cycle_Data & 0xff);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case DECz:
			case DECzx:
			case DECa:
			case DECax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						setFlagsNZ(--Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case DEXn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(--Register_X);
					interruptsAndNextOpcode();
				};
				break;

			case DEYn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(--Register_Y);
					interruptsAndNextOpcode();
				};
				break;

			case EORz:
			case EORzx:
			case EORa:
			case EORax:
			case EORay:
			case EORix:
			case EORiy:
			case EORb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator ^= Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case INCz:
			case INCzx:
			case INCa:
			case INCax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						setFlagsNZ(++Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case INXn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(++Register_X);
					interruptsAndNextOpcode();
				};
				break;

			case INYn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(++Register_Y);
					interruptsAndNextOpcode();
				};
				break;

			case ISBz:
			case ISBzx:
			case ISBa:
			case ISBax:
			case ISBay:
			case ISBix:
			case ISBiy: // Also known as INS
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						setFlagsNZ(++Cycle_Data);
						doSBC();
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case JSRw:
				// should read the value at current stack register.
				// Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {

					public void run() {
						PushHighPC();
					}
				};

				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PushLowPC();
					}
				};

				instrTable[buildCycle++] = () -> FetchHighAddr();
				// $FALL-THROUGH$

			case JMPw:
			case JMPi:
				instrTable[buildCycle++] = () -> {
					doJSR();
					interruptsAndNextOpcode();
				};
				break;

			case LASay:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Cycle_Data &= Register_StackPointer);
					Register_Accumulator = Cycle_Data;
					Register_X = Cycle_Data;
					Register_StackPointer = Cycle_Data;
					interruptsAndNextOpcode();
				};
				break;

			case LAXz:
			case LAXzy:
			case LAXa:
			case LAXay:
			case LAXix:
			case LAXiy:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator = Register_X = Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case LDAz:
			case LDAzx:
			case LDAa:
			case LDAax:
			case LDAay:
			case LDAix:
			case LDAiy:
			case LDAb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator = Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case LDXz:
			case LDXzy:
			case LDXa:
			case LDXay:
			case LDXb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_X = Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case LDYz:
			case LDYzx:
			case LDYa:
			case LDYax:
			case LDYb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Y = Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case LSRn:
				instrTable[buildCycle++] = () -> {
					flagC = (Register_Accumulator & 0x01) != 0;
					Register_Accumulator >>= 1;
					Register_Accumulator &= 0x7f;
					setFlagsNZ(Register_Accumulator);
					interruptsAndNextOpcode();
				};
				break;

			case LSRz:
			case LSRzx:
			case LSRa:
			case LSRax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						flagC = (Cycle_Data & 0x01) != 0;
						Cycle_Data >>= 1;
						Cycle_Data &= 0x7f;
						setFlagsNZ(Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case NOPn:
			case NOPn_1:
			case NOPn_2:
			case NOPn_3:
			case NOPn_4:
			case NOPn_5:
			case NOPn_6:
			case NOPb:
			case NOPb_1:
			case NOPb_2:
			case NOPb_3:
			case NOPb_4:
			case NOPz:
			case NOPz_1:
			case NOPz_2:
			case NOPzx:
			case NOPzx_1:
			case NOPzx_2:
			case NOPzx_3:
			case NOPzx_4:
			case NOPzx_5:
			case NOPa:
			case NOPax:
			case NOPax_1:
			case NOPax_2:
			case NOPax_3:
			case NOPax_4:
			case NOPax_5:
				// NOPb NOPz NOPzx - Also known as SKBn
				// NOPa NOPax - Also known as SKWn
				break;

			case LXAb: // Also known as OAL
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_X = Register_Accumulator = (byte) (Cycle_Data & (Register_Accumulator | 0xee)));
					interruptsAndNextOpcode();
				};
				break;

			case ORAz:
			case ORAzx:
			case ORAa:
			case ORAax:
			case ORAay:
			case ORAix:
			case ORAiy:
			case ORAb:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator |= Cycle_Data);
					interruptsAndNextOpcode();
				};
				break;

			case PHAn:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						cpuWrite(SP_PAGE << 8 | Register_StackPointer & 0xff,
								Register_Accumulator);
						Register_StackPointer--;
					}
				};
				break;

			case PHPn:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PushSR();
					}
				};
				break;

			case PLAn:
				// should read the value at current stack register.
				// Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;

				instrTable[buildCycle++] = () -> {
					Register_StackPointer++;
					setFlagsNZ(Register_Accumulator = cpuRead(SP_PAGE << 8
							| Register_StackPointer & 0xff));
				};
				break;

			case PLPn:
				// should read the value at current stack register.
				// Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;

				instrTable[buildCycle++] = () -> {
					PopSR();
					calculateInterruptTriggerCycle();
				};

				instrTable[buildCycle++] = () -> interruptsAndNextOpcode();

				break;

			case RLAz:
			case RLAzx:
			case RLAix:
			case RLAa:
			case RLAax:
			case RLAay:
			case RLAiy:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						final boolean newC = Cycle_Data < 0;
						PutEffAddrDataByte();
						Cycle_Data <<= 1;
						if (flagC) {
							Cycle_Data |= 0x01;
						}
						flagC = newC;
						setFlagsNZ(Register_Accumulator &= Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case ROLn:
				instrTable[buildCycle++] = () -> {
					final boolean newC = Register_Accumulator < 0;
					Register_Accumulator <<= 1;
					if (flagC) {
						Register_Accumulator |= 0x01;
					}
					setFlagsNZ(Register_Accumulator);
					flagC = newC;
					interruptsAndNextOpcode();
				};
				break;

			case ROLz:
			case ROLzx:
			case ROLa:
			case ROLax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						final boolean newC = Cycle_Data < 0;
						PutEffAddrDataByte();
						Cycle_Data <<= 1;
						if (flagC) {
							Cycle_Data |= 0x01;
						}
						setFlagsNZ(Cycle_Data);
						flagC = newC;
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case RORn:
				instrTable[buildCycle++] = () -> {
					final boolean newC = (Register_Accumulator & 0x01) != 0;
					Register_Accumulator >>= 1;
					if (flagC) {
						Register_Accumulator |= 0x80;
					} else {
						Register_Accumulator &= 0x7f;
					}
					setFlagsNZ(Register_Accumulator);
					flagC = newC;
					interruptsAndNextOpcode();
				};
				break;

			case RORz:
			case RORzx:
			case RORa:
			case RORax:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						final boolean newC = (Cycle_Data & 0x01) != 0;
						PutEffAddrDataByte();
						Cycle_Data >>= 1;
						if (flagC) {
							Cycle_Data |= 0x80;
						} else {
							Cycle_Data &= 0x7f;
						}
						setFlagsNZ(Cycle_Data);
						flagC = newC;
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case RRAa:
			case RRAax:
			case RRAay:
			case RRAz:
			case RRAzx:
			case RRAix:
			case RRAiy:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						final boolean newC = (Cycle_Data & 0x01) != 0;
						PutEffAddrDataByte();
						Cycle_Data >>= 1;
						if (flagC) {
							Cycle_Data |= 0x80;
						} else {
							Cycle_Data &= 0x7f;
						}
						flagC = newC;
						doADC();
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case RTIn:
				// should read the value at current stack register.
				// Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;

				instrTable[buildCycle++] = () -> {
					PopSR();
					calculateInterruptTriggerCycle();
				};

				instrTable[buildCycle++] = () -> PopLowPC();

				instrTable[buildCycle++] = () -> PopHighPC();

				instrTable[buildCycle++] = () -> {
					if (disassembler != null && MOS6510.isLoggable(Level.FINE)) {
						MOS6510.fine("****************************************************");
					}

					Register_ProgramCounter = Cycle_EffectiveAddress;
					interruptsAndNextOpcode();
				};
				break;

			case RTSn:
				// should read the value at current stack register.
				// Truly side-effect free.
				instrTable[buildCycle++] = wastedStealable;

				instrTable[buildCycle++] = () -> PopLowPC();

				instrTable[buildCycle++] = () -> PopHighPC();

				instrTable[buildCycle++] = () -> {
					cpuRead(Cycle_EffectiveAddress);
					Register_ProgramCounter = Cycle_EffectiveAddress + 1 & 0xffff;
				};
				break;

			case SAXz:
			case SAXzy:
			case SAXa:
			case SAXix: // Also known as AXS
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = (byte) (Register_Accumulator & Register_X);
						PutEffAddrDataByte();
					}
				};
				break;

			case SBCz:
			case SBCzx:
			case SBCa:
			case SBCax:
			case SBCay:
			case SBCix:
			case SBCiy:
			case SBCb:
			case SBCb_1:
				instrTable[buildCycle++] = () -> {
					doSBC();
					interruptsAndNextOpcode();
				};
				break;

			case SBXb:
				instrTable[buildCycle++] = () -> {
					final int tmp = (Register_X & Register_Accumulator & 0xff)
							- (Cycle_Data & 0xff);
					setFlagsNZ(Register_X = (byte) tmp);
					flagC = tmp >= 0;
					interruptsAndNextOpcode();
				};
				break;

			case SECn:
				instrTable[buildCycle++] = () -> {
					flagC = true;
					interruptsAndNextOpcode();
				};
				break;

			case SEDn:
				instrTable[buildCycle++] = () -> {
					flagD = true;
					interruptsAndNextOpcode();
				};
				break;

			case SEIn:
				instrTable[buildCycle++] = () -> {
					flagI = true;
					interruptsAndNextOpcode();
					if (!rstFlag && !nmiFlag && interruptCycle != MAX) {
						interruptCycle = MAX;
					}
				};
				break;

			case SHAay:
			case SHAiy: // Also known as AXA
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = (byte) (Register_X & Register_Accumulator & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8
									| Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHSay: // Also known as TAS
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Register_StackPointer = (byte) (Register_Accumulator & Register_X);
						Cycle_Data = (byte) ((Cycle_EffectiveAddress >> 8) + 1
								& Register_StackPointer & 0xff);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8
									| Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHXay: // Also known as XAS
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = (byte) (Register_X & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8
									| Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHYax: // Also known as SAY
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = (byte) (Register_Y & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8
									| Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SLOz:
			case SLOzx:
			case SLOa:
			case SLOax:
			case SLOay:
			case SLOix:
			case SLOiy: // Also known as ASO
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						flagC = Cycle_Data < 0;
						Cycle_Data <<= 1;
						setFlagsNZ(Register_Accumulator |= Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case SREz:
			case SREzx:
			case SREa:
			case SREax:
			case SREay:
			case SREix:
			case SREiy: // Also known as LSE
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						PutEffAddrDataByte();
						flagC = (Cycle_Data & 0x01) != 0;
						Cycle_Data >>= 1;
						Cycle_Data &= 0x7f;
						setFlagsNZ(Register_Accumulator ^= Cycle_Data);
					}
				};

				instrTable[buildCycle++] = writeToEffectiveAddress;
				break;

			case STAz:
			case STAzx:
			case STAa:
			case STAax:
			case STAay:
			case STAix:
			case STAiy:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = Register_Accumulator;
						PutEffAddrDataByte();
					}
				};
				break;

			case STXz:
			case STXzy:
			case STXa:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = Register_X;
						PutEffAddrDataByte();
					}
				};
				break;

			case STYz:
			case STYzx:
			case STYa:
				instrTable[buildCycle++] = new ProcessorCycleNoSteal() {
					public void run() {
						Cycle_Data = Register_Y;
						PutEffAddrDataByte();
					}
				};
				break;

			case TAXn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_X = Register_Accumulator);
					interruptsAndNextOpcode();
				};
				break;

			case TAYn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Y = Register_Accumulator);
					interruptsAndNextOpcode();
				};
				break;

			case TSXn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_X = Register_StackPointer);
					interruptsAndNextOpcode();
				};
				break;

			case TXAn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator = Register_X);
					interruptsAndNextOpcode();
				};
				break;

			case TXSn:
				instrTable[buildCycle++] = () -> {
					Register_StackPointer = Register_X;
					interruptsAndNextOpcode();
				};
				break;

			case TYAn:
				instrTable[buildCycle++] = () -> {
					setFlagsNZ(Register_Accumulator = Register_Y);
					interruptsAndNextOpcode();
				};
				break;

			default:
				legalInstr = false;
				break;
			}

			/*
			 * missing an addressing mode or implementation makes opcode
			 * invalid. Thse are normally called HLT instructions. In the
			 * hardware, the CPU state machine locks up and will never recover.
			 */
			if (!(legalMode && legalInstr)) {
				instrTable[buildCycle++] = () -> cycleCount--;
			}

			instrTable[buildCycle++] = () -> interruptsAndNextOpcode();

		}
	}

	/**
	 * Force CPU to start execution at given address
	 * 
	 * @param address
	 *            The address to start CPU execution at.
	 */
	public void forcedJump(final int address) {
		cycleCount = (NOPn << 3) + 1;
		Cycle_EffectiveAddress = Register_ProgramCounter = address;
	}

	/**
	 * Module Credits
	 * 
	 * @return credit string
	 */
	public static String credits() {
		return "MOS6510 CPU\n" + "\t(C) 2000 Simon A. White\n"
				+ "\t(C) 2008-2010 Antti S. Lankila\n";
	}

	/**
	 * Handle bus access signal. When RDY line is asserted, the CPU will pause
	 * when executing the next read operation.
	 * 
	 * @param rdy
	 *            new state for RDY signal
	 */
	public final void setRDY(final boolean rdy) {
		this.rdy = rdy;

		if (rdy) {
			context.cancel(eventWithSteals);
			context.schedule(eventWithoutSteals, 0, Phase.PHI2);
		} else {
			context.cancel(eventWithoutSteals);
			context.schedule(eventWithSteals, 0, Phase.PHI2);
		}
	}

	/**
	 * This forces the CPU to abort whatever it is doing and immediately enter
	 * the RST interrupt handling sequence. The implementation is not
	 * compatible: instructions actually get aborted mid-execution. However,
	 * there is no possible way to trigger this signal from programs, so it's
	 * OK.
	 */
	public final void triggerRST() {
		Initialise();
		cycleCount = (BRKn << 3);
		rstFlag = true;
		calculateInterruptTriggerCycle();
	}

	/**
	 * Trigger NMI interrupt on the CPU. Calling this method flags that CPU must
	 * enter the NMI routine at earliest opportunity. There is no way to cancel
	 * NMI request once given.
	 */
	public final void triggerNMI() {
		nmiFlag = true;
		calculateInterruptTriggerCycle();

		/* maybe process 1 clock of interrupt delay. */
		if (!rdy) {
			context.cancel(eventWithSteals);
			context.schedule(eventWithSteals, 0, Phase.PHI2);
		}
	}

	/** Pull IRQ line low on CPU. */
	public final void triggerIRQ() {
		irqAssertedOnPin = true;
		calculateInterruptTriggerCycle();

		/* maybe process 1 clock of interrupt delay. */
		if (!rdy && interruptCycle == cycleCount) {
			context.cancel(eventWithSteals);
			context.schedule(eventWithSteals, 0, Phase.PHI2);
		}
	}

	/** Inform CPU that IRQ is no longer pulled low. */
	public final void clearIRQ() {
		irqAssertedOnPin = false;
		calculateInterruptTriggerCycle();
	}

	/**
	 * Set N and Z flag values.
	 * 
	 * @param value
	 *            to set flags from
	 */
	protected final void setFlagsNZ(final byte value) {
		flagZ = value == 0;
		flagN = value < 0;
	}

	private byte getStatusRegister() {
		byte sr = 0;
		if (flagN) {
			sr |= 0x80;
		}
		if (getFlagV()) {
			sr |= 0x40;
		}
		if (flagU) {
			sr |= 0x20;
		}
		if (flagB) {
			sr |= 0x10;
		}
		if (flagD) {
			sr |= 0x08;
		}
		if (flagI) {
			sr |= 0x04;
		}
		if (flagZ) {
			sr |= 0x02;
		}
		if (flagC) {
			sr |= 0x01;
		}
		return sr;
	}

	private void setStatusRegister(final byte sr) {
		flagC = (sr & 0x01) != 0;
		flagZ = (sr & 0x02) != 0;
		flagI = (sr & 0x04) != 0;
		flagD = (sr & 0x08) != 0;
		flagB = (sr & 0x10) != 0;
		flagU = (sr & 0x20) != 0;
		setFlagV((sr & 0x40) != 0);
		flagN = (sr & 0x80) != 0;
	}

	/**
	 * Set CPU disassembler implementation.
	 * 
	 * @param disass
	 */
	public final void setDebug(final IMOS6510Disassembler disass) {
		disassembler = disass;
	}

	/**
	 * When stalled by BA but not yet tristated by AEC, the CPU generates read
	 * requests to the PLA chip. These reads likely concern whatever byte the
	 * CPU's current subcycle would need, but full emulation can be really
	 * tricky. We normally have this case only immediately after a write opcode,
	 * and thus the next read will concern the next opcode. Therefore, we fake
	 * it by reading the byte under the PC.
	 * 
	 * @return the value under PC
	 */
	public final byte getStalledOnByte() {
		return cpuRead(Register_ProgramCounter);
	}

	public final EventScheduler getEventScheduler() {
		return context;
	}

	/**
	 * Get data from system environment
	 * 
	 * @param address
	 *            The address to read the data from.
	 * @return data byte CPU requested
	 */
	protected abstract byte cpuRead(int address);

	/**
	 * Write data to system environment
	 * 
	 * @param address
	 *            The system address to write the value to.
	 * @param value
	 *            The value to write to the system address.
	 */
	protected abstract void cpuWrite(int address, byte value);
}
