package netsiddev_builder;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDChip;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class NetSIDDevBuilder implements SIDBuilder, Mixer {

	private EventScheduler context;
	private IConfig config;

	private NetSIDClient client;
	private CPUClock cpuClock;
	private List<NetSIDDev> sids = new ArrayList<>();

	public NetSIDDevBuilder(EventScheduler context, IConfig config, SidTune tune, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		this.client = new NetSIDClient(context, config.getEmulationSection());
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		String filterName = emulationSection.getFilterName(sidNum, Engine.NETSID, Emulation.DEFAULT, chipModel);

		final NetSIDDev sid = createSID(emulationSection, chipModel, sidEmu, tune, sidNum);
		client.setSampling(audioSection.getSampling());
		client.setFilter((byte) sidNum, chipModel, filterName);
		client.setClockFrequency(cpuClock.getCpuFrequency());
		client.addMutes(config.getEmulationSection());
		client.softFlush();
		for (byte addr = 0; sidEmu != null && addr < SIDChip.REG_COUNT; addr++) {
			sid.write(addr, sidEmu.readInternalRegister(addr));
		}
		if (sidNum < sids.size())
			sids.set(sidNum, sid);
		else
			sids.add(sid);
		updateMixer(audioSection);
		return sid;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev sid = (NetSIDDev) device;
		byte sidNum = (byte) sids.indexOf(sid);
		client.flush();
		for (byte reg = 0; reg < SIDChip.REG_COUNT; reg++) {
			client.write(sidNum, reg, (byte) 0);
		}
		sids.remove(sid);
		updateMixer(config.getAudioSection());
	}

	private NetSIDDev createSID(IEmulationSection emulationSection, ChipModel chipModel, SIDEmu sidEmu, SidTune tune,
			int sidNum) {
		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new NetSIDDev.FakeStereo(context, client, sidNum, chipModel, config, sids);
		} else {
			return new NetSIDDev(context, client, sidNum, chipModel);
		}
	}

	private void updateMixer(IAudioSection audioSection) {
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			setVolume(sidNum, audioSection.getVolume(sidNum));
			setBalance(sidNum, audioSection.getBalance(sidNum));
		}
	}

	@Override
	public void reset() {
		client.reset((byte) 0xf);
	}

	@Override
	public void start() {
		client.start();
	}

	@Override
	public void fadeIn(int fadeIn) {
		System.err.println("Fade-in unsupported by network SID client");
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void fadeOut(int fadeOut) {
		System.err.println("Fade-out unsupported by network SID client");
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		// -6db..6db (client)
		// -50..50 (server)
		float level = (((volume + 6f) * 100f) / 12f) - 50f;
		client.setVolume((byte) sidNum, (byte) level);
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		// 0..1 (client)
		// -100..100 (server)
		if (sids.size() == 1) {
			client.setSidPosition((byte) sidNum, (byte) 0);
		} else {
			float position = -100f + balance * 200f;
			client.setSidPosition((byte) sidNum, (byte) position);
		}
	}

	@Override
	public void fastForward() {
		client.fastForward();
	}

	@Override
	public void normalSpeed() {
		client.normalSpeed();
	}

	@Override
	public boolean isFastForward() {
		return client.isFastForward();
	}

	@Override
	public int getFastForwardBitMask() {
		return client.getFastForwardBitMask();
	}

	@Override
	public void pause() {
		client.flush();
	}

}
