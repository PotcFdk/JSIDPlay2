/**
 * viacore.c - Core functions for VIA emulation.
 *
 * Written by
 *  André Fachat <fachat@physik.tu-chemnitz.de>
 *  Andreas Boose <viceteam@t-online.de>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.c1541;

import java.util.Arrays;

import libsidplay.common.Event;

/**
 * <PRE>
 * 24jan97 a.fachat
 * new interrupt handling, hopefully according to the specs now.
 * All interrupts (note: not timer events (i.e. alarms) are put
 * into one interrupt flag.
 * if an interrupt condition changes, the function (i.e. cpp macro)
 * update_myviairq() id called, that checks the IRQ line state.
 * This is now possible, as ettore has decoupled A_* alarm events
 * from interrupts for performance reasons.
 * 
 * A new function for signaling rising/falling edges on the
 * control lines is introduced:
 *      myvia_signal(VIA_SIG_[CA1|CA2|CB1|CB2], VIA_SIG_[RISE|FALL])
 * which signals the corresponding edge to the VIA. The constants
 * are defined in via.h.
 * 
 * Except for shift register and input latching everything should be ok now.
 * </PRE>
 */
public abstract class VIACore {
	/* MOS 6522 registers */
	/**
	 * Port B.
	 */
	protected static final int VIA_PRB = 0;
	/**
	 * Port A.
	 */
	protected static final int VIA_PRA = 1;
	/**
	 * Data direction register for port B.
	 */
	protected static final int VIA_DDRB = 2;
	/**
	 * Data direction register for port A.
	 */
	protected static final int VIA_DDRA = 3;

	/**
	 * Timer 1 count low.
	 */
	protected static final int VIA_T1CL = 4;
	/**
	 * Timer 1 count high.
	 */
	protected static final int VIA_T1CH = 5;
	/**
	 * Timer 1 latch low.
	 */
	protected static final int VIA_T1LL = 6;
	/**
	 * Timer 1 latch high.
	 */
	protected static final int VIA_T1LH = 7;
	/**
	 * Timer 2 count low - read only.
	 */
	protected static final int VIA_T2CL = 8;
	/**
	 * Timer 2 latch low - write only.
	 */
	protected static final int VIA_T2LL = 8;
	/**
	 * Timer 2 latch/count high.
	 */
	protected static final int VIA_T2CH = 9;

	/**
	 * Serial port shift register.
	 */
	protected static final int VIA_SR = 10;
	/**
	 * Auxiliary control register.
	 */
	protected static final int VIA_ACR = 11;
	/**
	 * Peripheral control register.
	 */
	protected static final int VIA_PCR = 12;

	/**
	 * Interrupt flag register.
	 */
	protected static final int VIA_IFR = 13;
	/**
	 * Interrupt control register.
	 */
	protected static final int VIA_IER = 14;
	/**
	 * Port A with no handshake.
	 */
	protected static final int VIA_PRA_NHS = 15;

	/* Interrupt Masks */
	/* MOS 6522 */
	/**
	 * Control Bit.
	 */
	protected static final int VIA_IM_IRQ = 128;
	/**
	 * Timer 1 underflow.
	 */
	protected static final int VIA_IM_T1 = 64;
	/**
	 * Timer 2 underflow.
	 */
	protected static final int VIA_IM_T2 = 32;
	/**
	 * Handshake.
	 */
	protected static final int VIA_IM_CB1 = 16;
	/**
	 * Handshake.
	 */
	protected static final int VIA_IM_CB2 = 8;
	/**
	 * Shift Register completion.
	 */
	protected static final int VIA_IM_SR = 4;
	/**
	 * Handshake.
	 */
	protected static final int VIA_IM_CA1 = 2;
	/**
	 * Handshake.
	 */
	protected static final int VIA_IM_CA2 = 1;

	/* Signal values (for signaling edges on the control lines) */
	public static final int VIA_SIG_CA1 = 0;
	protected static final int VIA_SIG_CA2 = 1;
	public static final int VIA_SIG_CB1 = 2;
	protected static final int VIA_SIG_CB2 = 3;

	public static final int VIA_SIG_FALL = 0;
	public static final int VIA_SIG_RISE = 1;

	
	/**
	 * Registers
	 */
	protected final byte[] via = new byte[16];

