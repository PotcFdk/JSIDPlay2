package hardsid_builder;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import resid_builder.resid.ISIDDefs.ChipModel;

/**
 * <pre>
 * *************************************************************************
 *           hardsid.cpp  -  Hardsid support interface.
 *                           Created from Jarnos original
 *                           Sidplay2 patch
 *                           -------------------
 *  begin                : Fri Dec 15 2000
 *  copyright            : (C) 2000-2002 by Simon White
 *  email                : s_a_white@email.com
 * *************************************************************************
 * </pre>
 * 
 * @author Ken Händel
 * 
 */
public class HardSID extends SIDEmu {
	private static final int HARDSID_DELAY_CYCLES = 500;

	/** Number of SID slots */
	public static final int SID_DEVICES = 8;

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			HardSID.this.event();
		}
	};

	private final HsidDLL2 hsid2;

	private final int chipNum;

	private final ChipModel model;

	// Generic variables
	private String m_errorBuffer;

	private boolean m_status;
	private boolean m_locked;

	private int chipsUsed;

	public HardSID(EventScheduler context, final HsidDLL2 hsid2, final int sid,
			final ChipModel model) {
		super(context);
		this.hsid2 = hsid2;
		this.model = model;
		chipNum = sid;

		m_status = false;
		m_locked = false;

		m_errorBuffer = "";
		if (chipNum >= hsid2.HardSID_Devices()) {
			m_errorBuffer = "HARDSID WARNING: System doesn't have enough SID chips.";
			return;
		}

		m_status = true;
		reset((byte) 0);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		if (hsid2.HardSID_Version() >= HardSIDBuilder.HSID_VERSION_204) {
			hsid2.HardSID_Reset2(chipNum, volume);
		} else {
			hsid2.HardSID_Reset(chipNum);
		}
		hsid2.HardSID_Sync(chipNum);

		if (context != null) {
			context.cancel(event);
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		clock();
		/*
		 * Workaround: If real HardSID4U devices are used in addition to the
		 * fake devices -> drop read support. Otherwise HardSID 4U won't play
		 * for some unknown reason.
		 */
		if (hsid2.HardSID_Devices() > SID_DEVICES) {
			return (byte) 0xff;
		}
		/*
		 * HardSID4U does not support reads. This works against jsidplay2 faking
		 * to be hardsid, of course. But we should have some way to ask if the
		 * chip can or can't do a proper read operation. For now, it's better
		 * just to do this and maybe get it right, than never do it and always
		 * get it wrong.
		 */
		int cycles = clocksSinceLastAccess();
		while (cycles > 0xFFFF) {
			hsid2.HardSID_Delay(chipNum, 0xFFFF);
			cycles -= 0xFFFF;
		}

		return (byte) hsid2.HardSID_Read(chipNum, cycles, addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		clock();
		super.write(addr, data);
		int cycles = clocksSinceLastAccess();

		while (cycles > 0xFFFF) {
			hsid2.HardSID_Delay(chipNum, 0xFFFF);
			cycles -= 0xFFFF;
		}

		hsid2.HardSID_Write(chipNum, cycles, addr, data);
	}

	@Override
	public void clock() {
	}

	public final String error() {
		return m_errorBuffer;
	}

	public boolean bool() {
		return m_status;
	}

	@Override
	public void setFilter(final boolean enable) {
		hsid2.HardSID_Filter(chipNum, enable);
	}

	@Override
	public void setEnabled(final int num, final boolean mute) {
		if (hsid2.HardSID_Version() >= HardSIDBuilder.HSID_VERSION_207) {
			hsid2.HardSID_Mute2(chipNum, num, mute, false);
		} else {
			hsid2.HardSID_Mute(chipNum, num, mute);
		}
	}

	// HardSID specific
	public void flush() {
		hsid2.HardSID_Flush(chipNum);
	}

	// Must lock the SID before using the standard functions.
	public boolean lock(final boolean lock) {
		if (!lock) {
			reset((byte) 0);
			if (!m_locked) {
				return false;
			}
			if (hsid2.HardSID_Version() >= HardSIDBuilder.HSID_VERSION_204) {
				hsid2.HardSID_Unlock(chipNum);
			}
			m_locked = false;
			context.cancel(event);
		} else {
			if (m_locked) {
				return false;
			}
			if (hsid2.HardSID_Version() >= HardSIDBuilder.HSID_VERSION_204) {
				// If the player switches to the next song of a tune
				// the device will never get unlocked.
				// Therefore preemptive unlocking here!
				hsid2.HardSID_Unlock(chipNum);
				if (hsid2.HardSID_Lock(chipNum) == false) {
					return false;
				}
			}
			m_locked = true;
			context.cancel(event);
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
		return true;
	}

	protected void event() {
		final int cycles = clocksSinceLastAccess();
		hsid2.HardSID_Delay(chipNum, cycles / chipsUsed);
		context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
	}

	@Override
	public ChipModel getChipModel() {
		return model;
	}

	public void setChipsUsed(int size) {
		chipsUsed = size;
	}
}
