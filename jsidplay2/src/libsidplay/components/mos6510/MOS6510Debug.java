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

/**
 * Cycle-exact 6502/6510 emulation core.
 * 
 * Code is based on work by Simon A. White <sidplay2@yahoo.com>.
 * Original Java port by Ken Händel. Later on, it has been hacked to
 * improve compatibility with Lorenz suite on VICE's test suite.
 * 
 * @author alankila
 */
public class MOS6510Debug {
	protected static void dumpState(long time, MOS6510 cpu) {
		final StringBuffer m_fdbg = new StringBuffer();
		m_fdbg.append(String.format(" PC  I  A  X  Y  SP  DR PR NV-BDIZC  Instruction (%d)\n", time));
		m_fdbg.append(String.format("%04x ", cpu.instrStartPC));
		m_fdbg.append(cpu.irqAssertedOnPin ? "t" : "f");
		m_fdbg.append(String.format("%02x ", cpu.Register_Accumulator & 0xff));
		m_fdbg.append(String.format("%02x ", cpu.Register_X & 0xff));
		m_fdbg.append(String.format("%02x ", cpu.Register_Y & 0xff));
		m_fdbg.append(String.format("%02x%02x ", MOS6510.SP_PAGE, cpu.Register_StackPointer & 0xff));
		m_fdbg.append(String.format("%02x ", cpu.cpuRead(0)));
		m_fdbg.append(String.format("%02x ", cpu.cpuRead(1)));

		m_fdbg.append(cpu.flagN ? "1" : "0");
		m_fdbg.append(cpu.getFlagV() ? "1" : "0");
		m_fdbg.append(cpu.flagU ? "1" : "0");
		// normally it's not possible to read the B flag except by pushing it to stack and extracting it
		// immediately afterwards, which reads 1 for B. We return the last pushed value.
		m_fdbg.append(cpu.flagB ? "1" : "0");
		m_fdbg.append(cpu.flagD ? "1" : "0");
		m_fdbg.append(cpu.flagI ? "1" : "0");
		m_fdbg.append(cpu.flagZ ? "1" : "0");
		m_fdbg.append(cpu.flagC ? "1" : "0");

		int opcode = cpu.cycleCount >> 3;
		int instrOperand = cpu.instrOperand;
		byte Cycle_Data = cpu.Cycle_Data;
		int Cycle_EffectiveAddress = cpu.Cycle_EffectiveAddress;
		
		m_fdbg.append(String.format("  %02x ", opcode));

		switch (opcode) {
		// Accumulator or Implied addressing
		case ASLn:
		case LSRn:
		case ROLn:
		case RORn:
			m_fdbg.append("      ");
			break;
			// Zero Page Addressing Mode Handler
		case ADCz:
		case ANDz:
		case ASLz:
		case BITz:
		case CMPz:
		case CPXz:
		case CPYz:
		case DCPz:
		case DECz:
		case EORz:
		case INCz:
		case ISBz:
		case LAXz:
		case LDAz:
		case LDXz:
		case LDYz:
		case LSRz:
		case NOPz:
		case NOPz_1:
		case NOPz_2:
		case ORAz:
		case ROLz:
		case RORz:
		case SAXz:
		case SBCz:
		case SREz:
		case STAz:
		case STXz:
		case STYz:
		case SLOz:
		case RLAz:
		case RRAz:
			// ASOz AXSz DCMz INSz LSEz - Optional Opcode Names
			m_fdbg.append(String.format("%02x    ", instrOperand));
			break;
			// Zero Page with X Offset Addressing Mode Handler
		case ADCzx:
		case ANDzx:
		case ASLzx:
		case CMPzx:
		case DCPzx:
		case DECzx:
		case EORzx:
		case INCzx:
		case ISBzx:
		case LDAzx:
		case LDYzx:
		case LSRzx:
		case NOPzx:
		case NOPzx_1:
		case NOPzx_2:
		case NOPzx_3:
		case NOPzx_4:
		case NOPzx_5:
		case ORAzx:
		case RLAzx:
		case ROLzx:
		case RORzx:
		case RRAzx:
		case SBCzx:
		case SLOzx:
		case SREzx:
		case STAzx:
		case STYzx:
			// ASOzx DCMzx INSzx LSEzx - Optional Opcode Names
			m_fdbg.append(String.format("%02x    ", instrOperand & 0xff));
			break;
			// Zero Page with Y Offset Addressing Mode Handler
		case LDXzy:
		case STXzy:
		case SAXzy:
		case LAXzy:
			// AXSzx - Optional Opcode Names
			m_fdbg.append(String.format("%02x    ", instrOperand));
			break;
			// Absolute Addressing Mode Handler
		case ADCa:
		case ANDa:
		case ASLa:
		case BITa:
		case CMPa:
		case CPXa:
		case CPYa:
		case DCPa:
		case DECa:
		case EORa:
		case INCa:
		case ISBa:
		case JMPw:
		case JSRw:
		case LAXa:
		case LDAa:
		case LDXa:
		case LDYa:
		case LSRa:
		case NOPa:
		case ORAa:
		case ROLa:
		case RORa:
		case SAXa:
		case SBCa:
		case SLOa:
		case SREa:
		case STAa:
		case STXa:
		case STYa:
		case RLAa:
		case RRAa:
			// ASOa AXSa DCMa INSa LSEa - Optional Opcode Names
			m_fdbg.append(String.format("%02x %02x ", instrOperand & 0xff, instrOperand >> 8));
			break;
			// Absolute With X Offset Addresing Mode Handler
		case ADCax:
		case ANDax:
		case ASLax:
		case CMPax:
		case DCPax:
		case DECax:
		case EORax:
		case INCax:
		case ISBax:
		case LDAax:
		case LDYax:
		case LSRax:
		case NOPax:
		case NOPax_1:
		case NOPax_2:
		case NOPax_3:
		case NOPax_4:
		case NOPax_5:
		case ORAax:
		case RLAax:
		case ROLax:
		case RORax:
		case RRAax:
		case SBCax:
		case SHYax:
		case SLOax:
		case SREax:
		case STAax:
			// ASOax DCMax INSax LSEax SAYax - Optional Opcode Names
			m_fdbg.append(String.format("%02x %02x ", instrOperand & 0xff, instrOperand >> 8));
			break;
			// Absolute With Y Offset Addresing Mode Handler
		case ADCay:
		case ANDay:
		case CMPay:
		case DCPay:
		case EORay:
		case ISBay:
		case LASay:
		case LAXay:
		case LDAay:
		case LDXay:
		case ORAay:
		case RLAay:
		case RRAay:
		case SBCay:
		case SHAay:
		case SHSay:
		case SHXay:
		case SLOay:
		case SREay:
		case STAay:
			// ASOay AXAay DCMay INSax LSEay TASay XASay - Optional Opcode Names
			m_fdbg.append(String.format("%02x %02x ", instrOperand & 0xff, instrOperand >> 8));
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
		case SBCb:
		case SBCb_1:
		case SBXb:
			m_fdbg.append(String.format("%02x    ", Cycle_Data & 0xff));
			break;
			// Indirect Addressing Mode Handler
		case JMPi:
			m_fdbg.append(String.format("%02x %02x ", instrOperand & 0xff, instrOperand >> 8));
			break;
			// Indexed with X Preinc Addressing Mode Handler
		case ADCix:
		case ANDix:
		case CMPix:
		case DCPix:
		case EORix:
		case ISBix:
		case LAXix:
		case LDAix:
		case ORAix:
		case SAXix:
		case SBCix:
		case SLOix:
		case SREix:
		case STAix:
		case RLAix:
		case RRAix:
			// ASOix AXSix DCMix INSix LSEix - Optional Opcode Names
			m_fdbg.append(String.format("%02x    ", instrOperand));
			break;
			// Indexed with Y Postinc Addressing Mode Handler
		case ADCiy:
		case ANDiy:
		case CMPiy:
		case DCPiy:
		case EORiy:
		case ISBiy:
		case LAXiy:
		case LDAiy:
		case ORAiy:
		case RLAiy:
		case RRAiy:
		case SBCiy:
		case SHAiy:
		case SLOiy:
		case SREiy:
		case STAiy:
			// AXAiy ASOiy LSEiy DCMiy INSiy - Optional Opcode Names
			m_fdbg.append(String.format("%02x    ", instrOperand));
			break;
		default:
			m_fdbg.append("      ");
			break;
		}

		m_fdbg.append(cpu.disassembler.disassemble(opcode, instrOperand, Cycle_EffectiveAddress));

		switch (opcode) {
		// Zero Page Addressing Mode Handler
		case ADCz:
		case ANDz:
		case ASLz:
		case BITz:
		case CMPz:
		case CPXz:
		case CPYz:
		case DCPz:
		case DECz:
		case EORz:
		case INCz:
		case ISBz:
		case LAXz:
		case LDAz:
		case LDXz:
		case LDYz:
		case LSRz:
		case ORAz:

		case ROLz:
		case RORz:
		case SBCz:
		case SREz:
		case SLOz:
		case RLAz:
		case RRAz:
			// ASOz AXSz DCMz INSz LSEz - Optional Opcode Names
			m_fdbg.append(String.format(" {%02x}", Cycle_Data));
			break;
			// Zero Page with X Offset Addressing Mode Handler
		case ADCzx:
		case ANDzx:
		case ASLzx:
		case CMPzx:
		case DCPzx:
		case DECzx:
		case EORzx:
		case INCzx:
		case ISBzx:
		case LDAzx:
		case LDYzx:
		case LSRzx:
		case ORAzx:
		case RLAzx:
		case ROLzx:
		case RORzx:
		case RRAzx:
		case SBCzx:
		case SLOzx:
		case SREzx:
			// ASOzx DCMzx INSzx LSEzx - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case STAzx:
		case STYzx:
		case NOPzx:
		case NOPzx_1:
		case NOPzx_2:
		case NOPzx_3:
		case NOPzx_4:
		case NOPzx_5:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Zero Page with Y Offset Addressing Mode Handler
		case LAXzy:
		case LDXzy:
			// AXSzx - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case STXzy:
		case SAXzy:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Absolute Addressing Mode Handler
		case ADCa:
		case ANDa:
		case ASLa:
		case BITa:
		case CMPa:
		case CPXa:
		case CPYa:
		case DCPa:
		case DECa:
		case EORa:
		case INCa:
		case ISBa:
		case LAXa:
		case LDAa:
		case LDXa:
		case LDYa:
		case LSRa:
		case ORAa:
		case ROLa:
		case RORa:
		case SBCa:
		case SLOa:
		case SREa:
		case RLAa:
		case RRAa:
			// ASOa AXSa DCMa INSa LSEa - Optional Opcode Names
			m_fdbg.append(String.format(" {%02x}", Cycle_Data));
			break;
			// Absolute With X Offset Addresing Mode Handler
		case ADCax:
		case ANDax:
		case ASLax:
		case CMPax:
		case DCPax:
		case DECax:
		case EORax:
		case INCax:
		case ISBax:
		case LDAax:
		case LDYax:
		case LSRax:
		case ORAax:
		case RLAax:
		case ROLax:
		case RORax:
		case RRAax:
		case SBCax:
		case SLOax:
		case SREax:
			// ASOax DCMax INSax LSEax SAYax - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case SHYax:
		case STAax:
		case NOPax:
		case NOPax_1:
		case NOPax_2:
		case NOPax_3:
		case NOPax_4:
		case NOPax_5:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Absolute With Y Offset Addresing Mode Handler
		case ADCay:
		case ANDay:
		case CMPay:
		case DCPay:
		case EORay:
		case ISBay:
		case LASay:
		case LAXay:
		case LDAay:
		case LDXay:
		case ORAay:
		case RLAay:
		case RRAay:
		case SBCay:
		case SHSay:
		case SLOay:
		case SREay:
			// ASOay AXAay DCMay INSax LSEay TASay XASay - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case SHAay:
		case SHXay:
		case STAay:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Relative Addressing Mode Handler
		case BCCr:
		case BCSr:
		case BEQr:
		case BMIr:
		case BNEr:
		case BPLr:
		case BVCr:
		case BVSr:
			// this is already part of CPUParser.getDebug()
			break;

			// Indirect Addressing Mode Handler
		case JMPi:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Indexed with X Preinc Addressing Mode Handler
		case ADCix:
		case ANDix:
		case CMPix:
		case DCPix:
		case EORix:
		case ISBix:
		case LAXix:
		case LDAix:
		case ORAix:
		case SBCix:
		case SLOix:
		case SREix:
		case RLAix:
		case RRAix:
			// ASOix AXSix DCMix INSix LSEix - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case SAXix:
		case STAix:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

			// Indexed with Y Postinc Addressing Mode Handler
		case ADCiy:
		case ANDiy:
		case CMPiy:
		case DCPiy:
		case EORiy:
		case ISBiy:
		case LAXiy:
		case LDAiy:
		case ORAiy:
		case RLAiy:
		case RRAiy:
		case SBCiy:
		case SLOiy:
		case SREiy:
			// AXAiy ASOiy LSEiy DCMiy INSiy - Optional Opcode Names
			m_fdbg.append(String.format(" [%04x]{%02x}", Cycle_EffectiveAddress, Cycle_Data));
			break;
		case SHAiy:
		case STAiy:
			m_fdbg.append(String.format(" [%04x]", Cycle_EffectiveAddress));
			break;

		default:
			break;
		}

		MOS6510.MOS6510.info(m_fdbg.toString());
	}
}