	protected int ifr;
	protected int ier;
	protected char tal;
	protected char tbl;
	protected long tau;
	protected long tbu;
	protected long tai;
	protected long tbi;
	protected int pb7;
	protected int pb7x;
	protected int pb7o;
	protected int pb7xx;
	protected int pb7sx;
	protected byte oldpa;
	protected byte oldpb;
	protected byte ila;
	protected byte ilb;
	protected int ca2State;
	protected int cb2State;

	protected boolean enabled;

	private final Event t1Alarm;	
	private final Event t2Alarm;

	private boolean lastState;

	private boolean isCa2Indinput() {
		return (via[VIA_PCR] & 0x0a) == 0x02;
	}

	private boolean isCa2Handshake() {
		return (via[VIA_PCR] & 0x0c) == 0x08;
	}

	private boolean isCa2PulseMode() {
		return (via[VIA_PCR] & 0x0e) == 0x0a;
	}

	private boolean isCa2ToggleMode() {
		return (via[VIA_PCR] & 0x0e) == 0x08;
	}

	private boolean isCb2Handshake() {
		return (via[VIA_PCR] & 0xc0) == 0x80;
	}

	private boolean isCb2PulseMode() {
		return (via[VIA_PCR] & 0xe0) == 0xa0;
	}

	private boolean isCb2ToggleMode() {
		return (via[VIA_PCR] & 0xe0) == 0x80;
	}

	/**
	 * <PRE>
	 * 01apr98 a.fachat
	 * 
	 * One-shot Timing (partly from 6522-VIA.txt):
	 * 
	 * 	                     +-+ +-+ +-+ +-+ +-+ +-+   +-+ +-+ +-+ +-+ +-+ +-+
	 * 	                02 --+ +-+ +-+ +-+ +-+ +-+ +-#-+ +-+ +-+ +-+ +-+ +-+ +-
	 * 	                       |   |                           |
	 * 	                       +---+                           |
	 * 	       WRITE T1C-H ----+   +-----------------#-------------------------
	 * 	        ___                |                           |
	 * 	        IRQ OUTPUT --------------------------#---------+
	 * 	                           |                           +---------------
	 * 	                           |                           |
	 * 	        PB7 OUTPUT --------+                           +---------------
	 * 	                           +-----------------#---------+
	 * 	         T1                | N |N-1|N-2|N-3|     | 0 | -1|N  |N-1|N-2|
	 * 	         T2                | N |N-1|N-2|N-3|     | 0 | -1| -2| -3| -4|
	 * 	                           |                           |
	 * 	                           |<---- N + 1.5 CYCLES ----.|<--- N + 2 cycles --.
	 * 	                                                         +---+
	 * 	 myviat*u* clk ------------------------------------------+   +--------
	 * 	                                                     |
	 * 	                                                     |
	 * 	                                                  call of
	 * 	                                                int_myvia*
	 * 	                                                   here
	 * 
	 * 	   real myviatau value = myviatau* + TAUOFFSET
	 * 	   myviatbu = myviatbu* + 0
	 * 
	 * 
	 * IRQ and PB7 are set/toggled at the low-high transition of Phi2,
	 * but int_* is called a half-cycle before that. Does that matter?
	 * 
	 * PB7 output is still to be implemented
	 * </PRE>
	 */

	/**
	 * timer values do not depend on a certain value here, but PB7 does...
	 */
	private static final int TAUOFFSET = -1;

	protected void checkInterrupts() {
		boolean irq = (ifr & ier & 0x7f) != 0;
		if (lastState ^ irq) {
			setIRQ(irq);
			lastState = irq;
		}
	}

	/* the next two are used in myvia_read() */

	private long myviata() {
		if (cpuClk() < tau - TAUOFFSET) {
			return tau - TAUOFFSET - cpuClk() - 2;
		} else {
			return tal - (cpuClk() - tau + TAUOFFSET) % (tal + 2);
		}
	}

	private long myviatb() {
		return tbu - cpuClk() - 2;
	}

	/**
	 * Update timer A latch value.
	 */
	private void updateMyviatal(final long rclk) {
		pb7x = 0;
		pb7xx = 0;

		if (rclk > tau) {
			final int nuf = (int) ((tal + 1 + rclk - tau) / (tal + 2));

			if (0 == (via[VIA_ACR] & 0x40)) {
				if (nuf - pb7sx > 1 || 0 == pb7) {
					pb7o = 1;
					pb7sx = 0;
				}
			}
			pb7 ^= nuf & 1;

			tau = TAUOFFSET + tal + 2
					+ (rclk - (rclk - tau + TAUOFFSET) % (tal + 2));
			if (rclk == tau - tal - 1) {
				pb7xx = 1;
			}
		}

		if (tau == rclk) {
			pb7x = 1;
		}
		
		tal = (char) ((via[VIA_T1LL] & 0xff) + ((via[VIA_T1LH] & 0xff) << 8));
	}

