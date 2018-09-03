package builder.sidblaster;

import static builder.sidblaster.HardSID.HSID_USB_WSTATE_BUSY;
import static builder.sidblaster.SidBlasterBuilder.SHORTEST_DELAY;
import static libsidplay.common.SIDChip.REG_COUNT;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

/**
 *
 * @author Ken Händel
 * 
 */
public class SIDBlasterEmu extends SIDEmu {

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			context.schedule(event, hardSIDBuilder.eventuallyDelay(), Event.Phase.PHI2);
		}
	};

	private EventScheduler context;

	private final HardSID hardSID;

	private final byte deviceID;

	private ChipModel chipModel;

	private SidBlasterBuilder hardSIDBuilder;

	public SIDBlasterEmu(EventScheduler context, SidBlasterBuilder hardSIDBuilder, final HardSID hardSID,
			final byte deviceID, final byte sid, final ChipModel model) {
		this.context = context;
		this.hardSIDBuilder = hardSIDBuilder;
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.chipModel = model;
	}

	@Override
	public void reset(final byte volume) {
		hardSID.HardSID_Reset(deviceID);
		for (byte reg = 0; reg < REG_COUNT; reg++) {
			hardSID.HardSID_Try_Write(deviceID, SHORTEST_DELAY, reg, (byte) 0);
		}
		hardSID.HardSID_Try_Write(deviceID, SHORTEST_DELAY, (byte) 0xf, volume);
		hardSID.HardSID_Flush(deviceID);
		hardSID.HardSID_Sync(deviceID);
	}

	@Override
	public byte read(int addr) {
		clock();
		hardSID.HardSID_Delay(deviceID, (short) hardSIDBuilder.clocksSinceLastAccess());
		return hardSID.ReadFromHardSID(deviceID, (byte) addr);
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		while (hardSID.HardSID_Try_Write(deviceID, (short) hardSIDBuilder.clocksSinceLastAccess(), (byte) addr,
				data) == HSID_USB_WSTATE_BUSY)
			Thread.yield();
	}

	@Override
	public void clock() {
	}

	protected boolean lock() {
		boolean locked = hardSID.HardSID_Lock(deviceID);
		if (locked) {
			reset((byte) 0xf);
			context.schedule(event, 0, Event.Phase.PHI2);
		}
		return locked;
	}

	protected void unlock() {
		reset((byte) 0x0);
		context.cancel(event);
		hardSID.HardSID_Unlock(deviceID);
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
