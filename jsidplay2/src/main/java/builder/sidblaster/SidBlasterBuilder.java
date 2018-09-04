package builder.sidblaster;

import static libsidplay.common.Engine.SIDBLASTER;

import java.util.ArrayList;
import java.util.List;

import com.sun.jna.Native;

import libsidplay.common.CPUClock;
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

	private static final short REGULAR_DELAY = Short.MAX_VALUE;

	static final short SHORTEST_DELAY = 4;

	/**
	 * System event context.
	 */
	private EventScheduler context;

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

	public SidBlasterBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
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
		if (sidNum < hardSID.HardSID_Devices()) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			SIDBlasterEmu hsid = new SIDBlasterEmu(context, this, hardSID, sidNum);
			sids.add(hsid);
			if (hsid.lock()) {
				return hsid;
			}
		}
		System.err.println(/* throw new RuntimeException( */String
				.format("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)", sidNum));
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		SIDBlasterEmu hardSid = (SIDBlasterEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
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
