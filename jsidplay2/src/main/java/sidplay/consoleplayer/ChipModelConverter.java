package sidplay.consoleplayer;

import resid_builder.resid.ChipModel;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class ChipModelConverter implements IStringConverter<ChipModel> {
	@Override
	public ChipModel convert(String value) throws ParameterException {
		try {
			return ChipModel.valueOf(value);
		} catch (Exception e) {
			throw new ParameterException("Parameter value " + "should be "
					+ ChipModel.MOS6581 + " or " + ChipModel.MOS8580
					+ " (found " + value + ")");
		}
	}
}