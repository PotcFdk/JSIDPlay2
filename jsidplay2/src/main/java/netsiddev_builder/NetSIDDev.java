package netsiddev_builder;

import java.util.List;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SamplingMethod;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

public class NetSIDDev extends SIDEmu {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write
	 * commands are routed two both SIDs, while read command can be configured
	 * to be processed by a specific SID chip.
	 * 
	 * @author ken
	 *
	 */
	public static class FakeStereo extends NetSIDDev {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<NetSIDDev> sids;

		public FakeStereo(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model,
				final IConfig config, final List<NetSIDDev> sids) {
			super(context, connection, sidNum, model);
			this.emulationSection = config.getEmulationSection();
			this.prevNum = sidNum - 1;
			this.sids = sids;
		}

		@Override
		public byte read(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				NetSIDDev prevSID = sids.get(prevNum);
				if (prevSID != this)
					return prevSID.read(addr);
			}
			return super.read(addr);
		}

		@Override
		public byte readInternalRegister(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				NetSIDDev prevSID = sids.get(prevNum);
				if (prevSID != this)
					return prevSID.readInternalRegister(addr);
			}
			return super.readInternalRegister(addr);
		}

		@Override
		public void write(int addr, byte data) {
			super.write(addr, data);
			NetSIDDev prevSID = sids.get(prevNum);
			if (prevSID != this)
				prevSID.write(addr, data);
		}
	}

	private byte sidNum;
	private NetSIDConnection connection;
	private ChipModel chipModel;

	public NetSIDDev(EventScheduler context, NetSIDConnection connection, final int sidNum, final ChipModel model) {
		super(context);
		this.connection = connection;
		this.sidNum = (byte) sidNum;
		this.chipModel = model;
		reset((byte) 0xf);
	}

	private final Event event = new Event("NetSIDDev Delay") {

		@Override
		public void event() {
			context.schedule(event, connection.eventuallyDelay(sidNum), Event.Phase.PHI2);
		}
	};

	public void lock() {
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	public void unlock() {
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
		// nothing to do
	}

	@Override
	public void setChipModel(ChipModel model) {
		// is already configured by setFilter()
	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
		connection.setClockFrequency(sidNum, cpuFrequency);
	}

	@Override
	public void input(int input) {
		// configured in the JSIDDevice user interface
	}

	@Override
	public int getInputDigiBoost() {
		// configured in the JSIDDevice user interface
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
		// XXX unsupported by JSIDDevice
	}

	public void setSampling(SamplingMethod sampling) {
		connection.setSampling((byte) sidNum, sampling);
	}

}
