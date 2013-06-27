/**
 *                         CIA timer to produce interrupts
 *                         -------------------------------
 *  begin                : Wed Jun 7 2000
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

import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.pla.Bank;

/**
 * This class is heavily based on the ciacore/ciatimer source code from VICE.
 * The CIA state machine is lifted as-is. Big thanks to VICE project!
 * 
 * @author alankila
 */
public abstract class MOS6526 extends Bank {
	public enum Model {
		MOS6526, MOS6526A
	}

	/**
	 * These are the credits.
	 */
	private static final String CREDITS = "MOS6526 (CIA) Emulation:\n"
		+ "\tCopyright (C) 2001-2004 Simon White <sidplay2@yahoo.com>\n"
		+ "\tCopyright (C) 2009 VICE Project\n"
		+ "\tCopyright (C) 2009-2010 Antti S. Lankila\n";

	/**
	 * Interrupt flag: no interrupt.
	 */
	private static final byte INTERRUPT_NONE = 0;
	
	/**
	 * Interrupt flag: underflow Timer A.
	 */
	private static final byte INTERRUPT_UNDERFLOW_A = 1 << 0;

	/**
	 * Interrupt flag: underflow Timer B.
	 */
	private static final byte INTERRUPT_UNDERFLOW_B = 1 << 1;

	/**
	 * Interrupt flag: alarm clock.
	 */
	private static final byte INTERRUPT_ALARM = 1 << 2;

	/**
	 * Interrupt flag: serial port.
	 */
	private static final byte INTERRUPT_SP = 1 << 3;

	/**
	 * Interrupt flag: external flag.
	 */
	private static final byte INTERRUPT_FLAG = 1 << 4;

	//
	// CIA registers:
	//

	public static final int PRA = 0;

	public static final int PRB = 1;

	protected static final int DDRA = 2;

	protected static final int DDRB = 3;

	private static final int TAL = 4;

	private static final int TAH = 5;

	private static final int TBL = 6;

	private static final int TBH = 7;

	private static final int TOD_TEN = 8;

	private static final int TOD_SEC = 9;

	private static final int TOD_MIN = 10;

	private static final int TOD_HR = 11;

	private static final int SDR = 12;

	public static final int ICR = 13;

	protected static final int CRA = 14;

	protected static final int CRB = 15;

	protected abstract class InterruptSource extends Event {
		protected static final byte INTERRUPT_REQUEST = (byte) (1 << 7);

		public InterruptSource() {
			super("CIA Interrupt");
		}

		/** Interrupt control register */
		protected byte icr;

		/** Interrupt data register */
		protected byte idr;

		/**
		 * Trigger an interrupt.
		 * 
		 * @param interruptMask Interrupt flag number
		 */
		protected void trigger(final byte interruptMask) {
			idr |= interruptMask;
		}

		/**
		 * Clear interrupt state.
		 * 
		 * @return old interrupt state
		 */
		protected byte clear() {
			final byte old = idr;
			idr = 0;
			return old;
		}

		/**
		 * Clear pending interrupts, but do not signal to CPU we lost them.
		 * It is assumed that all components get reset() calls in synchronous manner.
		 */
		public void reset() {
			icr = idr = 0;
			context.cancel(this);
		}

		/**
		 * Set interrupt control mask bits.
		 * 
		 * @param interruptMask control mask bits
		 */
		public void setEnabled(final byte interruptMask) {
			icr |= interruptMask & ~INTERRUPT_REQUEST;
			trigger(INTERRUPT_NONE);
		}

		/**
		 * Clear selected interrupt control mask bits.
		 * 
		 * @param interruptMask control mask bits
		 */
		public void clearEnabled(final byte interruptMask) {
			icr &= ~interruptMask;
		}
	}

	/**
	 * InterruptSource that acts like new CIA
	 * 
	 * @author alankila
	 */
	protected final class InterruptSource6526A extends InterruptSource {
		@Override
		protected void trigger(final byte interruptMask) {
			super.trigger(interruptMask);
			if ((icr & idr) != 0 && (idr & INTERRUPT_REQUEST) == 0) {
				idr |= INTERRUPT_REQUEST;
				interrupt(true);
			}
		}

