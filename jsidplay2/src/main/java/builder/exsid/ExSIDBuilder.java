package builder.exsid;

import static libsidplay.common.Engine.EXSID;
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
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioDriver;

/**
 * 
 * Support of ExSID mini USB devices.
 *
 * @author Ken Händel
 *
 */
public class ExSIDBuilder implements HardwareSIDBuilder, Mixer {

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
	private static ExSID exSID;

	/**
	 * Number of ExSID devices.
	 */
	private static int deviceCount;

	/**
	 * Already used ExSID SIDs.
	 */
	private List<ExSIDEmu> sids = new ArrayList<>();

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public ExSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (exSID == null) {
			try {
				exSID = Native.load("exsid", ExSID.class, createOptions());
				init();
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Error: Linux or OSX is required to use " + EXSID + " soundcard!");
				printInstallationHint();
				throw e;
			}
		}
	}

	private Map<String, Object> createOptions() {
		Map<String, Object> options = new HashMap<String, Object>();
		options.put(Library.OPTION_TYPE_MAPPER, new DefaultTypeMapper() {
			{
				addTypeConverter(AudioOp.class, new EnumConverter<AudioOp>(AudioOp.class));
				addTypeConverter(ChipSelect.class, new EnumConverter<ChipSelect>(ChipSelect.class));
				addTypeConverter(ClockSelect.class, new EnumConverter<ClockSelect>(ClockSelect.class));
				addTypeConverter(HardwareModel.class, new EnumConverter<HardwareModel>(HardwareModel.class));
			}
		});
		return options;
	}

	private void init() {
		if (exSID.exSID_init() < 0) {
			throw new RuntimeException(exSID.exSID_error_str());
		}
		deviceCount = 1;
//		exSID.exSID_exit();
	}

	public static void printInstallationHint() {
//		if (OS.get() == OS.LINUX) {
//			printLinuxInstallationHint();
//		} else if (OS.get() == OS.MAC) {
//			printMacInstallationHint();
//		}
		System.err.println("Maybe you just forgot to plugin in your USB devices?");
		System.err.println();
	}

	@Override
	public SIDEmu lock(SIDEmu oldExSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		ChipModel defaultSidModel = emulationSection.getDefaultSidModel();
		if (oldExSID != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return oldExSID;
		}
		ExSIDEmu sid = createSID((byte) 0, sidNum, tune, chipModel, defaultSidModel);

		if (sidNum == 0 && sid.lock()) {
			sid.setFilterEnable(emulationSection, sidNum);
			sid.input(emulationSection.isDigiBoosted8580() ? sid.getInputDigiBoost() : 0);
			for (int voice = 0; voice < 4; voice++) {
				sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
			}
			sids.add(sid);
			setDelay(sidNum, audioSection.getDelay(sidNum));
			return sid;
		}
		System.err.printf("EXSID ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(SIDEmu sidEmu) {
		ExSIDEmu sid = (ExSIDEmu) sidEmu;
		exSID.exSID_audio_op(AudioOp.XS_AU_MUTE);
		sids.remove(sid);
		sid.unlock();
	}

	@Override
	public int getDeviceCount() {
		return deviceCount;
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return 0;
	}

	@Override
	public String getDeviceName(int sidNum) {
		return "ExSID";
	}

	@Override
	public ChipModel getDeviceChipModel(int sidNum) {
		return ChipModel.MOS6581;
	}

	@Override
	public void setAudioDriver(AudioDriver audioDriver) {
	}

	@Override
	public void start() {
	}

	@Override
	public void fadeIn(double fadeIn) {
		System.err.println("Fade-in unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by ExSID");
		// XXX unsupported by ExSID
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
	}

	private ExSIDEmu createSID(byte deviceId, int sidNum, SidTune tune, ChipModel chipModel,
			ChipModel defaultChipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new ExSIDEmu.FakeStereo(this, context, cpuClock, exSID, deviceId, sidNum, chipModel,
					defaultChipModel, sids, emulationSection);
		} else {
			return new ExSIDEmu(this, context, cpuClock, exSID, deviceId, sidNum, chipModel, defaultChipModel);
		}
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
			exSID.exSID_delay((short) (REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}
}
