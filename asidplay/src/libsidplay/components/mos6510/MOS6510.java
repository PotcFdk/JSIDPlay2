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
 * Code is based on work by Simon A. White <sidplay2@yahoo.com>.
 * Original Java port by Ken Händel. Later on, it has been hacked to
 * improve compatibility with Lorenz suite on VICE's test suite.
 * 
 * @author alankila
 */
public class MOS6510 {
	protected final CPUEnvironment env;

	// Interrupts
	public static final int SR_INTERRUPT = 2;
	//public static final int SR_BREAK = 4;
	//public static final int SR_UNUSED = 5;

	// Stack Address
	private static final byte SP_PAGE = 0x01;

	//
	// External signals
	//

	/**
	 * Address Controller, blocks reads
	 */
	private boolean aec;

	protected static final Logger MOS6510 = Logger.getLogger(MOS6510.class.getName());

	protected final EventScheduler eventContext;

	protected abstract class ProcessorCycle {
		abstract void invoke();

		boolean nosteal;
	}

	protected final ProcessorCycle[][] instrTable = new ProcessorCycle[0x100][8];

	protected final ProcessorCycle[][] interruptTable = new ProcessorCycle[3][8];

	protected ProcessorCycle[] instrCurrent;

	protected int cycleCount;

	//
	// Pointers to the current instruction cycle
	//

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

	// Only for Debug:
	protected int instrStartPC;
	protected int Instr_Operand;

	//
	// Interrupts
	//

	protected final class Interrupts {
		/** How many chips are asserting IRQ line */
		protected int irqs;

		/** IRQ requested */
		protected boolean irqFlag;

		/** When IRQ can trigger earliest */
		protected int irqClk;

		/** Number of sources pulling NMI line. */
		protected int nmis;

		/** NMI positive edge sent by CIA2? */
		protected boolean nmiFlag;

		/** When NMI can trigger earliest */
		protected int nmiClk;

		///** RST requested? */
		//boolean rstFlag;
	}

	protected final Interrupts interrupts = new Interrupts();

	/** When AEC signal is high, no stealing is possible */
	private final Event eventWithoutSteals = new Event("CPU-nosteal") {
		/** Run CPU until AEC goes low. */
		@Override
		public void event() {
			instrCurrent[cycleCount++].invoke();
			eventContext.schedule(this, 1);
		}
	};

	/** When AEC signal is low, steals permitted */
	private final Event eventWithSteals = new Event("CPU-steal") {
		/** Stall CPU when no more cycles are executable. */
		@Override
		public void event() {
			if (instrCurrent[cycleCount].nosteal) {
				instrCurrent[cycleCount++].invoke();
				eventContext.schedule(this, 1);
			} else {
				/* Even while stalled, the CPU can still process first clock of
				 * interrupt delay, but only the first one. However, IRQ may be
				 * modified by CLI and SEI, and are specially handled below. */
				if (interrupts.nmiClk == cycleCount) {
					interrupts.nmiClk --;
				}

				if (cycleCount == 0) {
					/* If stalled on first cycle of CLIn, consume IRQ delay
					 * normally and thus allow IRQ to trigger on the next
					 * instruction. */
					if (instrCurrent == instrTable[CLIn]) {
						instrCurrent = instrTable[NOPn];
						flagI = false;
						interrupts.irqClk = -1;
						return;
					}

					/* If stalled on first cycle of SEIn, don't consume IRQ delay.
					 * This has the effect of causing the interrupt to be skipped
					 * if it arrives during the CPU being stalled on this cycle. */
					if (instrCurrent == instrTable[SEIn]) {
						return;
					}
				}

				/* no special conditions matched */
				if (interrupts.irqClk == cycleCount) {
					interrupts.irqClk --;
				}
			}
		}
	};

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
		interrupts.irqs = 0;
		interrupts.irqFlag = false;
		interrupts.nmis = 0;
		interrupts.nmiFlag = false;
		interrupts.nmiClk = -1;
		interrupts.irqClk = -1;