		@Override
		protected byte clear() {
			if ((idr & INTERRUPT_REQUEST) != 0) {
				interrupt(false);
			}
			return super.clear();
		}

		@Override
		public void event() {
			throw new RuntimeException("6526A event called unexpectedly");
		}
	}

	/**
	 * InterruptSource that acts like old CIA
	 * 
	 * @author alankila
	 */
	protected final class InterruptSource6526 extends InterruptSource {
		/** Have we already scheduled CIA->CPU interrupt transition? */
		private boolean scheduled;

		/**
		 * Trigger an interrupt.
		 * 
		 * @param interruptMask Interrupt flag number
		 */
		@Override
		protected void trigger(final byte interruptMask) {
			super.trigger(interruptMask);
			if ((icr & idr) != 0 && (idr & INTERRUPT_REQUEST) == 0) {
				schedule();
			}
		}

		/**
		 * Schedules an IRQ asserting state transition for next cycle.
		 */
		private void schedule() {
			if (! scheduled) {
				context.schedule(this, 1, Phase.PHI1);
				scheduled = true;
			}
		}

		/**
		 * Clear interrupt state.
		 * 
		 * @return old interrupt state
		 */
		@Override
		protected byte clear() {
			if (scheduled) {
				context.cancel(this);
				scheduled = false;
			}
			if ((idr & INTERRUPT_REQUEST) != 0) {
				interrupt(false);
			}
			return super.clear();
		}

		/**
		 * Signal interrupt to CPU.
		 */
		@Override
		public void event() {
			idr |= INTERRUPT_REQUEST;
			interrupt(true);
			scheduled = false;
		}

		/**
		 * Clear pending interrupts, but do not signal to CPU we lost them.
		 * It is assumed that all components get reset() calls in synchronous manner.
		 */
		@Override
		public void reset() {
			super.reset();
			scheduled = false;
		}
	}

	/**
	 * CIA interrupt controller.
	 */
	protected final InterruptSource interruptSource;

	/**
	 * This class implements a Timer A or B of a MOS6526 chip.
	 * 
	 * @author Ken Händel
	 * 
	 */
	protected abstract class Timer extends Event {

		protected static final int CIAT_CR_START   = 0x01;
		protected static final int CIAT_STEP       = 0x04;
		protected static final int CIAT_CR_ONESHOT = 0x08;
		protected static final int CIAT_CR_FLOAD   = 0x10;
		protected static final int CIAT_PHI2IN     = 0x20;
		protected static final int CIAT_CR_MASK    = CIAT_CR_START | CIAT_CR_ONESHOT | CIAT_CR_FLOAD | CIAT_PHI2IN;

		protected static final int CIAT_COUNT2     = 0x100;
		protected static final int CIAT_COUNT3     = 0x200;

		protected static final int CIAT_ONESHOT0   = 0x08 << 8;
		protected static final int CIAT_ONESHOT    = 0x08 << 16;
		protected static final int CIAT_LOAD1      = 0x10 << 8;
		protected static final int CIAT_LOAD       = 0x10 << 16;

		protected static final int CIAT_OUT        = 0x80000000;

		/**
		 * CRA/CRB control register / state.
		 */
		protected int state;

		/**
		 * Copy of regs[CRA/B]
		 */
		protected byte lastControlValue;

		/**
		 * Current timer value.
		 */
		protected short timer;
		/**
		 * Timer start value (Latch).
		 */
		protected short latch;
		/**
		 * PB6/PB7 Flipflop to signal underflows.
		 */
		protected boolean pbToggle;

		/**
		 * This is a tri-state:

		 * when -1: cia is completely stopped
		 * when 0: cia 1-clock events are ticking.
		 * otherwise: cycleskipevent is ticking, and the value is the first
		 * phi1 clock of skipping.
		 */
		protected long ciaEventPauseTime;

		/**
		 * Create a new timer.
		 * 
		 * @param eventName The name of the new timer.
		 */
		public Timer(final String eventName) {
			super(eventName);
		}

		/**
		 * Set CRA/CRB control register.
		 * 
		 * @param cr
		 *          Control register value
		 */
		public final void setControlRegister(final byte cr) {
			state &= ~CIAT_CR_MASK;
			state |= cr & CIAT_CR_MASK ^ CIAT_PHI2IN;
			lastControlValue = cr;
		}

