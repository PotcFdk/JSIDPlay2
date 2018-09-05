package builder.hardsid;

import static libsidplay.common.Engine.HARDSID;

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
public class HardSIDBuilder implements SIDBuilder {

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
	 * Already used HardSIDs.
	 */
	private List<HardSIDEmu> sids = new ArrayList<HardSIDEmu>();

	/**
	 * Device number, if more than one USB devices is connected.
	 */
	private byte deviceID;

	protected long lastSIDWriteTime;

	public HardSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		if (hardSID == null) {
			try {
				hardSID = (HardSID) Native.loadLibrary("hardsid_usb", HardSID.class);
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Error: 32-bit Java for Windows is required to use " + HARDSID + " soundcard!");
				throw e;
			}
		}
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		ChipModel chipModel = ChipModel.getChipModel(config.getEmulationSection(), tune, sidNum);
		Integer chipNum = getModelDependantSidNum(chipModel, sidNum);
		if (deviceID < hardSID.HardSID_Devices() && chipNum != null && chipNum < hardSID.HardSID_SIDCount(deviceID)) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			System.out.println("Use HardSID sidNum: " + chipNum);
			HardSIDEmu hsid = new HardSIDEmu(context, this, hardSID, deviceID, chipNum, chipModel);
			sids.add(hsid);
			hsid.lock();
			return hsid;
		}
		System.err.println(/* throw new RuntimeException( */
				String.format("HARDSID ERROR: System doesn't have enough SID chips. Requested: (DeviceID=%d, SIDs=%d)",
						deviceID, hardSID.HardSID_SIDCount(deviceID)));
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		HardSIDEmu hardSid = (HardSIDEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	/**
	 * Get SID index based on the desired chip model.
	 * 
	 * @param chipModel desired chip model
	 * @param sidNum    current SID number
	 * @return SID index of the desired HardSID SID
	 */
	private Integer getModelDependantSidNum(final ChipModel chipModel, int sidNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();
		if (sidNum == 0) {
			// Mono SID: choose according to the chip model type
			return chipModel == ChipModel.MOS6581 ? sid6581 : sid8580;
		} else {
			// Stereo or 3-SID: use next free slot (prevent wrong type and already used one)
			for (int hardSidIdx = 0; hardSidIdx < hardSID.HardSID_SIDCount(deviceID); hardSidIdx++) {
				final int theHardSIDIdx = hardSidIdx;
				if (sids.stream().filter(sid -> theHardSIDIdx == sid.getSidNum()).findFirst().isPresent()) {
					continue;
				}
				if (hardSidIdx != sid6581 && hardSidIdx != sid8580) {
					return hardSidIdx;
				}
			}
		}
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
