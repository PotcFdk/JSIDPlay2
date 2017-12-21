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
package libsidutils.vicesync;

import java.io.IOException;

import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos6510.MOS6510;

/**
 * Alpha: Feature to compare VICE and JSIDPlay2 implementation by running both
 * and communicating via socket connection after each instruction.<BR>
 * This has been added to find differences and improve emulation quality.
 * Currently for Edge of Disgrace disk 1 hung up.
 * 
 * @author Ken Händel
 */
public class MOS6510ViceSync extends MOS6510 {

//	private final int cmpPC = 3746;
//	private final byte cmpAcc = (byte) 25;
//	private final byte cmpX = (byte) 126;
//	private final byte cmpY = (byte) 1;
//	private final byte cmpSP = (byte) 232;

//	private final int cmpPC = 0x0d99;
//	private final byte cmpAcc = (byte) 0xb7;
//	private final byte cmpX = (byte) 0x20;
//	private final byte cmpY = (byte) 0x04;
//	private final byte cmpSP = (byte) 0xe8;

//	private final int cmpPC = 0x1050;
//	private final byte cmpAcc = (byte) 0x62;
//	private final byte cmpX = (byte) 0x3d;
//	private final byte cmpY = (byte) 0x04;
//	private final byte cmpSP = (byte) 0xe2;

	private final int cmpPC = 0x1e01;
	private final byte cmpAcc = (byte) 0x00;
	private final byte cmpX = (byte) 0x78;
	private final byte cmpY = (byte) 0x04;
	private final byte cmpSP = (byte) 0xea;

//	private final int cmpPC = 0x1a07;
//	private final byte cmpAcc = (byte) 0xba;
//	private final byte cmpX = (byte) 0x1d;
//	private final byte cmpY = (byte) 0x18;
//	private final byte cmpSP = (byte) 0xed;

	private ViceSync sync;

	private long syncClk;
	private boolean sockedStarted;

	private boolean startComparison;

	public MOS6510ViceSync(final EventScheduler context) {
		super(context);
		sync = new ViceSync();
	}

	@Override
	protected void fetchNextOpcode() {
		if (!sockedStarted) {
			try {
				sync.connect(6510);
				sockedStarted = true;
				// here we tell vice when we want to start comparing the state!
				sync.send(String.format("%04X,%02X,%02X,%02X,%02X", cmpPC, cmpAcc, cmpX, cmpY, cmpSP));
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		if (sockedStarted) {
			try {
				if (!startComparison && Register_ProgramCounter == cmpPC && Register_Accumulator == cmpAcc
						&& Register_X == cmpX && Register_Y == cmpY && Register_StackPointer == cmpSP) {
					syncClk = context.getTime(Phase.PHI2);
					startComparison = true;
				}
				if (startComparison) {
					String received = sync.receive();
					ViceSync.MOS6510State viceState = sync.getState(received);
					sync.send("");

					System.out.println(viceState.toString());
					
					ViceSync.MOS6510State jsidplay2State = new ViceSync.MOS6510State(
							context.getTime(Phase.PHI2) - syncClk, Register_ProgramCounter, Register_Accumulator,
							Register_X, Register_Y, Register_StackPointer);

					System.out.println(jsidplay2State.toString());

					if (Register_ProgramCounter == 0x1e01 && (Register_Accumulator & 0xff) == 0x00
							&& (Register_X & 0xff) == 0x78 && (Register_Y & 0xff) == 0x04
							&& (Register_StackPointer & 0xff) == 0xea) {
						// pc=0d99(3481), a=b7, x=20, y=04, sp=e8
						sync.send("break");
						System.out.println("Until here we have exactly the same state!!!");
					}
					if (cycleCount > 7 && !jsidplay2State.equals(viceState)) {
						// pc=1000, a=c8, x=20, y=04, sp=e5
						System.out.println("This is the first point where they differ, do some analysis here!!!");
						System.err.println("Differs: " + jsidplay2State.equals(viceState));
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}
		super.fetchNextOpcode();
	}
}