		/**
		 * Get current timer value.
		 * 
		 * @return current timer value
		 */
		public final int getTimer() {
			return timer;
		}

		/**
		 * Get PB6/PB7 Flipflop state.
		 * 
		 * @return PB6/PB7 flipflop state
		 */
		public final boolean getPbToggle() {
			return pbToggle;
		}

		/**
		 * Set PB6/PB7 Flipflop state.
		 * 
		 * @param state
		 *            PB6/PB7 flipflop state
		 */
		public final void setPbToggle(final boolean state) {
			pbToggle = state;
		}

		/**
		 * Set high byte of Timer start value (Latch).
		 * 
		 * @param high
		 *            high byte of latch
		 */
		public final void setLatchHigh(final byte high) {
			latch = (short) (latch & 0xff | (high & 0xff) << 8);
			if ((state & CIAT_LOAD) != 0
					|| (state & CIAT_CR_START) == 0) {
				timer = latch;
			}
		}

		/**
		 * Set low byte of Timer start value (Latch).
		 * 
		 * @param low
		 *            low byte of latch
		 */
		public final void setLatchLow(final byte low) {
			latch = (short) (latch & 0xff00 | low & 0xff);
			if ((state & CIAT_LOAD) != 0) {
				timer = (short) (timer & 0xff00 | low & 0xff);
			}
		}

		/**
		 * Reset timer.
		 */
		public final void reset() {
			context.cancel(this);
			timer = latch = (short) 0xffff;
			pbToggle = false;
			state = 0;
			ciaEventPauseTime = 0;
			context.schedule(this, 1, Phase.PHI1);
		}

		/**
		 * Perform cycle skipping manually.
		 * 
		 * Clocks the CIA up to the state it should be in, and stops all events.
		 */
		public final void syncWithCpu() {
			if (ciaEventPauseTime > 0) {
				context.cancel(cycleSkippingEvent);
				final long elapsed = context.getTime(Phase.PHI2) - ciaEventPauseTime;
				/* It's possible for CIA to determine that it wants to go to sleep starting from the next
				 * cycle, and then have its plans aborted by CPU. Thus, we must avoid modifying
				 * the CIA state if the first sleep clock was still in the future.
				 */
				if (elapsed >= 0) {
					timer -= elapsed;
					clock();
				}
			}
			if (ciaEventPauseTime == 0) {
				context.cancel(this);
			}
			ciaEventPauseTime = -1;
		}

		/**
		 * Counterpart of syncWithCpu(),
		 * starts the event ticking if it is needed.
		 * No clock() call or anything such is permissible here!
		 */
		public final void wakeUpAfterSyncWithCpu() {
			ciaEventPauseTime = 0;
			context.schedule(this, 0, Phase.PHI1);
		}

		/**
		 * Timer ticking event.
		 */
		@Override
		public void event() {
			clock();
			reschedule();
		}

		/**
		 * Perform scheduled cycle skipping, and resume.
		 */
		private final Event cycleSkippingEvent = new Event("Skip CIA clock decrement cycles") {
			@Override
			public void event() {
				final long elapsed = context.getTime(Phase.PHI1) - ciaEventPauseTime;
				ciaEventPauseTime = 0;
				timer -= elapsed;
				Timer.this.event();
			}
		};

