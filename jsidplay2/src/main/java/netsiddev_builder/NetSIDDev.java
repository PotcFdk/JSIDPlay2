package netsiddev_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {
	private static final int DELAY_CYCLES = 250;

	private int sidNum;
	private NetSIDConnection connection;

	private final Event event = new Event("NetSIDDev Delay") {
		@Override
		public void event() {
			int cycles = delay();
			if (cycles > 0)
				connection.delay(sidNum, cycles);
			context.schedule(event, DELAY_CYCLES, Event.Phase.PHI2);
		}
	};

	private int delay() {
		int cycles = clocksSinceLastAccess();
		while (cycles > 0xFFFF) {
			connection.delay(sidNum, 0xFFFF);
			cycles -= 0xFFFF;
		}
		return cycles;
	}

	public NetSIDDev(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model) {
		super(context);
		this.connection = connection;
		this.sidNum = sidNum;
	}

	public void lock() {
		reset((byte) 0xf);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	public void unlock() {
		reset((byte) 0x0);
		context.cancel(event);
	}

	@Override
	public void reset(byte volume) {
		connection.flush(sidNum);

		connection.reset(sidNum, (byte) 0);

	}

	@Override
	public byte read(int addr) {
		return (byte) 0xff;
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		int cycles = delay();
		connection.addWrite(sidNum, cycles, (byte) addr, data);
	}

	@Override
	public void clock() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setChipModel(ChipModel model) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
		// TODO Auto-generated method stub

	}

	@Override
	public void input(int input) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getInputDigiBoost() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setVoiceMute(int num, boolean mute) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		// TODO Auto-generated method stub

	}

}
