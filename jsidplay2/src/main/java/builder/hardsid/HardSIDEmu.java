package builder.hardsid;

import static libsidplay.common.SIDChip.REG_COUNT;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.List;

import builder.resid.residfp.ReSIDfp;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.config.IEmulationSection;

/**
 *
 * @author Ken Händel
 *
 */
public class HardSIDEmu extends ReSIDfp {

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

		public FakeStereo(HardSIDBuilder hardSIDBuilder, EventScheduler context, CPUClock cpuClock, HardSID hardSID,
				byte deviceId, int chipNum, int sidNum, ChipModel chipModel, ChipModel defaultChipModel,
				List<HardSIDEmu> sids, IEmulationSection emulationSection) {
			super(hardSIDBuilder, context, cpuClock, hardSID, deviceId, chipNum, sidNum, chipModel, defaultChipModel);
			this.prevNum = sidNum - 1;
			this.sids = sids;
			this.emulationSection = emulationSection;
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

	private boolean doReadWriteDelayed;

	private String deviceName;

	private int sidNum;

	private final ChipModel chipModel;

	private boolean[] voiceMute = new boolean[4];

	private boolean[] filterDisable = new boolean[MAX_SIDS];

	public HardSIDEmu(HardSIDBuilder hardSIDBuilder, EventScheduler context, CPUClock cpuClock, HardSID hardSID,
			byte deviceID, int chipNum, int sidNum, ChipModel model, ChipModel defaultChipModel) {
		super(context);
		this.hardSIDBuilder = hardSIDBuilder;
		this.context = context;
		this.hardSID = hardSID;
		this.deviceID = deviceID;
		this.chipNum = (byte) chipNum;
		this.sidNum = sidNum;
		this.chipModel = model;

		super.setChipModel(model);
		super.setClockFrequency(cpuClock.getCpuFrequency());
		super.setSampler(sample -> {
		});
	}

	@Override
	public void write(int addr, byte data) {
		switch (addr & 0x1f) {
		case 0x04:
		case 0x0b:
		case 0x12:
			if (voiceMute[(addr - 4) / 7]) {
				data &= 0xfe;
			}
			super.write(addr, data);
			break;
		case 0x17:
			if (filterDisable[sidNum]) {
				data &= 0xf0;
			}
			super.write(addr, data);
			break;
		case 0x18:
			// samples muted? Fade-in is allowed anyway
			if (voiceMute[3] && (data & 0xf) < (readInternalRegister(addr) & 0xf)) {
				return;
			}
			super.write(addr, data);
			break;

		default:
			super.write(addr, data);
			break;
		}
		final byte dataByte = data;
		if (addr > 0x18) {
			return;
		}
		doReadWriteDelayed = true;
		doWriteDelayed(() -> {
			while (hardSID.hardsid_usb_write(deviceID, (byte) ((chipNum << 5) | addr),
					dataByte) == WState.WSTATE_BUSY) {
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
		super.clock();
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
		deviceReset((byte) 0xf);
		reset((byte) 0xf);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	protected void unlock() {
		deviceReset((byte) 0x0);
		reset((byte) 0x0);
		context.cancel(event);
		doReadWriteDelayed = false;
	}

	private void deviceReset(byte volume) {
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
	public void setVoiceMute(int num, boolean mute) {
		super.setVoiceMute(num, mute);
		if (num < 4) {
			voiceMute[num] = mute;
		}
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		super.setFilterEnable(emulation, sidNum);
		filterDisable[sidNum] = !emulation.isFilterEnable(sidNum);
	}

	public byte getDeviceId() {
		return deviceID;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	protected ChipModel getChipModel() {
		return chipModel;
	}

	public byte getChipNum() {
		return chipNum;
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