		/**
		 * Execute one CIA state transition.
		 */
		public void clock() {
			if (timer != 0 && (state & CIAT_COUNT3) != 0) {
				timer --;
			}

			/* ciatimer.c block start */
			int adj = state & (CIAT_CR_START | CIAT_CR_ONESHOT | CIAT_PHI2IN);
			if ((state & (CIAT_CR_START | CIAT_PHI2IN)) == (CIAT_CR_START | CIAT_PHI2IN)) {
				adj |= CIAT_COUNT2;
			}
			if ((state & CIAT_COUNT2) != 0
					|| (state & (CIAT_STEP | CIAT_CR_START)) == (CIAT_STEP | CIAT_CR_START)) {
				adj |= CIAT_COUNT3;
			}
			/* CR_FLOAD -> LOAD1, CR_ONESHOT -> ONESHOT0, LOAD1 -> LOAD, ONESHOT0 -> ONESHOT */
			adj |= (state & (CIAT_CR_FLOAD | CIAT_CR_ONESHOT | CIAT_LOAD1 | CIAT_ONESHOT0)) << 8;
			state = adj;
			/* ciatimer.c block end */

			if (timer == 0 && (state & CIAT_COUNT3) != 0) {
				state |= CIAT_LOAD | CIAT_OUT;

				if ((state & (CIAT_ONESHOT | CIAT_ONESHOT0)) != 0) {
					state &= ~(CIAT_CR_START | CIAT_COUNT2);
				}

				// By setting bits 2&3 of the control register,
				// PB6/PB7 will be toggled between high and low at each underflow.
				final boolean toggle = (lastControlValue & 0x06) == 6;
				pbToggle = toggle && !pbToggle;

				// Implementation of the serial port
				serialPort();

				// Timer A signals underflow handling: IRQ/B-count
				underFlow();
			}

			if ((state & CIAT_LOAD) != 0) {
				timer = latch;
				state &= ~CIAT_COUNT3;
			}
		}

		/**
		 * Reschedule CIA event at the earliest interesting time.
		 * If CIA timer is stopped or is programmed to just count down,
		 * the events are paused.
		 */
		private final void reschedule() {
			/* There are only two subcases to consider.
			 *
			 * - are we counting, and if so, are we going to
			 *   continue counting?
			 * - have we stopped, and are there no conditions to force a new beginning?
			 * 
			 * Additionally, there are numerous flags that are present only in passing manner,
			 * but which we need to let cycle through the CIA state machine.
			 */
			final int unwanted = CIAT_OUT | CIAT_CR_FLOAD | CIAT_LOAD1 | CIAT_LOAD;
			if ((state & unwanted) != 0) {
				context.schedule(this, 1);
				return;
			}

			if ((state & CIAT_COUNT3) != 0) {
				/* Test the conditions that keep COUNT2 and thus COUNT3 alive, and also
				 * ensure that all of them are set indicating steady state operation. */

				final int wanted = CIAT_CR_START | CIAT_PHI2IN | CIAT_COUNT2 | CIAT_COUNT3;
				if ((timer & 0xffff) > 2 && (state & wanted) == wanted) {
					/* we executed this cycle, therefore the pauseTime is +1. If we are called
					 * to execute on the very next clock, we need to get 0 because there's
					 * another timer-- in it. */
					ciaEventPauseTime = context.getTime(Phase.PHI1) + 1;
					/* execute event slightly before the next underflow. */
					context.schedule(cycleSkippingEvent, timer - 1 & 0xffff);
					return;
				}

				/* play safe, keep on ticking. */
				context.schedule(this, 1);
				return;

			} else {
				/* Test conditions that result in CIA activity in next clocks.
				 * If none, stop. */
				final int unwanted1 = CIAT_CR_START | CIAT_PHI2IN;
				final int unwanted2 = CIAT_CR_START | CIAT_STEP;

				if ((state & unwanted1) == unwanted1
						|| (state & unwanted2) == unwanted2) {
					context.schedule(this, 1);
					return;
				}

				ciaEventPauseTime = -1;
				return;
			}
		}

		//
		// Abstract methods
		//

		/**
		 * Handle the serial port.
		 */
		public abstract void serialPort();

		/**
		 * Signal timer underflow.
		 */
		public abstract void underFlow();
	}

	/**
	 * This is the timer A of this CIA.
	 * 
	 * @author Ken Händel
	 * 
	 */
	private final class TimerA extends Timer {
		/**
		 * Create timer A.
		 */
		public TimerA() {
			super("CIA A");
		}

		@Override
		public void serialPort() {
			// Handle serial port
			if ((regs[CRA] & 0x40) != 0) {
				if (sdr_count != 0) {
					if (--sdr_count == 0) {
						interruptSource.trigger(INTERRUPT_SP);
					}
				}
				if (sdr_count == 0 && sdr_buffered) {
					sdr_out = regs[SDR];
					sdr_buffered = false;
					sdr_count = 16;
					// Output rate 8 bits at ta / 2
				}
			}
		}

