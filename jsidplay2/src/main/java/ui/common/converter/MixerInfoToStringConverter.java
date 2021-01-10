package ui.common.converter;

import javax.sound.sampled.Mixer.Info;

import javafx.util.StringConverter;

public class MixerInfoToStringConverter extends StringConverter<Info> {

	@Override
	public Info fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(Info info) {
		return info.getName();
	}
}