	private void updateMyviatbl() {
		tbl = (char) ((via[VIA_T2CL] & 0xff) + ((via[VIA_T2CH] & 0xff) << 8));
	}

	/* ------------------------------------------------------------------------- */
	public final void disable() {
		alarmUnset(t1Alarm);
		alarmUnset(t2Alarm);
		enabled = false;
	}

	/*
	 * according to Rockwell, all internal registers are cleared, except for the
	 * Timer (1 and 2, counter and latches) and the shift register.
	 */
	public void reset /* viacore_reset */() {
		/* clear registers */
		Arrays.fill(via, (byte) 0);
		for (int i = 4; i < 10; i++) {
			via[i] = (byte) 0xff; /* AB 98.08.23 */
		}

		tal = 0xffff;
		tbl = 0xffff;
		tau = cpuClk();
		tbu = cpuClk();

		ier = 0;
		ifr = 0;

		pb7 = 0;
		pb7x = 0;
		pb7o = 0;
		pb7xx = 0;
		pb7sx = 0;

		/* disable vice interrupts */
		tai = 0;
		tbi = 0;
		lastState = false;
		
		oldpa = (byte) 0xff;
		oldpb = (byte) 0xff;

		ca2State = 1;
		cb2State = 1;
		setCa2(ca2State); /* input = high */
		setCb2(cb2State); /* input = high */

		enabled = true;
	}

	public final void signal(final int line, final int edge) {
		switch (line) {
		case VIA_SIG_CA1:
			if ((edge != 0 ? 1 : 0) == (via[VIA_PCR] & 0x01)) {
				if (isCa2ToggleMode() && 0 == ca2State) {
					ca2State = 1;
					setCa2(ca2State);
				}
				ifr |= VIA_IM_CA1;
				checkInterrupts();
			}
			break;
		case VIA_SIG_CA2:
			if (0 == (via[VIA_PCR] & 0x08)) {
				ifr |= ((edge << 2 ^ via[VIA_PCR]) & 0x04) != 0 ? 0
						: VIA_IM_CA2;
				checkInterrupts();
			}
			break;
		case VIA_SIG_CB1:
			if ((edge != 0 ? 0x10 : 0) == (via[VIA_PCR] & 0x10)) {
				if (isCb2ToggleMode() && 0 == cb2State) {
					cb2State = 1;
					setCb2(cb2State);
				}
				ifr |= VIA_IM_CB1;
				checkInterrupts();
			}
			break;
		case VIA_SIG_CB2:
			if (0 == (via[VIA_PCR] & 0x80)) {
				ifr |= ((edge << 6 ^ via[VIA_PCR]) & 0x40) != 0 ? 0
						: VIA_IM_CB2;
				checkInterrupts();
			}
			break;
		}
	}

