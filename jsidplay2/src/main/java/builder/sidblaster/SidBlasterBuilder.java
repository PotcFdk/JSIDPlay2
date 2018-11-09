package builder.sidblaster;

import static libsidplay.common.Engine.SIDBLASTER;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.sidtune.SidTune;

/**
 * 
 * @author Ken Händel
 * 
 */
public class SidBlasterBuilder implements SIDBuilder {

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

	private CPUClock cpuClock;

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
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		Integer deviceId = getModelDependantDeviceId(chipModel, sidNum);
		if (deviceId != null && deviceId < hardSID.HardSID_Devices()) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			System.out.println("Use device Idx: " + deviceId + " for sidNum=" + sidNum + " and model " + chipModel);
			SIDBlasterEmu hsid = createSID(deviceId.byteValue(), sidNum, tune);
			if (hsid.lock()) {
				sids.add(hsid);
				return hsid;
			}
		}
		System.err.println(/* throw new RuntimeException( */String
				.format("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)", sidNum));
		if (SidTune.isFakeStereoSid(config.getEmulationSection(), tune, sidNum)) {
			// Fake stereo chip not available? Re-use original chip
			return oldHardSID;
		}
		return SIDEmu.NONE;
	}

	private SIDBlasterEmu createSID(byte deviceId, int sidNum, SidTune tune) {
		if (SidTune.isFakeStereoSid(config.getEmulationSection(), tune, sidNum)) {
			return new SIDBlasterEmu.FakeStereo(context, config, cpuClock, this, hardSID, deviceId, sidNum,
					sids);
		} else {
			return new SIDBlasterEmu(context, config, cpuClock, this, hardSID, sidNum, deviceId);
		}
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		SIDBlasterEmu hardSid = (SIDBlasterEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
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
		return diff;
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY) {
			lastSIDWriteTime += REGULAR_DELAY;
			hardSID.HardSID_Delay(deviceID, REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}
}
