/**
 *                              Minimal VIC emulation
 *                              ---------------------
 *  begin                : Wed May 21 2001
 *  copyright            : (C) 2001 by Simon White
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
package libsidplay.components.mos656x;

import java.util.Arrays;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

/**
 * References below are from: <BR>
 * The MOS 6567/6569 video controller (VIC-II) and its application in the
 * Commodore 64 http://www.uni-mainz.de/~bauec002/VIC-Article.gz
 * 
 * @author Ken Händel
 * 
 */
public final class OldMOS656X extends Event {

	public enum Model { MOS6567R56A, MOS6567R8, MOS6569 }
	
	public static final int MOS6567R56A_SCREEN_HEIGHT = 262;

	public static final int MOS6567R56A_SCREEN_WIDTH = 64;

	public static final int MOS6567R56A_FIRST_DMA_LINE = 0x30;

	public static final int MOS6567R56A_LAST_DMA_LINE = 0xf7;

	public static final int MOS6567R8_SCREEN_HEIGHT = 263;

	public static final int MOS6567R8_SCREEN_WIDTH = 65;

	public static final int MOS6567R8_FIRST_DMA_LINE = 0x30;

	public static final int MOS6567R8_LAST_DMA_LINE = 0xf7;

	public static final int MOS6569_SCREEN_HEIGHT = 312;

	public static final int MOS6569_SCREEN_WIDTH = 63;

	public static final int MOS6569_FIRST_DMA_LINE = 0x30;

	public static final int MOS6569_LAST_DMA_LINE = 0xf7;

	protected byte regs[] = new byte[0x40];

	protected byte icr, idr, ctrl1;

	protected int /* uint_least16_t */yrasters, xrasters, raster_irq;

	protected int /* uint_least16_t */raster_x, raster_y;

	protected int /* uint_least16_t */first_dma_line, last_dma_line, y_scroll;

	protected boolean bad_lines_enabled, bad_line;

	protected boolean vblanking;

	protected boolean lp_triggered;

	protected byte lpx, lpy;

	protected byte sprite_dma, sprite_expand_y;

	protected byte sprite_mc_base[] = new byte[8];

	protected long /* event_clock_t */m_rasterClk;

	protected final EventScheduler event_context;

	protected final VICEnvironment env;

	public OldMOS656X(final EventScheduler context, final VICEnvironment env) {
		super("VIC Raster");
		this.env = env;
		event_context = context;
		// sprite_enable = regs[0x15];
		// sprite_y_expansion = regs[0x17];
		chip(Model.MOS6569);
	}

	@Override
	public void event() {
		final int delay = clock();
		event_context.schedule(this, delay, Event.Phase.PHI1);
	}

