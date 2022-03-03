package builder.jhardsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.ArrayList;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.HardwareSIDBuilder;
import libsidplay.common.Mixer;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioDriver;

/**
 *
 * Support of HARDSID USB devices like HardSID4U.
 * 
 * @author Ken Händel
 *
 */
public class JHardSIDBuilder implements HardwareSIDBuilder, Mixer {

	private static final short REGULAR_DELAY = 512;

	/**
	 * System event context.
	 */
	private EventScheduler context;

	/**
	 * Configuration
	 */
	private IConfig config;

	/**
	 * CPU clock.
	 */
	private CPUClock cpuClock;

	private final HardSIDUSB hardSID;

	/**
	 * Number of HardSID devices.
	 */
	private static int deviceCount;

	/**
	 * Number of SIDs of the first HardSID device.
	 */
	private static int chipCount;

	/**
	 * Device number. If more devices are connected, we use just the first one.
	 */
	private byte deviceID;

	/**
	 * Already used HardSIDs.
	 */
	private List<JHardSIDEmu> sids = new ArrayList<>();

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public JHardSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		hardSID = new HardSIDUSB();
		hardSID.hardsid_usb_init(true, SysMode.SIDPLAY);
		deviceCount = hardSID.hardsid_usb_getdevcount();
		chipCount = hardSID.hardsid_usb_getsidcount(deviceID);
	}

	@Override
	public void destroy() {
		hardSID.hardsid_usb_abortplay(deviceID);
		hardSID.hardsid_usb_close();
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		ChipModel defaultSidModel = emulationSection.getDefaultSidModel();

		Integer chipNum = getModelDependantChipNum(chipModel, sidNum);

		if (oldHardSID != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return oldHardSID;
		}
		if (deviceID < deviceCount && chipNum != null && chipNum < chipCount) {
			JHardSIDEmu sid = createSID(deviceID, chipNum, sidNum, tune, chipModel, defaultSidModel);

			sid.lock();
			sid.setFilterEnable(emulationSection, sidNum);
			sid.setDigiBoost(emulationSection.isDigiBoosted8580());
			for (int voice = 0; voice < 4; voice++) {
				sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
			}
			sids.add(sid);
			setDeviceName(sidNum, "HardSID");
			setDelay(sidNum, audioSection.getDelay(sidNum));
			return sid;
		}
		System.err.printf("HARDSID ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		JHardSIDEmu hardSid = (JHardSIDEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	@Override
	public int getDeviceCount() {
		return chipCount;
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return sidNum < sids.size() ? Integer.valueOf(sids.get(sidNum).getChipNum()) : null;
	}

	@Override
	public String getDeviceName(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getDeviceName() : null;
	}

	public void setDeviceName(int sidNum, String serialNo) {
		if (sidNum < sids.size()) {
			sids.get(sidNum).setDeviceName(serialNo);
		}
	}

	@Override
	public ChipModel getDeviceChipModel(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getChipModel() : null;
	}

	@Override
	public void setAudioDriver(AudioDriver audioDriver) {
	}

	@Override
	public void start() {
	}

	@Override
	public void fadeIn(double fadeIn) {
		System.err.println("Fade-in unsupported by HardSID4U");
		// XXX unsupported by HardSID4U
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by HardSID4U");
		// XXX unsupported by HardSID4U
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by HardSID4U");
		// XXX unsupported by HardSID4U
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by HardSID4U");
		// XXX unsupported by HardSID4U
	}

	public int getDelay(int sidNum) {
		return delayInCycles[sidNum];
	}

	@Override
	public void setDelay(int sidNum, int delay) {
		delayInCycles[sidNum] = (int) (cpuClock.getCpuFrequency() / 1000. * delay);
	}

	@Override
	public void fastForward() {
		fastForwardFactor++;
	}

	@Override
	public void normalSpeed() {
		fastForwardFactor = 0;
	}

	@Override
	public boolean isFastForward() {
		return fastForwardFactor != 0;
	}

	@Override
	public int getFastForwardBitMask() {
		return (1 << fastForwardFactor) - 1;
	}

	@Override
	public void pause() {
		while (hardSID.hardsid_usb_flush(deviceID) == WState.BUSY) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private JHardSIDEmu createSID(byte deviceId, int chipNum, int sidNum, SidTune tune, ChipModel chipModel,
			ChipModel defaultChipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new JHardSIDEmu.FakeStereo(this, context, cpuClock, hardSID, deviceId, chipNum, sidNum, chipModel,
					defaultChipModel, sids, emulationSection);
		} else {
			return new JHardSIDEmu(this, context, cpuClock, hardSID, deviceId, chipNum, sidNum, chipModel,
					defaultChipModel);
		}
	}

	/**
	 * Get HardSID device index based on the desired chip model.
	 *
	 * @param chipModel desired chip model
	 * @param sidNum    current SID number
	 * @return SID index of the desired HardSID device
	 */
	private Integer getModelDependantChipNum(final ChipModel chipModel, int sidNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();

		// use next free slot (prevent wrong type)
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (!isChipNumAlreadyUsed(chipNum) && isChipModelMatching(chipModel, chipNum)) {
				return chipNum;
			}
		}
		// Nothing matched? use next free slot
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (chipCount > 2 && (chipNum == sid6581 || chipNum == sid8580)) {
				// more SIDs available than configured? still skip wrong type
				continue;
			}
			if (!isChipNumAlreadyUsed(chipNum)) {
				return chipNum;
			}
		}
		// no slot left
		return null;
	}

	private boolean isChipModelMatching(final ChipModel chipModel, int chipNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();

		return chipNum == sid6581 && chipModel == ChipModel.MOS6581
				|| chipNum == sid8580 && chipModel == ChipModel.MOS8580;
	}

	private boolean isChipNumAlreadyUsed(final int chipNum) {
		return sids.stream().filter(sid -> chipNum == sid.getChipNum()).findFirst().isPresent();
	}

	int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return diff >> fastForwardFactor;
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY) {
			lastSIDWriteTime += REGULAR_DELAY;

			while (hardSID.hardsid_usb_delay(deviceID, REGULAR_DELAY >> fastForwardFactor) == WState.BUSY) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return REGULAR_DELAY;
	}

}
