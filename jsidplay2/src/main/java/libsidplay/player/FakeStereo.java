package libsidplay.player;

import libsidplay.common.ChipModel;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SamplingMethod;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;

public final class FakeStereo extends SIDEmu {
	private final SIDEmu s1;
	private final SIDEmu s2;
	private IEmulationSection emulation;

	public FakeStereo(EventScheduler context, SIDEmu s1, SIDEmu s2,
			IEmulationSection emulation) {
		super(context);
		this.s1 = s1;
		this.s2 = s2;
		this.emulation = emulation;
	}

	@Override
	public void reset(byte volume) {
		s1.reset(volume);
	}

	@Override
	public byte read(int addr) {
		return (emulation.getSidNumToRead() > 0 ? s2 : s1).read(addr);
	}

	@Override
	public void write(int addr, byte data) {
		s1.write(addr, data);
		s2.write(addr, data);
	}

	@Override
	public byte readInternalRegister(int addr) {
		return (emulation.getSidNumToRead() > 0 ? s2 : s1)
				.readInternalRegister(addr);
	}

	@Override
	public void clock() {
		s1.clock();
	}

	@Override
	public void setVoiceMute(int num, boolean mute) {
		s1.setVoiceMute(num, mute);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		s1.setFilter(config, sidNum);
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		s1.setFilterEnable(emulation, sidNum);
	}

	@Override
	public ChipModel getChipModel() {
		return s1.getChipModel();
	}

	@Override
	public void setChipModel(ChipModel model) {
		s1.setChipModel(model);
	}

	@Override
	public void setSampling(double cpuFrequency, float frequency,
			SamplingMethod sampling) {
		s1.setSampling(cpuFrequency, frequency, sampling);
	}

	@Override
	public void input(int input) {
		s1.input(input);
	}

	@Override
	public int getInputDigiBoost() {
		return s1.getInputDigiBoost();
	}

}