	private int clock() {
		final long cycles = event_context.getTime(event_context.phase()) - m_rasterClk;

		int delay = 1;
		// Cycle already executed check
		if (cycles == 0) {
			return delay;
		}

		// Update x raster
		m_rasterClk += cycles;
		raster_x += cycles;
		final int cycle = (raster_x + 9) % xrasters;
		raster_x %= xrasters;

		switch (cycle) {
		case 0: { // Calculate sprite DMA
			final byte y = (byte) (raster_y & 0xff);
			byte mask = 1;
			sprite_expand_y ^= regs[0x17] /* sprite_y_expansion */; // 3.8.1-2
			for (int i = 1; i < 0x10; i += 2, mask <<= 1) {
				// 3.8.1-3
				if ((regs[0x15] /* sprite_enable */& mask) != 0
						&& y == regs[i]) {
					sprite_dma |= mask;
					sprite_mc_base[i >> 1] = 0;
					sprite_expand_y &= ~(regs[0x17] /* sprite_y_expansion */& mask);
				}
			}

			delay = 2;
			if ((sprite_dma & 0x01) != 0) {
				env.signalAEC(false);
			} else {
				env.signalAEC(true);
				// No sprites before next compulsory cycle
				if ((sprite_dma & 0x1f) == 0) {
					delay = 9;
				}
			}
			break;
		}

		case 1:
			break;

		case 2:
			if ((sprite_dma & 0x02) != 0) {
				env.signalAEC(false);
			}
			break;

		case 3:
			if ((sprite_dma & 0x03) == 0) {
				env.signalAEC(true);
			}
			break;

		case 4:
			if ((sprite_dma & 0x04) != 0) {
				env.signalAEC(false);
			}
			break;

		case 5:
			if ((sprite_dma & 0x06) == 0) {
				env.signalAEC(true);
			}
			break;

		case 6:
			if ((sprite_dma & 0x08) != 0) {
				env.signalAEC(false);
			}
			break;

		case 7:
			if ((sprite_dma & 0x0c) == 0) {
				env.signalAEC(true);
			}
			break;

		case 8:
			if ((sprite_dma & 0x10) != 0) {
				env.signalAEC(false);
			}
			break;

		case 9: // IRQ occurred (xraster != 0)
			if (raster_y == yrasters - 1) {
				vblanking = true;
			} else {
				raster_y++;
				// Trigger raster IRQ if IRQ line reached
				if (raster_y == raster_irq) {
					trigger(MOS656X_INTERRUPT_RST);
				}
			}
			if ((sprite_dma & 0x18) == 0) {
				env.signalAEC(true);
			}
			break;

		case 10: // Vertical blank (line 0)
			if (vblanking) {
				vblanking = lp_triggered = false;
				raster_y = 0;
				// Trigger raster IRQ if IRQ in line 0
				if (raster_irq == 0) {
					trigger(MOS656X_INTERRUPT_RST);
				}
			}
			if ((sprite_dma & 0x20) != 0) {
				env.signalAEC(false);
			} else if ((sprite_dma & 0xf8) == 0) {
				delay = 10;
			}
			break;

		case 11:
			if ((sprite_dma & 0x30) == 0) {
				env.signalAEC(true);
			}
			break;

		case 12:
			if ((sprite_dma & 0x40) != 0) {
				env.signalAEC(false);
			}
			break;

		case 13:
			if ((sprite_dma & 0x60) == 0) {
				env.signalAEC(true);
			}
			break;

		case 14:
			if ((sprite_dma & 0x80) != 0) {
				env.signalAEC(false);
			}
			break;

		case 15:
			delay = 2;
			if ((sprite_dma & 0xc0) == 0) {
				env.signalAEC(true);
				delay = 5;
			}
			break;

		case 16:
			break;

		case 17:
			delay = 2;
			if ((sprite_dma & 0x80) == 0) {
				env.signalAEC(true);
				delay = 3;
			}
			break;

		case 18:
			break;

		case 19:
			env.signalAEC(true);
			break;

		case 20: // Start bad line
		{ // In line $30, the DEN bit controls if Bad Lines can occur
			if (raster_y == first_dma_line) {
				bad_lines_enabled = (ctrl1 & 0x10) != 0;
			}

			// Test for bad line condition
			bad_line = raster_y >= first_dma_line
			&& raster_y <= last_dma_line
			&& (raster_y & 7) == y_scroll && bad_lines_enabled;

			if (bad_line) {
				// DMA starts on cycle 23
				env.signalAEC(false);
			}
			delay = 3;
			break;
		}

		case 23: { // 3.8.1-7
			for (int i = 0; i < 8; i++) {
				if ((sprite_expand_y & 1 << i) != 0) {
					sprite_mc_base[i] += 2;
				}
			}
			break;
		}

		case 24: {
			byte mask = 1;
			for (int i = 0; i < 8; i++, mask <<= 1) { // 3.8.1-8
				if ((sprite_expand_y & mask) != 0) {
					sprite_mc_base[i]++;
				}
				if ((sprite_mc_base[i] & 0x3f) == 0x3f) {
					sprite_dma &= ~mask;
				}
			}
			delay = 39;
			break;
		}

		case 63: // End DMA - Only get here for non PAL
			env.signalAEC(true);
			delay = xrasters - cycle;
			break;

		default:
			if (cycle < 23) {
				delay = 23 - cycle;
			} else if (cycle < 63) {
				delay = 63 - cycle;
			} else {
				delay = xrasters - cycle;
			}
		}

		return delay;
	}

	protected void trigger(final int irq) {
		if (irq == 0) { // Clear any requested IRQs
			if ((idr & MOS656X_INTERRUPT_REQUEST) != 0) {
				env.interruptIRQ(false);
			}
			idr = 0;
			return;
		}

		idr |= irq;
		if ((icr & idr) != 0) {
			if ((idr & MOS656X_INTERRUPT_REQUEST) == 0) {
				idr |= MOS656X_INTERRUPT_REQUEST;
				env.interruptIRQ(true);
			}
		}
	}

