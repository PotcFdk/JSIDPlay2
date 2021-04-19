package builder.hardsid;

import static libsidplay.common.Engine.HARDSID;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jna.DefaultTypeMapper;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.platform.EnumConverter;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.HardwareSIDBuilder;
import libsidplay.common.Mixer;
import libsidplay.common.SIDEmu;
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
public class HardSIDBuilder implements HardwareSIDBuilder, Mixer {

	private static final short REGULAR_DELAY = 128;

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

	/**
	 * Native library wrapper.
	 */
	private static HardSID hardSID;

	/**
	 * Number of HardSID devices.
	 */
	private static int deviceCount;

	/**
	 * Number of SIDs of the first HardSID device.
	 */
	private static int chipCount;

	/**
	 * Already used HardSIDs.
	 */
	private List<HardSIDEmu> sids = new ArrayList<>();

	/**
	 * Device number. If more devices are connected, we use just the first one.
	 */
	private byte deviceID;

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public HardSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (hardSID == null) {
			try {
				hardSID = Native.load("hardsid_usb", HardSID.class, createOptions());
				init();
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Error: Windows is required to use " + HARDSID + " soundcard!");
				throw e;
			}
		}
	}

	private Map<String, Object> createOptions() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(Library.OPTION_TYPE_MAPPER, new DefaultTypeMapper() {
			{
				addTypeConverter(WState.class, new EnumConverter<WState>(WState.class));
				addTypeConverter(SysMode.class, new EnumConverter<SysMode>(SysMode.class));
				addTypeConverter(DeviceType.class, new EnumConverter<DeviceType>(DeviceType.class));
			}
		});
		return options;
	}

	private void init() {
		deviceCount = hardSID.hardsid_usb_getdevcount();
		chipCount = hardSID.hardsid_usb_getsidcount(deviceID);
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		Integer chipNum = getModelDependantChipNum(chipModel, sidNum);
		if (deviceID < deviceCount && chipNum != null && chipNum < chipCount) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			HardSIDEmu hsid = createSID(deviceID, chipNum, sidNum, tune, chipModel);
			hsid.lock();
			sids.add(hsid);
			setDelay(sidNum, config.getAudioSection().getDelay(sidNum));
			return hsid;
		}
		System.err.printf("HARDSID ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);
		return SIDEmu.NONE;
	}

	private HardSIDEmu createSID(byte deviceId, int chipNum, int sidNum, SidTune tune, ChipModel chipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new HardSIDEmu.FakeStereo(this, context, hardSID, deviceId, chipNum, sidNum, chipModel, sids,
					emulationSection);
		} else {
			return new HardSIDEmu(this, context, hardSID, deviceId, chipNum, sidNum, chipModel);
		}
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		HardSIDEmu hardSid = (HardSIDEmu) sidEmu;
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
		return null;
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
		while (hardSID.hardsid_usb_flush(deviceID) == WState.WSTATE_BUSY) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
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

			while (hardSID.hardsid_usb_delay(deviceID,
					(short) (REGULAR_DELAY >> fastForwardFactor)) == WState.WSTATE_BUSY) {
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
