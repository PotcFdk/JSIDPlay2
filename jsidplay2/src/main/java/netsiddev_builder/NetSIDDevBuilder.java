package netsiddev_builder;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
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

public class NetSIDDevBuilder implements SIDBuilder, Mixer {

	private EventScheduler context;
	private IConfig config;

	private NetSIDConnection connection;
	private CPUClock cpuClock;
	private List<NetSIDDev> sids = new ArrayList<>();

	public NetSIDDevBuilder(EventScheduler context, IConfig config, SidTune tune, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		this.connection = new NetSIDConnection(context);
	}

	@Override
	public SIDEmu lock(SIDEmu sidEmu, int sidNum, SidTune tune) {
		IEmulationSection emulationSection = config.getEmulationSection();
		final NetSIDDev sid = createSID(emulationSection, sidEmu, tune, sidNum);
		sid.setSampling(config.getAudioSection().getSampling());
		sid.setChipModel(ChipModel.getChipModel(emulationSection, tune, sidNum));
		sid.setFilter(config, sidNum);
		sid.setFilterEnable(emulationSection, sidNum);
		sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
		// this triggers refreshParams on the server side, therefore the last:
		sid.setClockFrequency(cpuClock.getCpuFrequency());
		for (int voice = 0; voice < (connection.VERSION < 3 ? 3 : 4); voice++) {
			sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
		}
		for (int i = 0; sidEmu != null && i < SIDChip.REG_COUNT; i++) {
			sid.write(i, sidEmu.readInternalRegister(i));
		}
		if (sidNum < sids.size())
			sids.set(sidNum, sid);
		else
			sids.add(sid);
		updateMixer(config.getAudioSection());
		return sid;
	}

	@Override
	public void unlock(SIDEmu device) {
		NetSIDDev sid = (NetSIDDev) device;
		sids.remove(sid);
		updateMixer(config.getAudioSection());
		connection.flush();
	}

	private void updateMixer(IAudioSection audioSection) {
		for (int i = 0; i < sids.size(); i++) {
			setVolume(i, audioSection.getVolume(i));
			setBalance(i, audioSection.getBalance(i));
		}
	}

	@Override
	public void reset() {
		connection.reset((byte) 0xf);
	}

	@Override
	public void start() {
		connection.start();
	}

	@Override
	public void fadeIn(int fadeIn) {
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void fadeOut(int fadeOut) {
		// XXX unsupported by JSIDDevice
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		// XXX magic formula, maybe wrong!
		float vol = 5 * (volume + (PLA.MAX_SIDS - sids.size()) * PLA.MAX_SIDS) / sids.size();
		connection.setVolume((byte) sidNum, (byte) vol);
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		connection.setBalance((byte) sidNum, balance);
	}

	@Override
	public void fastForward() {
		connection.fastForward();
	}

	@Override
	public void normalSpeed() {
		connection.normalSpeed();
	}

	@Override
	public boolean isFastForward() {
		return connection.isFastForward();
	}

	@Override
	public int getFastForwardBitMask() {
		return connection.getFastForwardBitMask();
	}

	@Override
	public void pause() {
		connection.flush();
	}

	/**
	 * Create NetworkSIDDevice, formerly used NetworkSIDDevice is removed
	 * beforehand.
	 * 
	 * @param oldSIDEmu
	 *            currently used NetworkSIDDevice
	 * @param tune
	 *            current tune
	 * @param sidNum
	 *            current SID number
	 * 
	 * @return new NetworkSIDDevice
	 */
	private NetSIDDev createSID(IEmulationSection emulationSection, SIDEmu sidEmu, SidTune tune, int sidNum) {
		final ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new NetSIDDev.FakeStereo(context, connection, sidNum, chipModel, config, sids);
		} else {
			return new NetSIDDev(context, connection, sidNum, chipModel);
		}
	}

}
