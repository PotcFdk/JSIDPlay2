package hardsid_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SamplingMethod;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

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
	private static final int HSID_VERSION_MIN = 0x0200;
	private static final int HSID_VERSION_204 = 0x0204;
	private static final int HSID_VERSION_207 = 0x0207;

	private static final int HARDSID_DELAY_CYCLES = 500;

	/** Number of SID slots */
	public static final int SID_DEVICES = 8;

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			final int cycles = clocksSinceLastAccess();
			hsidDll.HardSID_Delay(chipNum,
					cycles / hardSIDBuilder.getDevcesInUse());
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
	};

	private final HsidDLL2 hsidDll;

	private final int chipNum;

	private ChipModel chipModel;

	private boolean locked;

	private HardSIDBuilder hardSIDBuilder;

	public HardSID(HardSIDBuilder hardSIDBuilder, EventScheduler context,
			final HsidDLL2 hsidDll, final int sid, final ChipModel model) {
		super(context);
		this.hardSIDBuilder = hardSIDBuilder;
		this.hsidDll = hsidDll;
		this.chipNum = sid;
		this.chipModel = model;
		reset((byte) 0xf);
	}

	@Override
	@SuppressWarnings("deprecation")
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		if (hsidDll.HardSID_Version() >= HSID_VERSION_204) {
			hsidDll.HardSID_Reset2(chipNum, volume);
			for (int channel = 0; channel < 3; channel++) {
				hsidDll.HardSID_Mute2(chipNum, channel, volume == 0, true);
			}
		} else {
			hsidDll.HardSID_Reset(chipNum);
			for (int channel = 0; channel < 3; channel++) {
				hsidDll.HardSID_Mute(chipNum, channel, volume == 0);
			}
		}
		hsidDll.HardSID_Sync(chipNum);

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
		if (hsidDll.HardSID_Devices() > SID_DEVICES) {
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
			hsidDll.HardSID_Delay(chipNum, 0xFFFF);
			cycles -= 0xFFFF;
		}

		return (byte) hsidDll.HardSID_Read(chipNum, cycles, addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		clock();
		super.write(addr, data);
		int cycles = clocksSinceLastAccess();

		while (cycles > 0xFFFF) {
			hsidDll.HardSID_Delay(chipNum, 0xFFFF);
			cycles -= 0xFFFF;
		}
		hsidDll.HardSID_Write(chipNum, cycles, addr, data);
	}

	@Override
	public void clock() {
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable = emulation.isFilterEnable(sidNum);
		hsidDll.HardSID_Filter(chipNum, enable);
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
		if (hsidDll.HardSID_Version() >= HSID_VERSION_207) {
			hsidDll.HardSID_Mute2(chipNum, num, mute, false);
		} else {
			hsidDll.HardSID_Mute(chipNum, num, mute);
		}
	}

	// HardSID specific
	void flush() {
		hsidDll.HardSID_Flush(chipNum);
	}

	// Must lock the SID before using the standard functions.
	boolean lock(final boolean lock) {
		if (!lock && !locked || lock && locked) {
			return false;
		}
		if (!lock) {
			if (hsidDll.HardSID_Version() >= HSID_VERSION_204) {
				hsidDll.HardSID_Unlock(chipNum);
			}
			context.cancel(event);
		} else {
			// Check major version
			if (hsidDll.HardSID_Version() >> 8 < HSID_VERSION_MIN >> 8) {
				throw new RuntimeException(String.format(
						"HARDSID ERROR: HardSID.dll not V%d",
						HSID_VERSION_MIN >> 8));
			}
			// Check minor version
			if (hsidDll.HardSID_Version() < HSID_VERSION_MIN) {
				throw new RuntimeException(
						String.format(
								"HARDSID ERROR: HardSID.dll must be V%02d.%02d or greater",
								HSID_VERSION_MIN >> 8, HSID_VERSION_MIN & 0xff));
			}
			if (hsidDll.HardSID_Version() >= HSID_VERSION_204) {
				// If the player switches to the next song of a tune
				// the device will never get unlocked.
				// Therefore preemptive unlocking here!
				hsidDll.HardSID_Unlock(chipNum);
				if (hsidDll.HardSID_Lock(chipNum) == false) {
					return false;
				}
			}
			context.cancel(event);
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
		locked = lock;
		return true;
	}

	@Override
	public ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public void setChipModel(final ChipModel model) {
		System.err
				.println("HardSID WARNING: SID model cannot be changed on the fly!");
	}

	@Override
	public void setSampling(double cpuFrequency, float frequency,
			SamplingMethod sampling) {
	}

	@Override
	public void input(int input) {
	}

	@Override
	public int getInputDigiBoost() {
		return 0;
	}

	public static final String credits() {
		return "HardSID V1.0.1 Engine:\n"
				+ "\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
	}

}
