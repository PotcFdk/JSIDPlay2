package libsidplay.common;

/**
 * Example presets that ship with the ffmpeg.
 * 
 * @author ken
 *
 */
public enum VideoCoderPreset {

	VERY_FAST("libx264-veryfast.ffpreset"), FAST("libx264-fast.ffpreset"), NORMAL("libx264-normal.ffpreset"),
	HQ("libx264-hq.ffpreset"), LOSSLESS_ULTRA_FAST("libx264-lossless_ultrafast.ffpreset"),
	LOSSLESS_FAST("libx264-lossless_fast.ffpreset"), LOSSLESS_MEDIUM("libx264-lossless_medium.ffpreset"),
	LOSSLESS_MAX("libx264-lossless_max.ffpreset");

	private String presetName;

	private VideoCoderPreset(String presetName) {
		this.presetName = presetName;
	}

	public String getPresetName() {
		return presetName;
	}
}
