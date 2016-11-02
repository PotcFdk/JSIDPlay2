package netsiddev_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {
	private int sidNum;
	private NetSIDConnection connection;

//	private final Event event = new Event("NetSIDDev Delay") {
//
//		@Override
//		public void event() {
////			context.schedule(event, NetSIDConnection.CMD_BUFFER_SIZE >> 1, Event.Phase.PHI2);
//		}
//	};
//
	public NetSIDDev(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model) {
		super(context);
		this.connection = connection;
		this.sidNum = sidNum;
		reset((byte) 0xf);
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

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		int cycles = delay(clocksSinceLastAccess());
		connection.addWrite(0, cycles>>1, (byte) addr, data);
		connection.addWrite(1, cycles>>1, (byte) addr, data);
//		connection.addWrite(sidNum, cycles, (byte) addr, data);
	}

	private int delay(int cycles) {
		while (cycles > 0xFFFFFF) {
			connection.delay(sidNum, 0xFFFFFF);
			cycles -= 0xFFFFFF;
		}
		return (int) cycles;
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
		connection.setClockFrequency(sidNum, cpuFrequency);
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
