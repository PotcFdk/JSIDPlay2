package builder.hardsid;

import static builder.hardsid.HardSIDBuilder.SHORTEST_DELAY;
import static libsidplay.common.SIDChip.REG_COUNT;

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
public class HardSIDEmu extends SIDEmu {

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			context.schedule(event, hardSIDBuilder.eventuallyDelay(), Event.Phase.PHI2);
		}
	};

	private EventScheduler context;

	private final HardSID hardSID;

	private final byte deviceID;

	private final byte sidNum;

	private ChipModel chipModel;

	private HardSIDBuilder hardSIDBuilder;

	public HardSIDEmu(EventScheduler context, HardSIDBuilder hardSIDBuilder, final HardSID hardSID, final byte deviceID,
			final int sidNum, final ChipModel model) {
		this.context = context;
		this.hardSIDBuilder = hardSIDBuilder;
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.sidNum = (byte) sidNum;
		this.chipModel = model;
	}

	@Override
	public void reset(final byte volume) {
		hardSID.HardSID_Reset(deviceID);
		for (byte reg = 0; reg < REG_COUNT; reg++) {
			hardSID.HardSID_Delay(deviceID, SHORTEST_DELAY);
			hardSID.HardSID_Write(deviceID, sidNum, reg, (byte) 0);
		}
		hardSID.HardSID_Delay(deviceID, SHORTEST_DELAY);
		hardSID.HardSID_Write(deviceID, sidNum, (byte) 0xf, volume);
		hardSID.HardSID_Flush(deviceID);
	}

	@Override
	public byte read(int addr) {
		clock();
		// not supported by HardSID4U!
		return (byte) 0xff;
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		hardSID.HardSID_Write(deviceID, sidNum, (byte) addr, data);
	}

	@Override
	public void clock() {
		hardSID.HardSID_Delay(deviceID, (short) hardSIDBuilder.clocksSinceLastAccess());
	}

	protected void lock() {
		reset((byte) 0xf);
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

	public byte getSidNum() {
		return sidNum;
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
