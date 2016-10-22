package hardsid_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

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
			final int cycles = clocksSinceLastAccess();
			hsidDll.HardSID_Delay(chipNum, cycles / hardSIDBuilder.getSIDCount());
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
	};

	private final HsidDLL2 hsidDll;

	private final int chipNum;

	private ChipModel chipModel;

	private HardSIDBuilder hardSIDBuilder;

	public HardSID(HardSIDBuilder hardSIDBuilder, EventScheduler context, final HsidDLL2 hsidDll, final int sid,
			final ChipModel model) {
		super(context);
		this.hardSIDBuilder = hardSIDBuilder;
		this.hsidDll = hsidDll;
		this.chipNum = sid;
		this.chipModel = model;
		reset((byte) 0xf);
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		hsidDll.HardSID_Reset2(chipNum, volume);
	}

	@Override
	public byte read(int addr) {
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

	protected void lock() {
		hsidDll.HardSID_Lock(chipNum);
		reset((byte) 0x0);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	protected void unlock() {
		reset((byte) 0x0);
		hsidDll.HardSID_Unlock(chipNum);
		context.cancel(event);
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
		hsidDll.HardSID_Mute2(chipNum, num, mute, false);
	}

	protected ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public void setChipModel(final ChipModel model) {
	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
	}

	@Override
	public void input(int input) {
	}

	@Override
	public int getInputDigiBoost() {
		return 0;
	}

	public static final String credits() {
		return "HardSID V1.0.1 Engine:\n" + "\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
	}

}
