package hardsid_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDChip;
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
	private static final int HARDSID_DELAY_CYCLES = 250;

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			delay();
			context.schedule(event, HARDSID_DELAY_CYCLES, Event.Phase.PHI2);
		}
	};

	private final HardSID4U hardSID;

	private final int deviceID;

	private final int chipNum;

	private ChipModel chipModel;

	public HardSID(EventScheduler context, final HardSID4U hardSID, final int deviceID, final int sid,
			final ChipModel model) {
		super(context);
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.chipNum = sid;
		this.chipModel = model;
	}

	@Override
	public void reset(final byte volume) {
		delay();
		for (int i = 0; i < SIDChip.REG_COUNT; i++) {
			hardSID.HardSID_Write(deviceID, chipNum, i, 0);
			hardSID.HardSID_Delay(deviceID, 4);
		}
		hardSID.HardSID_Flush(deviceID);
		hardSID.HardSID_Reset(deviceID);
	}

	@Override
	public byte read(int addr) {
		clock();
		/*
		 * HardSID4U does not support reads. This works against jsidplay2 faking
		 * to be hardsid, of course. But we should have some way to ask if the
		 * chip can or can't do a proper read operation. For now, it's better
		 * just to do this and maybe get it right, than never do it and always
		 * get it wrong.
		 */
		delay();
		return (byte) hardSID.HardSID_Read(deviceID, chipNum, addr);
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		delay();
		hardSID.HardSID_Write(deviceID, chipNum, addr, data);
	}

	@Override
	public void clock() {
	}

	private void delay() {
		int cycles = clocksSinceLastAccess();
		while (cycles > 0xFFFF) {
			hardSID.HardSID_Delay(deviceID, 0xFFFF);
			cycles -= 0xFFFF;
		}
		if (cycles > 0)
			hardSID.HardSID_Delay(deviceID, cycles);
	}

	protected void lock() {
		reset((byte) 0x0);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	protected void unlock() {
		reset((byte) 0x0);
		context.cancel(event);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
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