	public final void write(int addr, byte b) {
		long rclk = cpuClk();

		switch (addr) {

		/* these are done with saving the value */
		case VIA_PRA: /* port A */
			ifr &= ~VIA_IM_CA1;
			if (!isCa2Indinput()) {
				ifr &= ~VIA_IM_CA2;
			}
			if (isCa2Handshake()) {
				ca2State = 0;
				setCa2(ca2State);
				if (isCa2PulseMode()) {
					ca2State = 1;
					setCa2(ca2State);
				}
			}
			if ((ier & (VIA_IM_CA1 | VIA_IM_CA2)) != 0) {
				checkInterrupts();
			}
			// $FALL-THROUGH$

		case VIA_PRA_NHS: /* port A, no handshake */
			via[VIA_PRA_NHS] = b;
			addr = VIA_PRA;
			// $FALL-THROUGH$

		case VIA_DDRA:
			via[addr] = b;
			b = (byte) (via[VIA_PRA] | ~via[VIA_DDRA]);
			storePra(addr, b);
			oldpa = b;
			break;

		case VIA_PRB: /* port B */
			ifr &= ~VIA_IM_CB1;
			if ((via[VIA_PCR] & 0xa0) != 0x20) {
				ifr &= ~VIA_IM_CB2;
			}
			if (isCb2Handshake()) {
				cb2State = 0;
				setCb2(cb2State);
				if (isCb2PulseMode()) {
					cb2State = 1;
					setCb2(cb2State);
				}
			}
			if ((ier & (VIA_IM_CB1 | VIA_IM_CB2)) != 0) {
				checkInterrupts();
			}
			// $FALL-THROUGH$

		case VIA_DDRB:
			via[addr] = b;
			b = (byte) (via[VIA_PRB] | ~via[VIA_DDRB]);
			storePrb(b);
			oldpb = b;
			break;

		case VIA_SR: /* Serial Port output buffer */
			via[addr] = b;
			storeSr(b);
			break;

		/* Timers */

		case VIA_T1CL:
		case VIA_T1LL:
			via[VIA_T1LL] = b;
			updateMyviatal(rclk);
			break;

		case VIA_T1CH: /* Write timer A high */
			via[VIA_T1LH] = b;
			updateMyviatal(rclk);
			/* load counter with latch value */
			tau = rclk + tal + 3 + TAUOFFSET;
			tai = rclk + tal + 2;
			alarmUnset(t1Alarm);
			alarmSet(t1Alarm, tai);

			/* set pb7 state */
			pb7 = 0;
			pb7o = 0;

			/* Clear T1 interrupt */
			ifr &= ~VIA_IM_T1;
			checkInterrupts();
			break;

		case VIA_T1LH: /* Write timer A high order latch */
			via[addr] = b;
			updateMyviatal(rclk);

			/* Clear T1 interrupt */
			ifr &= ~VIA_IM_T1;
			checkInterrupts();
			break;

		case VIA_T2LL: /* Write timer 2 low latch */
			via[VIA_T2LL] = b;
			updateMyviatbl();
			storeT2l(b);
			break;

		case VIA_T2CH: /* Write timer 2 high */
			via[VIA_T2CH] = b;
			updateMyviatbl();
			tbu = rclk + tbl + 3;
			tbi = rclk + tbl + 2;
			alarmUnset(t2Alarm);
			alarmSet(t2Alarm, tbi);

			/* Clear T2 interrupt */
			ifr &= ~VIA_IM_T2;
			checkInterrupts();
			break;

		/* Interrupts */

		case VIA_IFR: /* 6522 Interrupt Flag Register */
			ifr &= ~b;
			checkInterrupts();
			break;

		case VIA_IER: /* Interrupt Enable Register */
			if ((b & VIA_IM_IRQ) != 0) {
				/* set interrupts */
				ier |= b & 0x7f;
			} else {
				/* clear interrupts */
				ier &= ~b;
			}
			checkInterrupts();
			break;

		/* Control */

		case VIA_ACR:
			/* bit 7 timer 1 output to PB7 */
			updateMyviatal(rclk);
			if (((via[VIA_ACR] ^ b) & 0x80) != 0) {
				if ((b & 0x80) != 0) {
					pb7 = 1 ^ pb7x;
				}
			}
			if (((via[VIA_ACR] ^ b) & 0x40) != 0) {
				pb7 ^= pb7sx;
				if ((b & 0x40) != 0) {
					if (pb7x != 0 || pb7xx != 0) {
						if (tal != 0) {
							pb7o = 1;
						} else {
							pb7o = 0;
							if ((via[VIA_ACR] & 0x80) != 0 && pb7x != 0
									&& 0 == pb7xx) {
								pb7 ^= 1;
							}
						}
					}
				}
			}
			pb7sx = pb7x;

			via[addr] = b;
			storeAcr(b);

			/* bit 5 timer 2 count mode */
			if ((b & 32) != 0) {
				/* TODO */
				/* update_myviatb(0); *//* stop timer if mode == 1 */
			}

			/* bit 4, 3, 2 shift register control */

			break;

		case VIA_PCR:

			/* bit 7, 6, 5 CB2 handshake/interrupt control */
			/* bit 4 CB1 interrupt control */

			/* bit 3, 2, 1 CA2 handshake/interrupt control */
			/* bit 0 CA1 interrupt control */

			if ((b & 0x0e) == 0x0c) { /* set output low */
				ca2State = 0;
			} else if ((b & 0x0e) == 0x0e) { /* set output high */
				ca2State = 1;
			} else { /* set to toggle/pulse/input */
				/* FIXME: is this correct if handshake is already active? */
				ca2State = 1;
			}
			setCa2(ca2State);

			if ((b & 0xe0) == 0xc0) { /* set output low */
				cb2State = 0;
			} else if ((b & 0xe0) == 0xe0) { /* set output high */
				cb2State = 1;
			} else { /* set to toggle/pulse/input */
				/* FIXME: is this correct if handshake is already active? */
				cb2State = 1;
			}
			setCb2(cb2State);

			via[addr] = b;

			break;

		default:
			via[addr] = b;

		} /* switch */
	}

