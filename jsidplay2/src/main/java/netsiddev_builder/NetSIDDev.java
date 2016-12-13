package netsiddev_builder;

import java.util.List;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.SetClocking;
import netsiddev_builder.commands.TrySetSidModel;

public class NetSIDDev extends SIDEmu {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write
	 * commands are routed two both SIDs, while read command can be configured
	 * to be processed by a specific SID chip.
	 * 
	 * @author ken
	 *
	 */
	public static class FakeStereo extends NetSIDDev {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<NetSIDDev> sids;

		public FakeStereo(EventScheduler context, NetSIDClient connection, final int sidNum, final ChipModel model,
				final IConfig config, final List<NetSIDDev> sids) {
			super(context, connection, sidNum, model);
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

		/**
		 * Order of writes is important, since SID read flushes the write queue!
		 * (Polly_Rock.sid)
		 * 
		 * @see netsiddev_builder.NetSIDDev#write(int, byte)
		 */
		@Override
		public void write(int addr, byte data) {
			if (emulationSection.getSidNumToRead() <= prevNum) {
				sids.get(prevNum).write(addr, data);
				super.write(addr, data);
			} else {
				super.write(addr, data);
				sids.get(prevNum).write(addr, data);
			}
		}
	}

	private byte sidNum;
	private NetSIDClient client;
	private ChipModel chipModel;

	public NetSIDDev(EventScheduler context, NetSIDClient connection, final int sidNum, final ChipModel model) {
		super(context);
		this.client = connection;
		this.sidNum = (byte) sidNum;
		this.chipModel = model;
	}

	@Override
	public void reset(byte volume) {
		// nothing to do
	}

	@Override
	public byte read(int addr) {
		clock();
		return client.read(sidNum, (byte) addr);
	}

	@Override
	public void write(int addr, final byte data) {
		clock();
		super.write(addr, data);
		client.write(sidNum, (byte) addr, data);
	}

	@Override
	public void clock() {
		// nothing to do
	}

	@Override
	public void setChipModel(ChipModel model) {
		// is already configured by setFilter()
	}

	@Override
	public void setClockFrequency(double cpuFrequency) {
		client.addAndSend(new SetClocking(cpuFrequency));
	}

	@Override
	public void input(int input) {
		// configured in the JSIDDevice user interface
	}

	@Override
	public int getInputDigiBoost() {
		// XXX unsupported by JSIDDevice
		return 0;
	}

	@Override
	public void setVoiceMute(int voice, boolean mute) {
		if (client.getVersion() >= 3 || voice < 3) {
			client.addAndSend(new Mute((byte) sidNum, (byte) voice, mute));
		}
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		IEmulationSection emulationSection = config.getEmulationSection();
		String filterName = emulationSection.getFilterName(sidNum, Engine.NETSID, Emulation.DEFAULT, chipModel);
		client.addAndSend(new TrySetSidModel((byte) sidNum, chipModel, filterName));
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		System.err.println("Filter enable unsupported by network SID client");
		// XXX unsupported by JSIDDevice
	}

}