		// Signals
		aec = true;
		eventContext.schedule(eventWithoutSteals, 0, Phase.PHI2);
	}

	void IRQRequest() {
		PushSR(false);
		flagI = true;
	}

	void IRQLoRequest() {
		Register_ProgramCounter = env.cpuReadMemory(0xFFFE) & 0xff;
	}

	void IRQHiRequest() {
		Register_ProgramCounter |= (env.cpuReadMemory(0xFFFF) & 0xff) << 8;
	}

	void interruptsAndNextOpcode() {
		final int offset;
		//if (interrupts.rstFlag) {
		//	offset = oRST;
		//} else
		if (interrupts.nmiFlag && cycleCount > interrupts.nmiClk + 2) {
			interrupts.nmiFlag = false;
			offset = oNMI;
		} else if (!flagI && interrupts.irqFlag && cycleCount > interrupts.irqClk + 2) {
			offset = oIRQ;
		} else {
			FetchOpcode();
			return;
		}

		instrCurrent = interruptTable[offset];
		cycleCount = 0;
		instrCurrent[cycleCount++].invoke();
	}

	//
	// Declare Instruction Routines
	//

	//
	// Common Instruction Addressing Routines
	// Addressing operations as described in 64doc by John West and
	// Marko Makela
	//

	/**
	 * Fetch opcode, increment PC<BR>
	 * 
	 * Addressing Modes: All
	 */
	void FetchOpcode() {
		// Next line used for Debug
		instrStartPC = Register_ProgramCounter;
		
		interrupts.irqFlag = interrupts.irqs != 0;
		instrCurrent = instrTable[env.cpuReadMemory(Register_ProgramCounter) & 0xff];
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;
		interrupts.irqClk = -1;
		interrupts.nmiClk = -1;
		cycleCount = 0;
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
	void FetchLowAddr() {
		Cycle_EffectiveAddress = env.cpuReadMemory(Register_ProgramCounter) & 0xff;
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
	void FetchLowAddrX() {
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
	void FetchLowAddrY() {
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
	void FetchHighAddr() {
		// Get the high byte of an address from memory
		Cycle_EffectiveAddress |= (env.cpuReadMemory(Register_ProgramCounter) & 0xff) << 8;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;

		// Next line used for Debug
		Instr_Operand = Cycle_EffectiveAddress;
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
	void FetchHighAddrX() {
		FetchHighAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00 | Cycle_EffectiveAddress + Register_X & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_X & 0xff) & 0xffff;
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
	void FetchHighAddrY() {
		FetchHighAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00 | Cycle_EffectiveAddress + Register_Y & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_Y & 0xff) & 0xffff;
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
	void FetchLowEffAddr() {
		Cycle_EffectiveAddress = env.cpuReadMemory(Cycle_Pointer) & 0xff;
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
	void FetchHighEffAddr() {
		Cycle_Pointer = Cycle_Pointer & 0xff00 | Cycle_Pointer + 1 & 0xff;
		Cycle_EffectiveAddress |= (env.cpuReadMemory(Cycle_Pointer) & 0xff) << 8;
	}

	/**
	 * Fetch effective address high, add Y to low byte of effective address<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Indirect indexed (post Y)
	 * <UL>
	 */
	void FetchHighEffAddrY() {
		FetchHighEffAddr();
		Cycle_HighByteWrongEffectiveAddress = Cycle_EffectiveAddress & 0xff00 | Cycle_EffectiveAddress + Register_Y & 0xff;
		Cycle_EffectiveAddress = Cycle_EffectiveAddress + (Register_Y & 0xff) & 0xffff;
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
	void FetchLowPointer() {
		Cycle_Pointer = env.cpuReadMemory(Register_ProgramCounter) & 0xff;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;

		// Next line used for Debug
		Instr_Operand = Cycle_Pointer;
	}

	/**
	 * Fetch pointer address high, increment PC<BR>
	 * 
	 * Addressing Modes:
	 * <UL>
	 * <LI>Absolute Indirect
	 * </UL>
	 */
	void FetchHighPointer() {
		Cycle_Pointer |= (env.cpuReadMemory(Register_ProgramCounter) & 0xff) << 8;
		Register_ProgramCounter = Register_ProgramCounter + 1 & 0xffff;

		// Next line used for Debug
		Instr_Operand = Cycle_Pointer;
	}

	void PutEffAddrDataByte() {
		env.cpuWriteMemory(Cycle_EffectiveAddress, Cycle_Data);
	}

	/**
	 * Push Program Counter Low Byte on stack, decrement S
	 */
	void PushLowPC() {
		env.cpuWriteMemory(SP_PAGE << 8 | Register_StackPointer & 0xff, (byte) (Register_ProgramCounter & 0xff));
		Register_StackPointer--;
	}

	/**
	 * Push Program Counter High Byte on stack, decrement S
	 */
	void PushHighPC() {
		env.cpuWriteMemory(SP_PAGE << 8 | Register_StackPointer & 0xff, (byte) (Register_ProgramCounter >> 8));
		Register_StackPointer--;
	}

	/**
	 * Push P on stack, decrement S
	 * 
	 * @param newFlagB
	 *            new value for B flag to set on CPU and write to RAM
	 */
	void PushSR(final boolean newFlagB) {
		flagB = newFlagB;
		env.cpuWriteMemory(SP_PAGE << 8 | Register_StackPointer & 0xff, getStatusRegister());
		Register_StackPointer--;
	}

	/**
	 * Increment stack and pull program counter low byte from stack,
	 */
	void PopLowPC() {
		Register_StackPointer++;
		Cycle_EffectiveAddress = env.cpuReadMemory(SP_PAGE << 8 | Register_StackPointer & 0xff) & 0xff;
	}

	/**
	 * Increment stack and pull program counter high byte from stack,
	 */
	void PopHighPC() {
		Register_StackPointer++;
		Cycle_EffectiveAddress |= (env.cpuReadMemory(SP_PAGE << 8 | Register_StackPointer & 0xff) & 0xff) << 8;
	}

	/**
	 * increment S, Pop P off stack
	 */
	void PopSR() {
		// Get status register off stack
		Register_StackPointer++;
		setStatusRegister(env.cpuReadMemory(SP_PAGE << 8 | Register_StackPointer & 0xff));
		flagB = flagU = true;
	}

	protected boolean getFlagV() {
		return flagV;
	}

	protected void setFlagV(boolean b) {
		flagV = b;
	}
	
	//
	// Generic Binary Coded Decimal Correction
	//
	void Perform_ADC() {
		final int /* uint */C = flagC ? 1 : 0;
		final int /* uint */A = Register_Accumulator & 0xff;
		final int /* uint */s = Cycle_Data & 0xff;
		final int /* uint */regAC2 = A + s + C;

		if (flagD) {
			// BCD mode
			int /* uint */lo = (A & 0x0f) + (s & 0x0f) + C;
			int /* uint */hi = (A & 0xf0) + (s & 0xf0);
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

	void Perform_SBC() {
		final int /* uint */C = flagC ? 0 : 1;
		final int /* uint */A = Register_Accumulator & 0xff;
		final int /* uint */s = Cycle_Data & 0xff;
		final int /* uint */regAC2 = A - s - C;

		flagC = regAC2 >= 0;
		setFlagV(((regAC2 ^ A) & 0x80) != 0 && ((A ^ s) & 0x80) != 0);
		setFlagsNZ((byte) regAC2);

		if (flagD) {
			// BCD mode
			int /* uint */lo = (A & 0x0f) - (s & 0x0f) - C;
			int /* uint */hi = (A & 0xf0) - (s & 0xf0);
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

	private enum AccessMode {
		WRITE, READ
	}

	/**
	 * Create new CPU emu
	 * 
	 * @param context
	 *            The Event Context
	 * @param environment
	 *            C64 environment instance
	 */
	public MOS6510(final EventScheduler context, final CPUEnvironment env) {
		eventContext = context;
		this.env = env;

		// Initialize Processor Registers
		Register_Accumulator = 0;
		Register_X = 0;
		Register_Y = 0;

		Cycle_EffectiveAddress = 0;
		Cycle_Data = 0;

		buildInstr();
	}

	private void buildInstr() {
		final ProcessorCycle wastedStealable = new ProcessorCycle() {
			@Override
			public void invoke() {
			}
		};

		/* issue throw-away read. Some people use these to ACK CIA IRQs. */
		final ProcessorCycle throwAwayReadStealable = new ProcessorCycle() {
			@Override
			public void invoke() {
				env.cpuReadMemory(Cycle_HighByteWrongEffectiveAddress);
			}
		};

		// ----------------------------------------------------------------------
		// Build up the processor instruction table
		for (int i = 0; i < 0x100; i++) {
			int buildCycle = 0;

			if (MOS6510.isLoggable(Level.FINE)) {
				MOS6510.fine(String.format("Building Command %d[%02x]..", i, i));
			}

			final ProcessorCycle[] procCycle = instrTable[i];

			/*
			 * So: what cycles are marked as stealable? Rules are:
			 * 
			 * - CPU performs either read or write at every cycle. Reads are
			 *   always stealable. Writes are rare.
			 * 
			 * - Every instruction begins with a sequence of reads. Writes,
			 *   if any, are at the end for most instructions.
			 * 
			 * - Cycles that occur simultaneously with reads may or may not be
			 *   stealable. This is simply unknown. All it means is that the
			 *   internal state of the CPU may be slightly wrong while AEC is
			 *   pausing the CPU, but this should not have externally observable
			 *   consequences.
			 * 
			 * - Of the non-virtual cycles, the ones that do not put data to memory
			 *   are stealable.
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						env.cpuReadMemory(Register_ProgramCounter);
					}
				};
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Cycle_Data = env.cpuReadMemory(Register_ProgramCounter);
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddrX();
					}
				};

				// operates on 0 page in read mode. Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;
				break;

				// Zero Page with Y Offset Addressing Mode Handler
			case LDXzy:
			case LAXzy:
				access = AccessMode.READ;
				// $FALL-THROUGH$

			case STXzy:
			case SAXzy:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddrY();
					}
				};

				// operates on 0 page in read mode. Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddr();
					}
				};
				break;

			case JSRw:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddrX();
						// Handle page boundary crossing
						if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
							cycleCount++;
						}
					}
				};

				// this cycle is skipped if the address is already correct.
				// otherwise, it will be read and ignored.
				procCycle[buildCycle++] = throwAwayReadStealable;
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddrX();
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddrY();
						if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
							cycleCount++;
						}
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddrY();
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
				break;

				// Absolute Indirect Addressing Mode Handler
			case JMPi:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowPointer();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighPointer();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowEffAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighEffAddr();
					}
				};
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowPointer();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Cycle_Pointer = Cycle_Pointer + Register_X & 0xFF;
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowEffAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighEffAddr();
					}
				};
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowPointer();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowEffAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighEffAddrY();
						if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
							cycleCount++;
						}
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowPointer();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchLowEffAddr();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighEffAddrY();
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
				break;

			default:
				legalMode = false;
				break;
			}

			if (access == AccessMode.READ) {
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Cycle_Data = env.cpuReadMemory(Cycle_EffectiveAddress);
					}
				};
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Perform_ADC();
						interruptsAndNextOpcode();
					}
				};
				break;

			case ANCb:
			case ANCb_1:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator &= Cycle_Data);
						flagC = flagN;
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator &= Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case ANEb: // Also known as XAA
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator = (byte) ((Register_Accumulator | 0xee) & Register_X & Cycle_Data));
						interruptsAndNextOpcode();
					}
				};
				break;

			case ARRb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
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
					}
				};
				break;

			case ASLn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagC = Register_Accumulator < 0;
						setFlagsNZ(Register_Accumulator <<= 1);
						interruptsAndNextOpcode();
					}
				};
				break;

			case ASLz:
			case ASLzx:
			case ASLa:
			case ASLax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						flagC = Cycle_Data < 0;
						setFlagsNZ(Cycle_Data <<= 1);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case ASRb: // Also known as ALR
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_Accumulator &= Cycle_Data;
						flagC = (Register_Accumulator & 0x01) != 0;
						Register_Accumulator >>= 1;
						Register_Accumulator &= 0x7f;
						setFlagsNZ(Register_Accumulator);
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
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
						 * 2 cycles spent before arriving here. spend 0 - 2 cycles here;
						 * - condition false: Continue immediately to FetchNextInstr (return true).
						 * 
						 * Otherwise read the byte following the opcode (which is already scheduled to occur on this cycle).
						 * This effort is wasted. Then calculate address of the branch target. If branch is on same page,
						 * then continue at that insn on next cycle (this delays IRQs by 1 clock for some reason, allegedly).
						 * 
						 * If the branch is on different memory page, issue a spurious read with wrong high byte before
						 * continuing at the correct address.
						 */
						if (condition) {
							/* issue the spurious read for next insn here. */
							env.cpuReadMemory(Register_ProgramCounter);

							Cycle_HighByteWrongEffectiveAddress = Register_ProgramCounter & 0xff00 | Register_ProgramCounter + Cycle_Data & 0xff;
							Cycle_EffectiveAddress = Register_ProgramCounter + Cycle_Data & 0xffff;
							if (Cycle_EffectiveAddress == Cycle_HighByteWrongEffectiveAddress) {
								cycleCount += 1;
								interrupts.irqClk += 2;
								interrupts.nmiClk += 2;
							}
							Register_ProgramCounter = Cycle_EffectiveAddress;
						} else {
							/* branch not taken: skip the following spurious read insn and go to FetchNextInstr immediately. */
							interruptsAndNextOpcode();
						}
					}
				};

				procCycle[buildCycle++] = throwAwayReadStealable;
				break;
			}

			case BITz:
			case BITa:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagZ = (Register_Accumulator & Cycle_Data) == 0;
						flagN = Cycle_Data < 0;
						setFlagV((Cycle_Data & 0x40) != 0);
						interruptsAndNextOpcode();
					}
				};
				break;

			case BRKn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushHighPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushLowPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushSR(true);
						flagI = true;

						/* check if we need to transform BRK into NMI.
						 * c64doc says "If the interrupt arrives before the
						 * flag-setting cycle" */
						if (interrupts.nmiFlag && interrupts.nmiClk < 3) {
							instrCurrent = interruptTable[oNMI];
							interrupts.nmiFlag = false;
							cycleCount = 5;
						}
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						IRQLoRequest();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						IRQHiRequest();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchOpcode();
					}
				};
				break;

			case CLCn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagC = false;
						interruptsAndNextOpcode();
					}
				};
				break;

			case CLDn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagD = false;
						interruptsAndNextOpcode();
					}
				};
				break;

			case CLIn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						if (flagI) {
							interrupts.irqClk = cycleCount;
						}
						flagI = false;
						interruptsAndNextOpcode();
					}
				};
				break;

			case CLVn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagV(false);
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ((byte) (Register_Accumulator - Cycle_Data));
						flagC = (Register_Accumulator & 0xff) >= (Cycle_Data & 0xff);
						interruptsAndNextOpcode();
					}
				};
				break;

			case CPXz:
			case CPXa:
			case CPXb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ((byte) (Register_X - Cycle_Data));
						flagC = (Register_X & 0xff) >= (Cycle_Data & 0xff);
						interruptsAndNextOpcode();
					}
				};
				break;

			case CPYz:
			case CPYa:
			case CPYb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ((byte) (Register_Y - Cycle_Data));
						flagC = (Register_Y & 0xff) >= (Cycle_Data & 0xff);
						interruptsAndNextOpcode();
					}
				};
				break;

			case DCPz:
			case DCPzx:
			case DCPa:
			case DCPax:
			case DCPay:
			case DCPix:
			case DCPiy: // Also known as DCM
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						Cycle_Data--;
						setFlagsNZ((byte) (Register_Accumulator - Cycle_Data));
						flagC = (Register_Accumulator & 0xff) >= (Cycle_Data & 0xff);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case DECz:
			case DECzx:
			case DECa:
			case DECax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						setFlagsNZ(--Cycle_Data);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case DEXn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(--Register_X);
						interruptsAndNextOpcode();
					}
				};
				break;

			case DEYn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(--Register_Y);
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator ^= Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case INCz:
			case INCzx:
			case INCa:
			case INCax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						setFlagsNZ(++Cycle_Data);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case INXn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(++Register_X);
						interruptsAndNextOpcode();
					}
				};
				break;

			case INYn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(++Register_Y);
						interruptsAndNextOpcode();
					}
				};
				break;

			case ISBz:
			case ISBzx:
			case ISBa:
			case ISBax:
			case ISBay:
			case ISBix:
			case ISBiy: // Also known as INS
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						setFlagsNZ(++Cycle_Data);
						Perform_SBC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case JSRw:
				// should read the value at current stack register.
				// Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushHighPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushLowPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						FetchHighAddr();
					}
				};
				// $FALL-THROUGH$

			case JMPw:
			case JMPi:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter = Cycle_EffectiveAddress;
						interruptsAndNextOpcode();
					}
				};
				break;

			case LASay:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Cycle_Data &= Register_StackPointer);
						Register_Accumulator = Cycle_Data;
						Register_X = Cycle_Data;
						Register_StackPointer = Cycle_Data;
						interruptsAndNextOpcode();
					}
				};
				break;

			case LAXz:
			case LAXzy:
			case LAXa:
			case LAXay:
			case LAXix:
			case LAXiy:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator = Register_X = Cycle_Data);
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator = Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case LDXz:
			case LDXzy:
			case LDXa:
			case LDXay:
			case LDXb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_X = Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case LDYz:
			case LDYzx:
			case LDYa:
			case LDYax:
			case LDYb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Y = Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case LSRn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagC = (Register_Accumulator & 0x01) != 0;
						Register_Accumulator >>= 1;
						Register_Accumulator &= 0x7f;
						setFlagsNZ(Register_Accumulator);
						interruptsAndNextOpcode();
					}
				};
				break;

			case LSRz:
			case LSRzx:
			case LSRa:
			case LSRax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						flagC = (Cycle_Data & 0x01) != 0;
						Cycle_Data >>= 1;
							Cycle_Data &= 0x7f;
							setFlagsNZ(Cycle_Data);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_X = Register_Accumulator = (byte) (Cycle_Data & (Register_Accumulator | 0xee)));
						interruptsAndNextOpcode();
					}
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator |= Cycle_Data);
						interruptsAndNextOpcode();
					}
				};
				break;

			case PHAn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						env.cpuWriteMemory(SP_PAGE << 8 | Register_StackPointer & 0xff, Register_Accumulator);
						Register_StackPointer--;
					}
				};
				break;

			case PHPn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PushSR(true);
					}
				};
				break;

			case PLAn:
				// should read the value at current stack register.
				// Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_StackPointer++;
						setFlagsNZ(Register_Accumulator = env.cpuReadMemory(SP_PAGE << 8 | Register_StackPointer & 0xff));
					}
				};
				break;

			case PLPn:
				// should read the value at current stack register.
				// Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;

				/* XXX SR is actually read on this cycle. But it should be
				 * a side-effect free read. Therefore I have taken the liberty
				 * of arranging it later than the interrupt check on next cycle;
				 * the end result should still be identical. */
				procCycle[buildCycle++] = wastedStealable;

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						interruptsAndNextOpcode();
						PopSR();
					}
				};

				break;

			case RLAz:
			case RLAzx:
			case RLAix:
			case RLAa:
			case RLAax:
			case RLAay:
			case RLAiy:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case ROLn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						final boolean newC = Register_Accumulator < 0;
						Register_Accumulator <<= 1;
						if (flagC) {
							Register_Accumulator |= 0x01;
						}
						setFlagsNZ(Register_Accumulator);
						flagC = newC;
						interruptsAndNextOpcode();
					}
				};
				break;

			case ROLz:
			case ROLzx:
			case ROLa:
			case ROLax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case RORn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
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
					}
				};
				break;

			case RORz:
			case RORzx:
			case RORa:
			case RORax:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
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

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case RRAa:
			case RRAax:
			case RRAay:
			case RRAz:
			case RRAzx:
			case RRAix:
			case RRAiy:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						final boolean newC = (Cycle_Data & 0x01) != 0;
						PutEffAddrDataByte();
						Cycle_Data >>= 1;
						if (flagC) {
							Cycle_Data |= 0x80;
						} else {
							Cycle_Data &= 0x7f;
						}
						flagC = newC;
						Perform_ADC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case RTIn:
				// should read the value at current stack register.
				// Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						PopSR();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						PopLowPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						PopHighPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter = Cycle_EffectiveAddress;
						interruptsAndNextOpcode();
					}
				};
				break;

			case RTSn:
				// should read the value at current stack register.
				// Truly side-effect free.
				procCycle[buildCycle++] = wastedStealable;

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						PopLowPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						PopHighPC();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter = Cycle_EffectiveAddress + 1 & 0xffff;
					}
				};
				break;

			case SAXz:
			case SAXzy:
			case SAXa:
			case SAXix: // Also known as AXS
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Perform_SBC();
						interruptsAndNextOpcode();
					}
				};
				break;

			case SBXb:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						final int tmp = (Register_X & Register_Accumulator & 0xff) - (Cycle_Data & 0xff);
						setFlagsNZ(Register_X = (byte) tmp);
						flagC = tmp >= 0;
						interruptsAndNextOpcode();
					}
				};
				break;

			case SECn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagC = true;
						interruptsAndNextOpcode();
					}
				};
				break;

			case SEDn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						flagD = true;
						interruptsAndNextOpcode();
					}
				};
				break;

			case SEIn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						interruptsAndNextOpcode();
						flagI = true;
					}
				};
				break;

			case SHAay:
			case SHAiy: // Also known as AXA
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = (byte) (Register_X & Register_Accumulator & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8 | Cycle_EffectiveAddress | 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHSay: // Also known as TAS
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Register_StackPointer = (byte) (Register_Accumulator & Register_X);
						Cycle_Data = (byte) ((Cycle_EffectiveAddress >> 8) + 1 & Register_StackPointer & 0xff);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8 | Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHXay: // Also known as XAS
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = (byte) (Register_X & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8 | Cycle_EffectiveAddress & 0xff;
						}
						PutEffAddrDataByte();
					}
				};
				break;

			case SHYax: // Also known as SAY
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = (byte) (Register_Y & (Cycle_EffectiveAddress >> 8) + 1);
						if (Cycle_HighByteWrongEffectiveAddress != Cycle_EffectiveAddress) {
							Cycle_EffectiveAddress = (Cycle_Data & 0xff) << 8 | Cycle_EffectiveAddress & 0xff;
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
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						flagC = Cycle_Data < 0;
						Cycle_Data <<= 1;
						setFlagsNZ(Register_Accumulator |= Cycle_Data);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case SREz:
			case SREzx:
			case SREa:
			case SREax:
			case SREay:
			case SREix:
			case SREiy: // Also known as LSE
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
						flagC = (Cycle_Data & 0x01) != 0;
						Cycle_Data >>= 1;
						Cycle_Data &= 0x7f;
						setFlagsNZ(Register_Accumulator ^= Cycle_Data);
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						PutEffAddrDataByte();
					}
				};
				break;

			case STAz:
			case STAzx:
			case STAa:
			case STAax:
			case STAay:
			case STAix:
			case STAiy:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = Register_Accumulator;
						PutEffAddrDataByte();
					}
				};
				break;

			case STXz:
			case STXzy:
			case STXa:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = Register_X;
						PutEffAddrDataByte();
					}
				};
				break;

			case STYz:
			case STYzx:
			case STYa:
				procCycle[buildCycle++] = new ProcessorCycle() {
					{
						nosteal = true;
					}

					@Override
					public void invoke() {
						Cycle_Data = Register_Y;
						PutEffAddrDataByte();
					}
				};
				break;

			case TAXn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_X = Register_Accumulator);
						interruptsAndNextOpcode();
					}
				};
				break;

			case TAYn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Y = Register_Accumulator);
						interruptsAndNextOpcode();
					}
				};
				break;

			case TSXn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_X = Register_StackPointer);
						interruptsAndNextOpcode();
					}
				};
				break;

			case TXAn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator = Register_X);
						interruptsAndNextOpcode();
					}
				};
				break;

			case TXSn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_StackPointer = Register_X;
						interruptsAndNextOpcode();
					}
				};
				break;

			case TYAn:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						setFlagsNZ(Register_Accumulator = Register_Y);
						interruptsAndNextOpcode();
					}
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
			if (!(legalMode || legalInstr)) {
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter = Register_ProgramCounter - 1 & 0xffff;
					}
				};
			} else if (!(legalMode && legalInstr)) {
				throw new RuntimeException("Instruction " + i + " not built correctly.");
			}

			procCycle[buildCycle++] = new ProcessorCycle() {
				@Override
				public void invoke() {
					interruptsAndNextOpcode();
				}
			};

			if (MOS6510.isLoggable(Level.FINE)) {
				MOS6510.fine(".");
				MOS6510.fine(String.format("Done [%d Cycles]\n", buildCycle));
			}
		}

		// ----------------------------------------------------------------------
		// Build interrupts
		for (int i = 0; i < 3; i++) {
			int buildCycle = 0;

			if (MOS6510.isLoggable(Level.FINE)) {
				MOS6510.fine(String.format("Building Interrupt %d[%02x]..", i, i));
			}

			final ProcessorCycle[] procCycle = interruptTable[i];
			/* common interrupt handling lead-in:
			 * 2x read from PC, store pc hi, pc lo, sr to stack.
			 */

			procCycle[buildCycle ++] = procCycle[buildCycle ++] = new ProcessorCycle() {
				@Override
				void invoke() {
					env.cpuReadMemory(Register_ProgramCounter);
				}
			};

			procCycle[buildCycle++] = new ProcessorCycle() {
				{
					nosteal = true;
				}

				@Override
				public void invoke() {
					PushHighPC();
				}
			};
			procCycle[buildCycle++] = new ProcessorCycle() {
				{
					nosteal = true;
				}

				@Override
				public void invoke() {
					PushLowPC();
				}
			};

			procCycle[buildCycle++] = new ProcessorCycle() {
				{
					nosteal = true;
				}

				@Override
				public void invoke() {
					IRQRequest();
				}
			};

			switch (i) {
			case oRST:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						/* reset the values in MMU */
						env.cpuWriteMemory(0, (byte) 0x2f);
						env.cpuWriteMemory(1, (byte) 0x37);

						Register_ProgramCounter = env.cpuReadMemory(0xFFFC) & 0xff;
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter |= (env.cpuReadMemory(0xFFFD) & 0xff) << 8;
					}
				};
				break;

			case oNMI:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter = env.cpuReadMemory(0xFFFA) & 0xff;
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						Register_ProgramCounter |= (env.cpuReadMemory(0xFFFB) & 0xff) << 8;
					}
				};
				break;

			case oIRQ:
				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						IRQLoRequest();
					}
				};

				procCycle[buildCycle++] = new ProcessorCycle() {
					@Override
					public void invoke() {
						IRQHiRequest();
					}
				};
				break;
			}

			procCycle[buildCycle++] = new ProcessorCycle() {
				@Override
				public void invoke() {
					FetchOpcode();
				}
			};

			if (MOS6510.isLoggable(Level.FINE)) {
				MOS6510.fine(".");
				MOS6510.fine(String.format("Done [%d Cycles]\n", buildCycle));
			}
		}
	}

	public void forcedJump(final int address) {
		cycleCount = 1;
		instrCurrent = instrTable[NOPn];
		Cycle_EffectiveAddress = Register_ProgramCounter = address;
	}

	/**
	 * Module Credits
	 * 
	 * @return credit string
	 */
	public static String credits() {
		return "MOS6510 CPU\n"
		+ "\t(C) 2000 Simon A. White\n"
		+ "\t(C) 2008-2010 Antti S. Lankila\n";
	}
	/**
	 * Handle bus access signals
	 * 
	 * @param state
	 *            new state for AEC signals
	 */
	public final void aecSignal(final boolean newAec) {
		if (aec == newAec) {
			return;
		}

		if (aec) {
			eventContext.cancel(eventWithoutSteals);
		} else {
			eventContext.cancel(eventWithSteals);
		}

		aec = newAec;

		if (aec) {
			eventContext.schedule(eventWithoutSteals, 0, Phase.PHI2);
		} else {
			eventContext.schedule(eventWithSteals, 0, Phase.PHI2);
		}
	}

	//
	// Interrupt Routines
	//

	private static final int oRST = 0;
	private static final int oNMI = 1;
	private static final int oIRQ = 2;

	/**
	 * This forces the CPU to abort whatever it is doing and immediately
	 * enter the RST interrupt handling sequence. The implementation is
	 * not compatible: instructions actually get aborted mid-execution.
	 * However, there is no possible way to trigger this signal from
	 * programs, so it's OK.
	 */
	public final void triggerRST() {
		Initialise();
		cycleCount = 0;
		instrCurrent = interruptTable[oRST];
	}

	/**
	 * Trigger NMI interrupt on the CPU. Calling this method
	 * flags that CPU must enter the NMI routine at earliest
	 * opportunity. There is no way to cancel NMI request once
	 * given, but the NMI can't be retriggered until a
	 * clearNMI() call has been made.
	 */
	public final void triggerNMI() {
		if (interrupts.nmis == 0) {
			interrupts.nmiFlag = true;
			interrupts.nmiClk = cycleCount;
			/* maybe process 1 clock of interrupt delay. */
			if (! aec) {
				eventContext.cancel(eventWithSteals);
				eventContext.schedule(eventWithSteals, 0, Phase.PHI2);
			}
		}

		interrupts.nmis ++;
	}

	/**
	 * Remove one source of level-triggered IRQ interrupts.
	 * This call must be performed after each triggerIRQ.
	 */
	public final void clearNMI() {
		if (--interrupts.nmis < 0) {
			throw new RuntimeException("Bizarre attempt to clear untriggered NMI.");
		}
	}

	/**
	 * Trigger IRQ interrupt on the CPU. Calling this method
	 * increments the number of sources pulling IRQ line up.
	 * To remove IRQ, clearIRQ() call must be made.
	 */
	public final void triggerIRQ() {
		/* mark interrupt arrival time */
		if (interrupts.irqs == 0) {
			interrupts.irqFlag = true;
			interrupts.irqClk = cycleCount;
			/* maybe process 1 clock of interrupt delay. */
			if (! aec) {
				eventContext.cancel(eventWithSteals);
				eventContext.schedule(eventWithSteals, 0, Phase.PHI2);
			}
		}

		interrupts.irqs ++;
	}

	/**
	 * Remove one source of level-triggered IRQ interrupts.
	 * This call must be performed after each triggerIRQ.
	 */
	public final void clearIRQ() {
		if (--interrupts.irqs < 0) {
			throw new RuntimeException("Bizarre attempt to clear untriggered IRQ.");
		}
	}

	/* in principle the flags are read much less seldom than written. This suggests that
	 * one might optimize the code slighly by reducing computation in the write path.
	 * However, the difference is very minor, and this code is cleaner.
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

	public final byte getLastReadByte() {
		// XXX should probably use a side-effect free read here.
		return env.cpuReadMemory(Register_ProgramCounter);
	}
}
