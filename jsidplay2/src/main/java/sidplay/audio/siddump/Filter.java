package sidplay.audio.siddump;

import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.FILTERCTRL;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.FILTERFREQ_HI;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.FILTERFREQ_LO;
import static libsidutils.siddump.SIDDumpConfiguration.SIDDumpReg.VOL;

public class Filter {

	private int cutoff, ctrl, type;

	public int getCutoff() {
		return cutoff;
	}

	public int getCtrl() {
		return ctrl;
	}

	public int getType() {
		return type;
	}

	public void read(byte[] registers) {
		cutoff = (registers[FILTERFREQ_LO.getRegister()] & 0xff) << 5
				| (registers[FILTERFREQ_HI.getRegister()] & 0xff) << 8;
		ctrl = registers[FILTERCTRL.getRegister()] & 0xff;
		type = registers[VOL.getRegister()] & 0xff;
	}

	public void assign(Filter filter) {
		ctrl = filter.ctrl;
		cutoff = filter.cutoff;
		type = filter.type;
	}

}