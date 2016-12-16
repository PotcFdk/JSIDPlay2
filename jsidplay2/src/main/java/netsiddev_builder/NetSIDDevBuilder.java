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
import libsidplay.components.pla.PLA;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.SetClocking;
import netsiddev_builder.commands.SetSidLevel;
import netsiddev_builder.commands.SetSidPosition;
import netsiddev_builder.commands.TrySetSampling;
import netsiddev_builder.commands.TrySetSidModel;

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
		client.init((byte) 0xf);
		client.add(new TrySetSampling(audioSection.getSampling()));
		client.add(new TrySetSidModel((byte) sidNum, chipModel, filterName));
		client.add(new SetClocking(cpuClock.getCpuFrequency()));
		for (byte sidNum2 = 0; sidNum2 < PLA.MAX_SIDS; sidNum2++) {
			for (byte voice = 0; voice < (client.getVersion() < 3 ? 3 : 4); voice++) {
				client.add(new Mute(sidNum2, voice, emulationSection.isMuteVoice(sidNum2, voice)));
			}
		}
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
		client.init((byte) 0x0);
		sids.remove(device);
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
		client.softFlush();
	}

	@Override
	public void reset() {
		// nothing to do
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
		float level = -50f + ((volume + 6f) / 12f) * 100f;
		client.add(new SetSidLevel((byte) sidNum, (byte) level));
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		// 0..1 (client)
		// -100..100 (server)
		boolean isMono = sids.size() == 1;
		if (isMono) {
			client.add(new SetSidPosition((byte) sidNum, (byte) 0));
		} else {
			float position = -100f + balance * 200f;
			client.add(new SetSidPosition((byte) sidNum, (byte) position));
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
		client.addAndSend(new Flush());
	}

}
