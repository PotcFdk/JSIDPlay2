package netsiddev_builder;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDChip;
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
			unlock(sidEmu);
		}
		final ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		final NetSIDDev sid = new NetSIDDev(context, connection, sidNum, chipModel);
		IEmulationSection emulationSection = config.getEmulationSection();
		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setFilter(config, sidNum);
		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
		// this triggers refreshParams on the server side, therefore the last!
		sid.setClockFrequency(cpuClock.getCpuFrequency());
		for (int voice = 0; voice < 4; voice++) {
			sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
		}
		for (int i = 0; sidEmu != null && i < SIDChip.REG_COUNT; i++) {
			sid.write(i, sidEmu.readInternalRegister(i));
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
