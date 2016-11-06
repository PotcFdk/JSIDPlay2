package netsiddev_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {
	private byte sidNum;
	private NetSIDConnection connection;

	public NetSIDDev(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model) {
		super(context);
		this.connection = connection;
		this.sidNum = (byte) sidNum;
	}

	public void lock() {
		reset((byte) 0xf);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	public void unlock() {
		reset((byte) 0xf);
		context.cancel(event);
	}

	@Override
	public void reset(byte volume) {
		connection.flush(sidNum);
		connection.reset(sidNum, (byte) volume);
	}

	@Override
	public byte read(int addr) {
		clock();
		return (byte) 0xff;
	}

	private final Event event = new Event("NetSIDDev Delay") {

		@Override
		public void event() {
			final long now = context.getTime(Event.Phase.PHI2);
			int diff = (int) (now - lastTime);
			if (diff > 0xFFFF) {
				lastTime += 0xFFFF;
				connection.delay(sidNum, (byte) 0xFFFF);
			}
			context.schedule(event, 0xFFFF, Event.Phase.PHI2);
		}
	};

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		connection.addWrite(sidNum, clocksSinceLastAccess(), (byte) addr, data);
	}

	@Override
	public void clock() {
	}

	@Override
	public void setChipModel(ChipModel model) {
	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
		connection.setClockFrequency(sidNum, cpuFrequency);
	}

	@Override
	public void input(int input) {
	}

	@Override
	public int getInputDigiBoost() {
		return 0;
	}

	@Override
	public void setVoiceMute(int num, boolean mute) {
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
	}

}
