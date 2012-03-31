/**
 *                           ReSid Emulation
 *                           ---------------
 *  begin                : Fri Apr 4 2001
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
package resid_builder;

import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import resid_builder.ReSIDBuilder.MixerEvent;
import resid_builder.resid.ISIDDefs.ChipModel;
import resid_builder.resid.ISIDDefs.SamplingMethod;
import resid_builder.resid.SID;
import sidplay.ini.IniFilterSection;

public class ReSID extends SIDEmu {
	private static final Logger RESID = Logger.getLogger(ReSID.class.getName());

	private static final String VERSION = "0.0.2";

	private static final int OUTPUTBUFFERSIZE = 5000; /* supports 5 ms chunk at 96 kHz */

	private final SID sid = new SID();

	protected int position;
	
	protected int buffer[];

	private final ReSIDBuilder.MixerEvent mixerEvent;
	
	public ReSID(EventScheduler context, MixerEvent mixerEvent) {
		super(context);
		this.mixerEvent = mixerEvent;
		buffer = new int[OUTPUTBUFFERSIZE];
		position = 0;
		reset((byte) 0);
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
		/* No matter how many chips are in use,
		 * mixerEvent is singleton with respect to them.
		 * Only one will be scheduled. This is a bit dirty, though. */
		context.cancel(mixerEvent);
		mixerEvent.setContext(context);
		context.schedule(mixerEvent, 0, Event.Phase.PHI2);
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		// correction for sid_detection.prg
		lastTime--;
		clock();
		return sid.read(addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		super.write(addr, data);
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}
		
		clock();
		sid.write(addr, data);
	}

	@Override
	public void clock() {
		int cycles = clocksSinceLastAccess();
		position += sid.clock(cycles, buffer, position);
	}

	@Override
	public void setFilter(final boolean enable) {
		sid.getFilter6581().enable(enable);
		sid.getFilter8580().enable(enable);
	}

	public void filter(final IniFilterSection filter6581, final IniFilterSection filter8580) {
		if (filter6581 != null) {
			sid.getFilter6581().setFilterCurve(filter6581.getFilter6581CurvePosition());
		}

		if (filter8580 != null) {
			sid.getFilter8580().setFilterCurve(filter8580.getFilter8580CurvePosition());
		}
	}


	@Override
	public void setEnabled(final int num, final boolean mute) {
		sid.mute(num, mute);
	}

	public void sampling(final double systemclock, final float freq, final SamplingMethod method) {
		sid.setSamplingParameters(systemclock, method, freq, 20000);
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 */
	public void model(final ChipModel model) {
		sid.setChipModel(model);
	}

	// Standard component functions
	public static final String credits() {
		String m_credit = "ReSID V" + VERSION + " Engine:\n";
		m_credit += "\tCopyright (C) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
		m_credit += "MOS6581/8580 (SID) Emulation:\n";
		m_credit += "\tCopyright (C) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (C) 2005-2011 Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

	public SID sid() {
		return sid;
	}
	
	@Override
	public ChipModel getChipModel() {
		return sid.getChipModel();
	}
	
	public int getPosition() {
		return position;
	}
	public void setPosition(final int position) {
		this.position = position;
	}

	public int[] getBuffer() {
		return buffer;
	}
}