package ui.common.converter;

import java.util.ResourceBundle;

import javax.sound.sampled.Mixer.Info;

import javafx.util.StringConverter;

public final class MixerInfoToStringConverter extends StringConverter<Info> {

	private final ResourceBundle bundle;
	private final String nullBundleKey;

	public MixerInfoToStringConverter(ResourceBundle bundle, String nullBundleKey) {
		this.bundle = bundle;
		this.nullBundleKey = nullBundleKey;
	}

	@Override
	public Info fromString(String string) {
		throw new RuntimeException("This function is not supported!");
	}

	@Override
	public String toString(Info info) {
		return info != null ? info.getName() : bundle.getString(nullBundleKey);
	}
}
