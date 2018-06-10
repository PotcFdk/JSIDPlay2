package sidblaster_builder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

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
public class SidBlasterSIDBuilder implements SIDBuilder {

	private static final short REGULAR_DELAY = 256;

	static final short SHORTEST_DELAY = 4;

	/**
	 * System event context.
	 */
	private EventScheduler context;

	/**
	 * Native library wrapper.
	 */
	private static SIDBlasterSID hardSID;

	/**
	 * Already used SIDBlaster SIDs.
	 */
	private List<HardSID> sids = new ArrayList<HardSID>();

	/**
	 * Device number, if more than one USB devices is connected.
	 */
	private byte deviceID;

	protected long lastSIDWriteTime;

	private static boolean initialized;

	public SidBlasterSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		if (!initialized) {
			// Extract and Load driver recognizing real SIDBlaster devices
			try {
				System.load(extract(config, "/sidblaster_builder/win64/Release/", "hardsid.dll"));
			} catch (Error e) {
				System.err.println("64-bit Java and Windows is required to make use of that feature!");
				throw e;
			} catch (IOException e) {
				throw new RuntimeException(String.format("SIDBLASTER ERROR: hardsid.dll not found!"));
			}
			// Extract and Load JNI driver wrapper using the library above
			try {
				System.load(extract(config, "/sidblaster_builder/win64/Release/", "JSIDBlaster.dll"));
			} catch (Error e) {
				System.err.println("64-bit Java and Windows is required to make use of that feature!");
				throw e;
			} catch (IOException e) {
				throw new RuntimeException(String.format("SIDBLASTER ERROR: JSIDBlaster.dll not found!"));
			}
			initialized = true;
		}
		// Go and use JNI driver wrapper
		hardSID = new SIDBlasterSID();
	}

	/**
	 * Extract a classpath resource referencing a file to the temp directory and
	 * mark to be deleted after exit.
	 * 
	 * @param config
	 *            configuration
	 * @param path
	 *            resource path
	 * @param libName
	 *            filename
	 * @return absolute path name of the extracted file
	 * @throws IOException
	 *             I/O error
	 */
	private String extract(final IConfig config, final String path, final String libName) throws IOException {
		File file = new File(new File(config.getSidplay2Section().getTmpDir()), libName);
		file.deleteOnExit();
		try (InputStream is = getClass().getResourceAsStream(path + libName)) {
			Files.copy(is, Paths.get(file.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
		}
		return file.getAbsolutePath();
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		final byte chipNum = 0;
		if (deviceID < hardSID.HardSID_DeviceCount() && chipNum < hardSID.HardSID_SIDCount(deviceID)) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			// what chip model is plugged-in? We don't know
			ChipModel chipModel = ChipModel.MOS6581;
			HardSID hsid = new HardSID(context, this, hardSID, deviceID, chipNum, chipModel);
			sids.add(hsid);
			hsid.lock();
			return hsid;
		}
		throw new RuntimeException(
				String.format("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (DeviceID=%d, SID=%d)",
						deviceID, chipNum));
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		HardSID hardSid = (HardSID) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return Math.max(SHORTEST_DELAY, diff);
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY << 1) {
			lastSIDWriteTime += REGULAR_DELAY;
			hardSID.HardSID_Delay(deviceID, REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}

}