		/**
		 * This event exists solely to break the ambiguity of what scheduling on
		 * top of PHI1 causes, because there is no ordering between events on
		 * same phase. Thus it is scheduled in PHI2 to ensure the b.event() is
		 * run once before the value changes.
		 * <UL>
		 * <LI>PHI1 a.event() (which calls underFlow())
		 * <LI>PHI1 b.event()
		 * <LI>PHI2 bTick.event()
		 * <LI>PHI1 a.event()
		 * <LI>PHI1 b.event()
		 * </UL>
		 */
		private final Event bTick = new Event("CIA B counts A") {
			@Override
			public void event() {
				/* we pretend that we are CPU doing a write to ctrl register */
				b.syncWithCpu();
				b.state |= Timer.CIAT_STEP;
				b.wakeUpAfterSyncWithCpu();
			}
		};

		/**
		 * Signal underflows of Timer A to Timer B.
		 */
		@Override
		public void underFlow() {
			interruptSource.trigger(INTERRUPT_UNDERFLOW_A);
			if ((regs[CRB] & 0x41) == 0x41) {
				if ((b.state & Timer.CIAT_CR_START) != 0) {
					context.schedule(bTick, 0, Phase.PHI2);
				}
			}
		}
	}

	/**
	 * This is the timer B of this CIA.
	 * 
	 * @author Ken Händel
	 * 
	 */
	private final class TimerB extends Timer {
		/**
		 * Create timer B.
		 */
		public TimerB() {
			super("CIA B");
		}

		@Override
		public void serialPort() {
			// nothing to do
		}

		@Override
		public void underFlow() {
			interruptSource.trigger(INTERRUPT_UNDERFLOW_B);
		}
	}

	/**
	 * Timers A and B.
	 */
	protected final Timer a, b;

	/**
	 * These are all CIA registers.
	 */
	protected byte[] regs = new byte[0x10];

	//
	// Serial Data Registers
	//

	protected byte sdr_out;

	protected boolean sdr_buffered;

	protected int sdr_count;

	/**
	 * Event context.
	 */
	protected final EventScheduler context;

	//
	// Time Of Day
	//

	protected boolean m_todlatched;

	protected boolean m_todstopped;

	protected byte[] m_todclock = new byte[4], m_todalarm = new byte[4], m_todlatch = new byte[4];

	protected long m_todCycles, m_todPeriod = 0xffffffffL;

	protected final Event m_todEvent = new Event("CIA Time of Day") {
		//
		// TOD implementation taken from Vice
		//

		/**
		 * Convert 8-bit value to BCD.
		 * 
		 * @param thebyte
		 *            the value to convert
		 * @return BCD code
		 */
		private byte byte2bcd(final byte thebyte) {
			final int num = thebyte & 0xff;
			return (byte) ((num / 10 << 4) + num % 10);
		}

		/**
		 * Convert BCD to 8-bit value.
		 * 
		 * @param bcd
		 *            BCD code
		 * @return 8-bit value
		 */
		private byte bcd2byte(final byte bcd) {
			return (byte) (10 * ((bcd & 0xf0) >> 4) + (bcd & 0xf));
		}


		@Override
		public void event() {
			// Reload divider according to 50/60 Hz flag
			// Only performed on expiry according to Frodo
			if ((regs[CRA] & 0x80) != 0) {
				m_todCycles += m_todPeriod * 5;
			} else {
				m_todCycles += m_todPeriod * 6;
			}

			// Fixed precision 25.7
			context.schedule(this, m_todCycles >> 7);
			m_todCycles &= 0x7F; // Just keep the decimal part

			if (!m_todstopped) {
				// inc timer
				final byte[] tod = m_todclock;
				int todPos = 0;
				byte t = (byte) (bcd2byte(tod[todPos]) + 1);
				tod[todPos++] = byte2bcd((byte) (t % 10));
				if (t >= 10) {
					t = (byte) (bcd2byte(tod[todPos]) + 1);
					tod[todPos++] = byte2bcd((byte) (t % 60));
					if (t >= 60) {
						t = (byte) (bcd2byte(tod[todPos]) + 1);
						tod[todPos++] = byte2bcd((byte) (t % 60));
						if (t >= 60) {
							byte pm = (byte) (tod[todPos] & 0x80);
							t = (byte) (tod[todPos] & 0x1f);
							if (t == 0x11) {
								pm ^= 0x80; // toggle am/pm on 0:59->1:00 hr
							}
							if (t == 0x12) {
								t = 1;
							} else if (++t == 10) {
								t = 0x10; // increment, adjust bcd
							}
							t &= 0x1f;
							tod[todPos] = (byte) (t | pm);
						}
					}
				}
				// check alarm
				if (Arrays.equals(m_todalarm, m_todclock)) {
					interruptSource.trigger(INTERRUPT_ALARM);
				}
			}
		}
	};

