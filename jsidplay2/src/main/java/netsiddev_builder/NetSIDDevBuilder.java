package netsiddev_builder;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class NetSIDDevBuilder implements SIDBuilder {

	private EventScheduler context;
	private IConfig config;

	private List<NetSIDDev> sids = new ArrayList<NetSIDDev>();
	private NetSIDConnection connection;
	private CPUClock cpuClock;

	public NetSIDDevBuilder(EventScheduler context, IConfig config, SidTune tune, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		connection = NetSIDConnection.getInstance(config, tune);
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		final ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		final NetSIDDev sid = new NetSIDDev(context, this, connection, sidNum, chipModel);
		IEmulationSection emulationSection = config.getEmulationSection();
//		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setClockFrequency(cpuClock.getCpuFrequency());
//		sid.setFilter(config, sidNum);
//		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
		sids.add(sid);
		sid.lock();
		return sid;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev sid = (NetSIDDev) device;
		sid.unlock();
		sids.remove(sid);
	}
	
	public int getSidCount() {
		return sids.size();
	}

}
