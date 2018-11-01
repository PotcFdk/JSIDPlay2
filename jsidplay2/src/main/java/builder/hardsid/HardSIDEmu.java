package builder.hardsid;

import static libsidplay.common.SIDChip.REG_COUNT;

import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

/**
 * <pre>
 * *************************************************************************
 *           hardsid.cpp  -  Hardsid support interface.
 *                           Created from Jarnos original
 *                           Sidplay2 patch
 *                           -------------------
 *  begin                : Fri Dec 15 2000
 *  copyright            : (C) 2000-2002 by Simon White
 *  email                : s_a_white@email.com
 * *************************************************************************
 * </pre>
 * 
 * @author Ken Händel
 * 
 */
public class HardSIDEmu extends SIDEmu {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write commands
	 * are routed two both SIDs, while read command can be configured to be
	 * processed by a specific SID chip.
	 * 
	 * @author ken
	 *
	 */
	public static class FakeStereo extends HardSIDEmu {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<HardSIDEmu> sids;

		public FakeStereo(final EventScheduler context, final IConfig config, CPUClock cpuClock,
				HardSIDBuilder hardSIDBuilder, final HardSID hardSID, final byte deviceId, final int chipNum,
				final int sidNum, ChipModel chipModel, final List<HardSIDEmu> sids) {
			super(context, config, cpuClock, hardSIDBuilder, hardSID, deviceId, chipNum, sidNum, chipModel);
			this.emulationSection = config.getEmulationSection();
			this.prevNum = sidNum - 1;
			this.sids = sids;
		}

		@Override
		public byte read(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				return sids.get(prevNum).read(addr);
			}
			return super.read(addr);
		}

		@Override
		public byte readInternalRegister(int addr) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				return sids.get(prevNum).readInternalRegister(addr);
			}
			return super.readInternalRegister(addr);
		}

		@Override
		public void write(int addr, byte data) {
			super.write(addr, data);
			sids.get(prevNum).write(addr, data);
		}
	}

	private static final short SHORTEST_DELAY = 4;

	private final Event event = new Event("HardSID Delay") {
		@Override
		public void event() {
			context.schedule(event, hardSIDBuilder.eventuallyDelay(), Event.Phase.PHI2);
		}
	};

	private EventScheduler context;

	private final HardSID hardSID;

	private final byte deviceID;

	private final byte chipNum;

	private final byte sidNum;

	private ChipModel chipModel;

	private HardSIDBuilder hardSIDBuilder;

	private final CPUClock cpuClock;

	private final IAudioSection audioSection;

	private boolean doReadWriteDelayed;

	public HardSIDEmu(EventScheduler context, IConfig config, CPUClock cpuClock, HardSIDBuilder hardSIDBuilder,
			final HardSID hardSID, final byte deviceID, final int chipNum, int sidNum, final ChipModel model) {
		this.context = context;
		this.audioSection = config.getAudioSection();
		this.cpuClock = cpuClock;
		this.hardSIDBuilder = hardSIDBuilder;
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.chipNum = (byte) chipNum;
		this.sidNum = (byte) sidNum;
		this.chipModel = model;
	}

	@Override
	public void reset(final byte volume) {
		hardSID.HardSID_Reset(deviceID);
		for (byte reg = 0; reg < REG_COUNT; reg++) {
			hardSID.HardSID_Delay(deviceID, SHORTEST_DELAY);
			hardSID.HardSID_Write(deviceID, chipNum, reg, (byte) 0);
		}
		hardSID.HardSID_Delay(deviceID, SHORTEST_DELAY);
		hardSID.HardSID_Write(deviceID, chipNum, (byte) 0xf, volume);
		hardSID.HardSID_Flush(deviceID);
	}

	@Override
	public byte read(int addr) {
		clock();
		// not supported by HardSID4U!
		return (byte) 0xff;
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);

		doReadWriteDelayed = true;
		doWriteDelayed(() -> {
			hardSID.HardSID_Write(deviceID, chipNum, (byte) addr, data);
		});
	}

	@Override
	public void clock() {
		doReadWriteDelayed = true;
		doWriteDelayed(() -> {
			hardSID.HardSID_Delay(deviceID, (short) hardSIDBuilder.clocksSinceLastAccess());
		});
	}

	private void doWriteDelayed(Runnable runnable) {
		int delay = (int) (cpuClock.getCpuFrequency() / 1000. * audioSection.getDelay(sidNum));
		if (delay > 0) {
			context.schedule(new Event("Delayed SID output") {
				@Override
				public void event() throws InterruptedException {
					if (doReadWriteDelayed) {
						runnable.run();
					}
				}
			}, delay);
		} else {
			runnable.run();
		}
	}

	protected void lock() {
		reset((byte) 0xf);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	protected void unlock() {
		reset((byte) 0x0);
		context.cancel(event);
		doReadWriteDelayed = false;
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
	}

	public byte getChipNum() {
		return chipNum;
	}

	protected ChipModel getChipModel() {
		return chipModel;
	}

	@Override
	public void setChipModel(final ChipModel model) {
	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
	}

	@Override
	public void input(int input) {
	}

	@Override
	public int getInputDigiBoost() {
		return 0;
	}

	public static final String credits() {
		return "HardSID V1.0.1 Engine:\n" + "\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n";
	}

}
