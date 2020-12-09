package builder.hardsid;

import static libsidplay.common.SIDChip.REG_COUNT;

import java.util.List;

import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;

/**
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

		public FakeStereo(final EventScheduler context, final IConfig config, HardSIDBuilder hardSIDBuilder,
				final HardSID hardSID, final byte deviceId, final int chipNum, final int sidNum, ChipModel chipModel,
				final List<HardSIDEmu> sids) {
			super(context, hardSIDBuilder, hardSID, deviceId, chipNum, sidNum, chipModel);
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

	private final EventScheduler context;

	private final HardSIDBuilder hardSIDBuilder;

	private final HardSID hardSID;

	private final byte deviceID;

	private final byte chipNum;

	private int sidNum;

	private final ChipModel chipModel;

	private boolean doReadWriteDelayed;

	public HardSIDEmu(EventScheduler context, HardSIDBuilder hardSIDBuilder, final HardSID hardSID, final byte deviceID,
			final int chipNum, final int sidNum, final ChipModel model) {
		this.context = context;
		this.hardSIDBuilder = hardSIDBuilder;
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.chipNum = (byte) chipNum;
		this.sidNum = sidNum;
		this.chipModel = model;
	}

	@Override
	public void reset(final byte volume) {
		hardSID.hardsid_usb_abortplay(deviceID);
		for (byte reg = 0; reg < REG_COUNT; reg++) {
			while (hardSID.hardsid_usb_write(deviceID, (byte) ((chipNum << 5) | reg), (byte) 0) == WState.WSTATE_BUSY) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
			while (hardSID.hardsid_usb_delay(deviceID, SHORTEST_DELAY) == WState.WSTATE_BUSY) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		while (hardSID.hardsid_usb_write(deviceID, (byte) ((chipNum << 5) | 0xf), volume) == WState.WSTATE_BUSY) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		while (hardSID.hardsid_usb_flush(deviceID) == WState.WSTATE_BUSY) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
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
			while (hardSID.hardsid_usb_write(deviceID, (byte) ((chipNum << 5) | addr), data) == WState.WSTATE_BUSY) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		});
	}

	@Override
	public void clock() {
		final short clocksSinceLastAccess = (short) hardSIDBuilder.clocksSinceLastAccess();
		doWriteDelayed(() -> {
			if (clocksSinceLastAccess > 0) {
				while (hardSID.hardsid_usb_delay(deviceID, clocksSinceLastAccess) == WState.WSTATE_BUSY) {
					try {
						Thread.sleep(0);
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
				}
			}
		});
	}

	private void doWriteDelayed(Runnable runnable) {
		if (hardSIDBuilder.getDelay(sidNum) > 0) {
			context.schedule(new Event("Delayed SID output") {
				@Override
				public void event() throws InterruptedException {
					if (doReadWriteDelayed) {
						runnable.run();
					}
				}
			}, hardSIDBuilder.getDelay(sidNum));
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
		final StringBuffer credits = new StringBuffer();
		credits.append("HardSID4U Java version by Ken Händel <kschwiersch@yahoo.de> Copyright (©) 2007\n");
		credits.append("\tSupported by official HardSID support\n");
		credits.append("\tBased on HardSID V1.0.1 Engine:\n");
		credits.append("\tCopyright (©) 1999-2002 Simon White <sidplay2@yahoo.com>\n");
		return credits.toString();
	}

}
