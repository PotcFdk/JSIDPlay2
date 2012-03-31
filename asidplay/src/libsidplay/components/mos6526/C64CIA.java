/**
 *                                     C64 CIAs
 *                                     --------
 *  begin                : Fri Apr 4 2001
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
package libsidplay.components.mos6526;

import libsidplay.common.EventScheduler;


/**
 * The CIA emulations are very generic and here we need to effectively wire them
 * into the computer (like adding a chip to a PCB).
 * 
 * @author Ken Händel
 * 
 */
public class C64CIA {
	/**
	 * CIA 1 specifics: Generates IRQs
	 */
	public static final class C64CIA1 extends MOS6526 {
		private final CIAEnvironment m_env;

		@Override
		public void interrupt(final boolean state) {
			m_env.interruptIRQ(state);
		}

		@Override
		public void write(final int addr, final byte data) {
			super.write(addr, data);

			if (addr == PRB || addr == DDRB) {
				final byte newLp = (byte) ((regs[PRB] | ~regs[DDRB]) & 0x10);
				m_env.lightpen(newLp != 0x10);
			}
		}

		public C64CIA1(final EventScheduler context, final CIAEnvironment env) {
			super(context);
			m_env = env;
		}
	}

	/**
	 * CIA 2 specifics: Generates NMIs
	 */
	public static final class C64CIA2 extends MOS6526 {
		private final CIAEnvironment m_env;

		@Override
		public void interrupt(final boolean state) {
			m_env.interruptNMI(state);
		}

		public C64CIA2(final EventScheduler context, final CIAEnvironment env) {
			super(context);
			m_env = env;
		}
	}
}