	/* --------------------------------------------------------------------- */

	public final byte read/* viacore_read */(int addr) {
		long rclk = cpuClk();

		switch (addr) {

		case VIA_PRA: /* port A */
			ifr &= ~VIA_IM_CA1;
			if ((via[VIA_PCR] & 0x0a) != 0x02) {
				ifr &= ~VIA_IM_CA2;
			}
			if (isCa2Handshake()) {
				ca2State = 0;
				setCa2(ca2State);
				if (isCa2PulseMode()) {
					ca2State = 1;
					setCa2(ca2State);
				}
			}
			if ((ier & (VIA_IM_CA1 | VIA_IM_CA2)) != 0) {
				checkInterrupts();
			}
			// $FALL-THROUGH$

		case VIA_PRA_NHS: { /* port A, no handshake */
			/*
			 * WARNING: this pin reads the voltage of the output pins, not the
			 * ORA value as the other port. Value read might be different from
			 * what is expected due to excessive load.
			 */
			byte b = readPra();
			ila = b;
			return b;
		}

		case VIA_PRB: { /* port B */
			ifr &= ~VIA_IM_CB1;
			if ((via[VIA_PCR] & 0xa0) != 0x20) {
				ifr &= ~VIA_IM_CB2;
			}
			if ((ier & (VIA_IM_CB1 | VIA_IM_CB2)) != 0) {
				checkInterrupts();
			}

			/*
			 * WARNING: this pin reads the ORA for output pins, not the voltage
			 * on the pins as the other port.
			 */
			byte b = readPrb();
			ilb = b;
			b = (byte) (b & ~via[VIA_DDRB] | via[VIA_PRB] & via[VIA_DDRB]);

			if ((via[VIA_ACR] & 0x80) != 0) {
				updateMyviatal(rclk);
				b = (byte) (b & 0x7f | ((pb7 ^ pb7x | pb7o) != 0 ? 0x80 : 0));
			}
			return b;
		}

		/* Timers */
		case VIA_T1CL /* TIMER_AL */: /* timer A low */
			ifr &= ~VIA_IM_T1;
			checkInterrupts();
			return (byte) myviata();

		case VIA_T1CH /* TIMER_AH */: /* timer A high */
			return (byte) (myviata() >> 8);

		case VIA_T2CL /* TIMER_BL */: /* timer B low */
			ifr &= ~VIA_IM_T2;
			checkInterrupts();
			return (byte) myviatb();

		case VIA_T2CH /* TIMER_BH */: /* timer B high */
			return (byte) (myviatb() >> 8 & 0xff);

		case VIA_SR: /* Serial Port Shift Register */
			return via[addr];

		/* Interrupts */
		case VIA_IFR: {/* Interrupt Flag Register */
			byte t = (byte) ifr;
			if ((ifr & ier /* [VIA_IER] */) != 0) {
				t |= 0x80;
			}
			return t;
		}

		case VIA_IER: /* 6522 Interrupt Control Register */
			return (byte) (ier /* [VIA_IER] */| 0x80);

		} /* switch */


		return via[addr];
	}

	protected VIACore(String name) {
		t1Alarm = new Event(name + "T1") {
			@Override
			public void event() {
				/* handle continuous mode */
				if (0 != (via[VIA_ACR] & 0x40)) {
					tai += tal + 2;
					alarmSet(this, tai);
				}
				ifr |= VIA_IM_T1;
				checkInterrupts();
			}
		};

		t2Alarm = new Event(name + "T2") {
			@Override
			public void event() {
				ifr |= VIA_IM_T2;
				checkInterrupts();
			}
		};
	}

	protected abstract void alarmSet(final Event alarm, final long ti);
	protected abstract void alarmUnset(final Event alarm);
	protected abstract long cpuClk();
	protected abstract void setIRQ(boolean irq);
	protected abstract void storePra(int addr, byte value);
	protected abstract void storePrb(byte value);
	protected abstract void storeAcr(byte value);
	protected abstract void storeSr(byte value);
	protected abstract void storeT2l(byte value);
	protected abstract byte readPra();
	protected abstract byte readPrb();
	protected abstract void setCa2(int state);
	protected abstract void setCb2(int state);
}
