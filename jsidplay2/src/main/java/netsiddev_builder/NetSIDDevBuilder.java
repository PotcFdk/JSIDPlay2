package netsiddev_builder;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;

public class NetSIDDevBuilder implements SIDBuilder {

	private EventScheduler context;
	private IConfig config;

	private List<NetSIDDev> sids = new ArrayList<NetSIDDev>();

	public NetSIDDevBuilder(EventScheduler context, IConfig config) {
		this.context = context;
		this.config = config;
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		final ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		final NetSIDDev impl = new NetSIDDev(context, sidNum, chipModel);
		impl.lock();
		sids.add(impl);
		return impl;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev impl = (NetSIDDev) device;
		impl.unlock();
		sids.remove(impl);
	}

}
