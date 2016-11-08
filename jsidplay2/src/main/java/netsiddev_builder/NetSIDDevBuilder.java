package netsiddev_builder;

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

	private NetSIDConnection connection;
	private CPUClock cpuClock;

	public NetSIDDevBuilder(EventScheduler context, IConfig config, SidTune tune, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		connection = new NetSIDConnection(context, config, tune);
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		if (sidEmu != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return sidEmu;
		}
		final ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		final NetSIDDev sid = new NetSIDDev(context, connection, sidNum, chipModel);
		IEmulationSection emulationSection = config.getEmulationSection();
//		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setClockFrequency(cpuClock.getCpuFrequency());
		sid.setFilter(config, sidNum);
//		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
		for (int voice = 0; voice < 4; voice++) {
			sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
		}
		sid.lock();
		return sid;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev impl = (NetSIDDev) device;
		impl.unlock();
	}

}
