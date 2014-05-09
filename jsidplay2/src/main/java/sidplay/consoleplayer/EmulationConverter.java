package sidplay.consoleplayer;

import libsidplay.player.Emulation;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class EmulationConverter implements IStringConverter<Emulation> {
	@Override
	public Emulation convert(String value) throws ParameterException {
		try {
			return Emulation.valueOf(value);
		} catch (Exception e) {
			throw new ParameterException("Parameter value " + "should be "
					+ Emulation.EMU_NONE + ", " + Emulation.EMU_RESID
					+ " or " + Emulation.EMU_HARDSID + " (found " + value
					+ ")");
		}
	}
}