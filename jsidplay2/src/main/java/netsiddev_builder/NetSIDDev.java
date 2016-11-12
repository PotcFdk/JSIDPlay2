package netsiddev_builder;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {
	private byte sidNum;
	private NetSIDConnection connection;
	private ChipModel chipModel;

	public NetSIDDev(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model) {
		super(context);
		this.connection = connection;
		this.sidNum = (byte) sidNum;
		this.chipModel = model;
	}

	private final Event event = new Event("NetSIDDev Delay") {

		@Override
		public void event() {
			context.schedule(event, connection.eventuallyDelay(sidNum), Event.Phase.PHI2);
		}
	};

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
		connection.reset(sidNum, (byte) volume);
	}

	@Override
	public byte read(int addr) {
		clock();
		return connection.read(sidNum, (byte) addr);
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		connection.write(sidNum, (byte) addr, data);
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
	public void setVoiceMute(int voice, boolean mute) {
		connection.mute(sidNum, (byte) voice, mute);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		IEmulationSection emulationSection = config.getEmulationSection();
		switch (chipModel) {
		case MOS6581:
			String filterName6581 = emulationSection.getFilterName(sidNum, Emulation.RESIDFP, ChipModel.MOS6581);
			connection.setFilter((byte) sidNum, filterName6581);
			break;
		case MOS8580:
			String filterName8580 = emulationSection.getFilterName(sidNum, Emulation.RESIDFP, ChipModel.MOS8580);
			connection.setFilter((byte) sidNum, filterName8580);
			break;
		default:
			throw new RuntimeException("Unknown SID chip model: " + chipModel);
		}
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
	}

}