	/**
	 * Create a new CIA.
	 * @param ctx the event context
	 */
	protected MOS6526(final EventScheduler ctx, final Model model) {
		this.context = ctx;
		this.interruptSource = model == Model.MOS6526A
		? new InterruptSource6526A() : new InterruptSource6526();
		a = new TimerA();
		b = new TimerB();
		reset();
	}
	//
	// Environment Interface
	//

	/**
	 * Signal interrupt.
	 * 
	 * @param state
	 *            interrupt state
	 */
	public abstract void interrupt(boolean state);

	public abstract void pulse();

	public abstract byte readPRA();

	public abstract byte readPRB();

	public abstract void writePRA(byte data);

	public abstract void writePRB(byte data);
	
	/**
	 * External interrupt control.
	 * 
	 * @param flag
	 *             Interrupt flag
	 */
	public void setFlag(final boolean flag) {
		if (flag) {
			interruptSource.trigger(INTERRUPT_FLAG);
		}
	}
	
	//
	// Component Standard Calls
	//

	/**
	 * Reset CIA.
	 */
	public void reset() {
		a.reset();
		b.reset();
		sdr_out = 0;
		sdr_count = 0;
		sdr_buffered = false;
		interruptSource.reset();
		Arrays.fill(regs, (byte) 0);

		// Reset tod
		Arrays.fill(m_todclock, (byte) 0);
		Arrays.fill(m_todalarm, (byte) 0);
		Arrays.fill(m_todlatch, (byte) 0);

		m_todlatched = false;
		m_todstopped = true;
		m_todclock[TOD_HR - TOD_TEN] = 1; // the most common value
		m_todCycles = 0;

		context.schedule(m_todEvent, 0, Phase.PHI1);
	}

	/**
	 * Read CIA register.
	 * 
	 * @param addr
	 *            register address to read (lowest 4 bits)
	 */
	@Override
	public final byte read(int addr) {
		addr &= 0xf;
		
		a.syncWithCpu();
		a.wakeUpAfterSyncWithCpu();
		b.syncWithCpu();
		b.wakeUpAfterSyncWithCpu();

		switch (addr) {
		case PRA: // Simulate a serial port
			// 0 = input 0xff = output
			return (byte) (readPRA() & (regs[PRA] | ~regs[DDRA]));
		case PRB:
			byte data = (byte) (readPRB() & (regs[PRB] | ~regs[DDRB]));
        	pulse();
			// Timers can appear on the port
			if ((regs[CRA] & 0x02) != 0) {
				data &= 0xbf;
				if ((regs[CRA] & 0x04) != 0 ? a.getPbToggle() : (a.state & Timer.CIAT_OUT) != 0) {
					data |= 0x40;
				}
			}
			if ((regs[CRB] & 0x02) != 0) {
				data &= 0x7f;
				if ((regs[CRB] & 0x04) != 0 ? b.getPbToggle() : (b.state & Timer.CIAT_OUT) != 0) {
					data |= 0x80;
				}
			}
			return data;
		case TAL:
			return (byte) (a.getTimer() & 0xff);
		case TAH:
			return (byte) (a.getTimer() >> 8);
		case TBL:
			return (byte) (b.getTimer() & 0xff);
		case TBH:
			return (byte) (b.getTimer() >> 8);

			// TOD implementation taken from Vice
			// TOD clock is latched by reading Hours, and released
			// upon reading Tenths of Seconds. The counter itself
			// keeps ticking all the time.
			// Also note that this latching is different from the input one.
		case TOD_TEN: // Time Of Day clock 1/10 s
		case TOD_SEC: // Time Of Day clock sec
		case TOD_MIN: // Time Of Day clock min
		case TOD_HR: // Time Of Day clock hour
			if (!m_todlatched) {
				System.arraycopy(m_todclock, 0, m_todlatch, 0, 4);
			}
			if (addr == TOD_TEN) {
				m_todlatched = false;
			}
			if (addr == TOD_HR) {
				m_todlatched = true;
			}
			return m_todlatch[addr - TOD_TEN];

		case ICR:
			return interruptSource.clear();

		case CRA:
			return (byte) (regs[CRA] & 0xee | a.state & 1);
		case CRB:
			return (byte) (regs[CRB] & 0xee | b.state & 1);
		default:
			return regs[addr];
		}
	}