	public void chip(final Model model) {
		switch (model) {
		// Seems to be an older NTSC chip
		case MOS6567R56A:
			yrasters = MOS6567R56A_SCREEN_HEIGHT;
			xrasters = MOS6567R56A_SCREEN_WIDTH;
			first_dma_line = MOS6567R56A_FIRST_DMA_LINE;
			last_dma_line = MOS6567R56A_LAST_DMA_LINE;
			break;

			// NTSC Chip
		case MOS6567R8:
			yrasters = MOS6567R8_SCREEN_HEIGHT;
			xrasters = MOS6567R8_SCREEN_WIDTH;
			first_dma_line = MOS6567R8_FIRST_DMA_LINE;
			last_dma_line = MOS6567R8_LAST_DMA_LINE;
			break;

			// PAL Chip
		case MOS6569:
			yrasters = MOS6569_SCREEN_HEIGHT;
			xrasters = MOS6569_SCREEN_WIDTH;
			first_dma_line = MOS6569_FIRST_DMA_LINE;
			last_dma_line = MOS6569_LAST_DMA_LINE;
			break;
		}
		raster_y = yrasters - 1;
	}

	/**
	 * Handle light pen trigger
	 */
	public void lightpen() {
		// Synchronise simulation
		clock();

		if (!lp_triggered) {
			// Latch current coordinates
			lpx = (byte) (raster_x << 2);
			lpy = (byte) (raster_y & 0xff);
			trigger(MOS656X_INTERRUPT_LP);
		}
	}

	//
	// Component Standard Calls
	//

	public void reset() {
		icr = idr = ctrl1 = 0;
		raster_irq = 0;
		y_scroll = 0;
		raster_y = yrasters - 1;
		raster_x = 0;
		bad_lines_enabled = false;
		m_rasterClk = 0;
		vblanking = lp_triggered = false;
		lpx = lpy = 0;
		sprite_dma = 0;
		sprite_expand_y = (byte) 0xff;
		Arrays.fill(regs, (byte) 0);
		Arrays.fill(sprite_mc_base, (byte) 0);
		event_context.schedule(this, 0, Event.Phase.PHI1);
	}

	public byte read(final int addr) {
		if (addr > 0x3f) {
			return 0;
		}
		if (addr > 0x2e) {
			return (byte) 0xff;
		}

		// Sync up timers
		clock();

		switch (addr) {
		case 0x11: // Control register 1
			return (byte) (ctrl1 & 0x7f | (raster_y & 0x100) >> 1);
		case 0x12: // Raster counter
			return (byte) (raster_y & 0xFF);
		case 0x13:
			return lpx;
		case 0x14:
			return lpy;
		case 0x19: // IRQ flags
			return idr;
		case 0x1a: // IRQ mask
			return (byte) (icr | 0xf0);
		default:
			return regs[addr];
		}
	}

	public void write(final int addr, final byte data) {
		if (addr > 0x3f) {
			return;
		}

		regs[addr] = data;

		// Sync up timers
		clock();

		switch (addr) {
		case 0x11: // Control register 1
		{
			raster_irq &= 0x00ff | (data & 0x80) << 1;
			ctrl1 = data;
			y_scroll = data & 7;

			if (raster_x < 11) {
				break;
			}

			// In line $30, the DEN bit controls if Bad Lines can occur
			if (raster_y == first_dma_line && (data & 0x10) != 0) {
				bad_lines_enabled = true;
			}

			// Bad Line condition?
			bad_line = raster_y >= first_dma_line
			&& raster_y <= last_dma_line
			&& (raster_y & 7) == y_scroll && bad_lines_enabled;

			// Start bad dma line now
			if (bad_line && raster_x < 53) {
				env.signalAEC(false);
			}
			break;
		}

		case 0x12: // Raster counter
			raster_irq = raster_irq & 0x100 | data & 0xff;
			break;

		case 0x17:
			sprite_expand_y |= ~data; // 3.8.1-1
			break;

		case 0x19: // IRQ flags
			idr &= ~data & 0x0f | 0x80;
			if ((idr & 0xff) == 0x80) {
				trigger(0);
			}
			break;

		case 0x1a: // IRQ mask
			icr = (byte) (data & 0x0f);
			trigger(icr & idr);
			break;
		}
	}

	// ----------------------------------------------------------------------------
	// Inline functions.
	// ----------------------------------------------------------------------------

	public static final int MOS656X_INTERRUPT_RST = 1 << 0;

	public static final int MOS656X_INTERRUPT_LP = 1 << 3;

	public static final int MOS656X_INTERRUPT_REQUEST = 1 << 7;

	// ----------------------------------------------------------------------------
	// END Inline functions.
	// ----------------------------------------------------------------------------
}
