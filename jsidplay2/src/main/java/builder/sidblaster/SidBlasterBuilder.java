package builder.sidblaster;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static libsidplay.common.Engine.SIDBLASTER;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;

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
 * @author Ken Händel
 * 
 */
public class SidBlasterBuilder implements HardwareSIDBuilder, Mixer {

	private static final short REGULAR_DELAY = 4096;

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
	 * Already used SIDBlaster SIDs.
	 */
	private List<SIDBlasterEmu> sids = new ArrayList<SIDBlasterEmu>();

	/**
	 * Device number, if more than one USB devices is connected.
	 */
	private byte deviceID;

	protected long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public SidBlasterBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (hardSID == null) {
			try {
				hardSID = (HardSID) Native.loadLibrary("hardsid", HardSID.class);
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Error: Windows is required to use " + SIDBLASTER + " soundcard!");
				throw e;
			}
		}
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);

		Integer deviceId = getModelDependantDeviceId(chipModel, sidNum);
		if (deviceId != null && deviceId < hardSID.HardSID_Devices()) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			SIDBlasterEmu hsid = createSID(deviceId.byteValue(), sidNum, tune,
					emulationSection.getSidBlasterModel(deviceId));
			if (hsid.lock()) {
				sids.add(hsid);
				setDelay(sidNum, audioSection.getDelay(sidNum));
				return hsid;
			}
		}
		System.err.println(/* throw new RuntimeException( */
				String.format("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)",
						sidNum));
		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			// Fake stereo chip not available? Re-use original chip
			return oldHardSID;
		}
		return SIDEmu.NONE;
	}

	private SIDBlasterEmu createSID(byte deviceId, int sidNum, SidTune tune, ChipModel chipModel) {
		if (SidTune.isFakeStereoSid(config.getEmulationSection(), tune, sidNum)) {
			return new SIDBlasterEmu.FakeStereo(context, config, this, hardSID, deviceId, sidNum, chipModel, sids);
		} else {
			return new SIDBlasterEmu(context, this, hardSID, deviceId, sidNum, chipModel);
		}
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		SIDBlasterEmu hardSid = (SIDBlasterEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	@Override
	public int getDeviceCount() {
		return hardSID != null ? hardSID.HardSID_Devices() : null;
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return sidNum < sids.size() ? Integer.valueOf(sids.get(sidNum).getDeviceId()) : null;
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
		System.err.println("Fade-in unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	public int getDelayInCycles(int sidNum) {
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
		hardSID.HardSID_Flush(deviceID);
	}

	/**
	 * Get SIDBlaster device index based on the desired chip model.
	 * 
	 * @param chipModel desired chip model
	 * @param sidNum    current SID number
	 * @return SID index of the desired SIDBlaster device
	 */
	private Integer getModelDependantDeviceId(final ChipModel chipModel, int sidNum) {
		ChipModel device0Model = config.getEmulationSection().getSidBlaster0Model();
		ChipModel device1Model = config.getEmulationSection().getSidBlaster1Model();
		ChipModel device2Model = config.getEmulationSection().getSidBlaster2Model();
		if (sidNum == 0) {
			// Mono SID: choose according to the chip model type
			if (device0Model == chipModel && 0 < hardSID.HardSID_Devices()) {
				return 0;
			}
			if (device1Model == chipModel && 1 < hardSID.HardSID_Devices()) {
				return 1;
			}
			if (device2Model == chipModel && 2 < hardSID.HardSID_Devices()) {
				return 2;
			}
		} else {
			// Stereo or 3-SID: use next free slot (prevent already used one and wrong type)
			for (int sidBlasterIdx = 0; sidBlasterIdx < hardSID.HardSID_Devices(); sidBlasterIdx++) {
				final int theSidBlasterIdx = sidBlasterIdx;
				if (sids.stream().filter(sid -> theSidBlasterIdx == sid.getDeviceId()).findFirst().isPresent()) {
					continue;
				}
				if (theSidBlasterIdx == 0 && device0Model == chipModel) {
					return 0;
				}
				if (theSidBlasterIdx == 1 && device1Model == chipModel) {
					return 1;
				}
				if (theSidBlasterIdx == 2 && device2Model == chipModel) {
					return 2;
				}
			}
		}
		// Nothing matched? Use next free slot (prevent already used one)
		for (int sidBlasterIdx = 0; sidBlasterIdx < hardSID.HardSID_Devices(); sidBlasterIdx++) {
			final int theSidBlasterIdx = sidBlasterIdx;
			if (sids.stream().filter(sid -> theSidBlasterIdx == sid.getDeviceId()).findFirst().isPresent()) {
				continue;
			}
			return sidBlasterIdx;
		}
		// no slot left
		return null;
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
			hardSID.HardSID_Delay(deviceID, (short) (REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}
}