	/**
	 * Write CIA register.
	 * 
	 * @param addr
	 *            register address to write (lowest 4 bits)
	 * @param data
	 *            value to write
	 */
	@Override
	public final void write(int addr, byte data) {
		addr &= 0xf;
		
		a.syncWithCpu();
		b.syncWithCpu();

		byte oldData = regs[addr];
		regs[addr] = data;

		switch (addr) {
		case PRA:
		case DDRA:
			writePRA((byte) (regs[PRA] | ~regs[DDRA]));
			break;
		case PRB:
			pulse();
			// $FALL-THROUGH$
		case DDRB:
			writePRB((byte) (regs[PRB] | ~regs[DDRB]));
			break;
		case TAL:
			a.setLatchLow(data);
			break;
		case TBL:
			b.setLatchLow(data);
			break;
		case TAH:
			a.setLatchHigh(data);
			break;
		case TBH:
			b.setLatchHigh(data);
			break;
			// TOD implementation taken from Vice
		case TOD_HR: // Time Of Day clock hour
			// Flip AM/PM on hour 12
			// (Andreas Boose <viceteam@t-online.de> 1997/10/11).
			// Flip AM/PM only when writing time, not when writing alarm
			// (Alexander Bluhm <mam96ehy@studserv.uni-leipzig.de> 2000/09/17).
			data &= 0x9f;
			if ((data & 0x1f) == 0x12 && (regs[CRB] & 0x80) == 0) {
				data ^= 0x80;
			}
			//$FALL-THROUGH$

		case TOD_TEN: // Time Of Day clock 1/10 s
		case TOD_SEC: // Time Of Day clock sec
		case TOD_MIN: // Time Of Day clock min
			if ((regs[CRB] & 0x80) != 0) {
				m_todalarm[addr - TOD_TEN] = data;
			} else {
				if (addr == TOD_TEN) {
					m_todstopped = false;
				}
				if (addr == TOD_HR) {
					m_todstopped = true;
				}
				m_todclock[addr - TOD_TEN] = data;
			}
			// check alarm
			if (!m_todstopped && Arrays.equals(m_todalarm, m_todclock)) {
				interruptSource.trigger(INTERRUPT_ALARM);
			}
			break;

		case SDR:
			if ((regs[CRA] & 0x40) != 0) {
				sdr_buffered = true;
			}
			break;

		case ICR:
			if ((data & 0x80) != 0) {
				interruptSource.setEnabled(data);
			} else {
				interruptSource.clearEnabled(data);
			}
			break;

		case CRA:
		case CRB: {
			final Timer t = addr == CRA ? a : b;
			final boolean start = (data & 1) != 0 && (oldData & 1) == 0;
			if (start) {
				// Reset the underflow flipflop for the data port
				t.setPbToggle(true);
			}
			if (addr == CRB) {
				t.setControlRegister((byte) (data | (data & 0x40) >> 1));
			} else {
				t.setControlRegister(data);
			}
			break;
		}
		default:
			break;
		}

		a.wakeUpAfterSyncWithCpu();
		b.wakeUpAfterSyncWithCpu();
	}

	/**
	 * Get the credits.
	 * 
	 * @return the credits
	 */
	public static final String credits() {
		return CREDITS;
	}

	/**
	 * Set day-of-time event occurrence of rate.
	 * 
	 * @param clock
	 */
	public final void setDayOfTimeRate(final double clock) {
		m_todPeriod = (long) (clock * (1 << 7));
	}
}