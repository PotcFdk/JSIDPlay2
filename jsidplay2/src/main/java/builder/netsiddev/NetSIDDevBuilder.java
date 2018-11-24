package builder.netsiddev;

import static libsidplay.sidtune.SidTune.RESET;
import static libsidplay.common.SIDChip.REG_COUNT;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.ArrayList;
import java.util.List;

import builder.netsiddev.commands.Flush;
import builder.netsiddev.commands.Mute;
import builder.netsiddev.commands.SetClocking;
import builder.netsiddev.commands.SetDelay;
import builder.netsiddev.commands.SetSidLevel;
import builder.netsiddev.commands.SetSidPosition;
import builder.netsiddev.commands.SetTuneHeader;
import builder.netsiddev.commands.TrySetSampling;
import builder.netsiddev.commands.TrySetSidModel;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioDriver;

public class NetSIDDevBuilder implements SIDBuilder, Mixer {

	private final IConfig config;

	private final CPUClock cpuClock;
	private final NetSIDClient client;
	private final List<NetSIDDev> sids = new ArrayList<>();

	public NetSIDDevBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
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
		if (client.getVersion() > 3 && tune != RESET && tune.getTuneHeader() != null) {
			client.add(new SetTuneHeader(tune.getTuneHeader()));
		}
		client.add(new TrySetSampling(audioSection.getSampling()));
		client.add(new TrySetSidModel((byte) sidNum, chipModel, filterName));
		client.add(new SetClocking(cpuClock.getCpuFrequency()));
		for (byte sidNum2 = 0; sidNum2 < MAX_SIDS; sidNum2++) {
			for (byte voice = 0; voice < (client.getVersion() < 3 ? 3 : 4); voice++) {
				client.add(new Mute(sidNum2, voice, emulationSection.isMuteVoice(sidNum2, voice)));
			}
		}
		client.softFlush();
		for (byte addr = 0; sidEmu != null && addr < REG_COUNT; addr++) {
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
		try {
			updateMixer(config.getAudioSection());
		} catch (RuntimeException e) {
			// socket closed? Ignore, because don't prevent unlocking other SIDs
		}
	}

	private NetSIDDev createSID(IEmulationSection emulationSection, ChipModel chipModel, SIDEmu sidEmu, SidTune tune,
			int sidNum) {
		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new NetSIDDev.FakeStereo(client, sidNum, chipModel, config, sids);
		} else {
			return new NetSIDDev(client, sidNum, chipModel);
		}
	}

	private void updateMixer(IAudioSection audioSection) {
		for (int sidNum = 0; sidNum < sids.size(); sidNum++) {
			setVolume(sidNum, audioSection.getVolume(sidNum));
			setBalance(sidNum, audioSection.getBalance(sidNum));
			setDelay(sidNum, audioSection.getDelay(sidNum));
		}
		client.softFlush();
	}

	@Override
	public void setAudioDriver(AudioDriver audioDriver) {
		// unused, since mixing is done on the server side
	}

	@Override
	public void start() {
		client.start();
	}

	@Override
	public void fadeIn(double fadeIn) {
		System.err.println("Fade-in unsupported by network SID client");
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void fadeOut(double fadeOut) {
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
	public void setDelay(int sidNum, int delay) {
		if (client.getVersion() > 3) {
			client.add(new SetDelay((byte) sidNum, (byte) delay));
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
