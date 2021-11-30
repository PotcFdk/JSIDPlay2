package ui.entities.config;

import static sidplay.ini.IniDefaults.DEFAULT_BLEED;
import static sidplay.ini.IniDefaults.DEFAULT_BLUR;
import static sidplay.ini.IniDefaults.DEFAULT_BRIGHTNESS;
import static sidplay.ini.IniDefaults.DEFAULT_CONTRAST;
import static sidplay.ini.IniDefaults.DEFAULT_ENABLE_DATABASE;
import static sidplay.ini.IniDefaults.DEFAULT_FADE_IN_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_FADE_OUT_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_GAMMA;
import static sidplay.ini.IniDefaults.DEFAULT_LOOP;
import static sidplay.ini.IniDefaults.DEFAULT_OFFSET;
import static sidplay.ini.IniDefaults.DEFAULT_PAL_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_PHASE_SHIFT;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_LENGTH;
import static sidplay.ini.IniDefaults.DEFAULT_SATURATION;
import static sidplay.ini.IniDefaults.DEFAULT_SINGLE_TRACK;
import static sidplay.ini.IniDefaults.DEFAULT_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_TINT;
import static sidplay.ini.IniDefaults.DEFAULT_TMP_DIR;
import static sidplay.ini.IniDefaults.DEFAULT_TURBO_TAPE;

import java.io.File;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.config.ISidPlay2Section;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.converter.FileAttributeConverter;
import ui.common.converter.FileToStringDeserializer;
import ui.common.converter.FileToStringSerializer;
import ui.common.converter.FileXmlAdapter;
import ui.common.properties.ShadowField;
import ui.favorites.PlaybackType;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	public static final PlaybackType DEFAULT_PLAYBACK_TYPE = PlaybackType.PLAYBACK_OFF;
	public static final boolean DEFAULT_PROXY_ENABLE = false;
	public static final String DEFAULT_PROXY_HOSTNAME = null;
	public static final int DEFAULT_PROXY_PORT = 80;
	public static final int DEFAULT_FRAME_X = 0;
	public static final int DEFAULT_FRAME_Y = 0;
	public static final int DEFAULT_FRAME_WIDTH = 1310;
	public static final int DEFAULT_FRAME_HEIGHT = 1024;
	public static final boolean DEFAULT_MINIMIZED = false;
	public static final float DEFAULT_VIDEO_SCALING = 2f;
	public static final boolean DEFAULT_SHOW_MONITOR = true;

	private int version;

	@Override
	public final int getVersion() {
		return version;
	}

	@Override
	public final void setVersion(int version) {
		this.version = version;
	}

	private ShadowField<BooleanProperty, Boolean> enableDatabase = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_ENABLE_DATABASE);

	@Override
	public final boolean isEnableDatabase() {
		return enableDatabase.get();
	}

	@Override
	public final void setEnableDatabase(boolean isEnableDatabase) {
		enableDatabase.set(isEnableDatabase);
	}

	public final BooleanProperty enableDatabaseProperty() {
		return enableDatabase.property();
	}

	private ShadowField<DoubleProperty, Number> startTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_START_TIME);

	@Override
	public final double getStartTime() {
		return startTime.get().doubleValue();
	}

	@Override
	public final void setStartTime(double startTime) {
		this.startTime.set(startTime);
	}

	public final DoubleProperty startTimeProperty() {
		return startTime.property();
	}

	private ShadowField<DoubleProperty, Number> defaultPlayLength = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_PLAY_LENGTH);

	@Override
	public final double getDefaultPlayLength() {
		return defaultPlayLength.get().doubleValue();
	}

	@Override
	public final void setDefaultPlayLength(double defaultPlayLength) {
		this.defaultPlayLength.set(defaultPlayLength);
	}

	public final DoubleProperty defaultPlayLengthProperty() {
		return defaultPlayLength.property();
	}

	private ShadowField<DoubleProperty, Number> fadeInTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_IN_TIME);

	@Override
	public final double getFadeInTime() {
		return fadeInTime.get().doubleValue();
	}

	@Override
	public final void setFadeInTime(double fadeInTime) {
		this.fadeInTime.set(fadeInTime);
	}

	public final DoubleProperty fadeInTimeProperty() {
		return fadeInTime.property();
	}

	private ShadowField<DoubleProperty, Number> fadeOutTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_OUT_TIME);

	@Override
	public final double getFadeOutTime() {
		return fadeOutTime.get().doubleValue();
	}

	@Override
	public final void setFadeOutTime(double fadeOutTime) {
		this.fadeOutTime.set(fadeOutTime);
	}

	public final DoubleProperty fadeOutTimeProperty() {
		return fadeOutTime.property();
	}

	private ShadowField<ObjectProperty<PlaybackType>, PlaybackType> playbackType = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_PLAYBACK_TYPE);

	@Enumerated(EnumType.STRING)
	public final PlaybackType getPlaybackType() {
		return playbackType.get();
	}

	public final void setPlaybackType(PlaybackType playbackType) {
		this.playbackType.set(playbackType);
	}

	public final ObjectProperty<PlaybackType> playbackTypeProperty() {
		return playbackType.property();
	}

	private ShadowField<BooleanProperty, Boolean> loop = new ShadowField<>(SimpleBooleanProperty::new, DEFAULT_LOOP);

	@Override
	public final boolean isLoop() {
		return loop.get();
	}

	@Override
	public final void setLoop(boolean loop) {
		this.loop.set(loop);
	}

	public final BooleanProperty loopProperty() {
		return loop.property();
	}

	private ShadowField<BooleanProperty, Boolean> single = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_SINGLE_TRACK);

	@Override
	public final boolean isSingle() {
		return single.get();
	}

	@Override
	public final void setSingle(boolean isSingle) {
		single.set(isSingle);
	}

	public final BooleanProperty singleProperty() {
		return single.property();
	}

	private ShadowField<ObjectProperty<File>, File> hvmec = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getHVMEC() {
		return hvmec.get();
	}

	public final void setHVMEC(File hVMEC) {
		hvmec.set(hVMEC);
	}

	public final ObjectProperty<File> hvmecProperty() {
		return hvmec.property();
	}

	private ShadowField<ObjectProperty<File>, File> demos = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getDemos() {
		return demos.get();
	}

	public final void setDemos(File demos) {
		this.demos.set(demos);
	}

	public final ObjectProperty<File> demosProperty() {
		return demos.property();
	}

	private ShadowField<ObjectProperty<File>, File> mags = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getMags() {
		return mags.get();
	}

	public final void setMags(File mags) {
		this.mags.set(mags);
	}

	public final ObjectProperty<File> magsProperty() {
		return mags.property();
	}

	private ShadowField<ObjectProperty<File>, File> cgsc = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getCgsc() {
		return cgsc.get();
	}

	public final void setCgsc(File cgsc) {
		this.cgsc.set(cgsc);
	}

	public final ObjectProperty<File> cgscProperty() {
		return cgsc.property();
	}

	private ShadowField<ObjectProperty<File>, File> hvsc = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@Override
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getHvsc() {
		return hvsc.get();
	}

	@Override
	public final void setHvsc(File hvsc) {
		this.hvsc.set(hvsc);
	}

	public final ObjectProperty<File> hvscProperty() {
		return hvsc.property();
	}

	private ShadowField<ObjectProperty<File>, File> gameBase64 = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getGameBase64() {
		return gameBase64.get();
	}

	public final void setGameBase64(File gameBase64) {
		this.gameBase64.set(gameBase64);
	}

	public final ObjectProperty<File> gameBase64Property() {
		return gameBase64.property();
	}

	private ShadowField<BooleanProperty, Boolean> proxyEnable = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PROXY_ENABLE);

	public final boolean isProxyEnable() {
		return proxyEnable.get();
	}

	public final void setProxyEnable(boolean isProxyEnable) {
		proxyEnable.set(isProxyEnable);
	}

	public final BooleanProperty proxyEnableProperty() {
		return proxyEnable.property();
	}

	private ShadowField<StringProperty, String> proxyHostname = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_PROXY_HOSTNAME);

	public final String getProxyHostname() {
		return proxyHostname.get();
	}

	public final void setProxyHostname(String hostname) {
		this.proxyHostname.set(hostname);
	}

	public final StringProperty proxyHostnameProperty() {
		return proxyHostname.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> proxyPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_PROXY_PORT);

	public final int getProxyPort() {
		return proxyPort.get();
	}

	public final void setProxyPort(int port) {
		this.proxyPort.set(port);
	}

	public final ObjectProperty<Integer> proxyPortProperty() {
		return proxyPort.property();
	}

	private ShadowField<ObjectProperty<File>, File> lastDirectory = new ShadowField<>(SimpleObjectProperty::new, null);

	@Override
	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getLastDirectory() {
		return lastDirectory.get();
	}

	@Override
	public final void setLastDirectory(File lastDirectory) {
		this.lastDirectory.set(lastDirectory);
	}

	public final ObjectProperty<File> lastDirectoryProperty() {
		return lastDirectory.property();
	}

	private ShadowField<ObjectProperty<File>, File> tmpDir = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_TMP_DIR);

	@Override
	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public final File getTmpDir() {
		return tmpDir.get();
	}

	@Override
	public final void setTmpDir(File tmpDir) {
		this.tmpDir.set(tmpDir);
	}

	public final ObjectProperty<File> tmpDirProperty() {
		return tmpDir.property();
	}

	private ShadowField<DoubleProperty, Number> frameX = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FRAME_X);

	public final double getFrameX() {
		return frameX.get().doubleValue();
	}

	public final void setFrameX(double frameX) {
		this.frameX.set(frameX);
	}

	public final DoubleProperty frameXProperty() {
		return frameX.property();
	}

	private ShadowField<DoubleProperty, Number> frameY = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FRAME_Y);

	public final double getFrameY() {
		return frameY.get().doubleValue();
	}

	public final void setFrameY(double frameY) {
		this.frameY.set(frameY);
	}

	public final DoubleProperty frameYProperty() {
		return frameY.property();
	}

	private ShadowField<DoubleProperty, Number> frameWidth = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FRAME_WIDTH);

	public final double getFrameWidth() {
		return frameWidth.get().doubleValue();
	}

	public final void setFrameWidth(double frameWidth) {
		this.frameWidth.set(frameWidth);
	}

	public final DoubleProperty frameWidthProperty() {
		return frameWidth.property();
	}

	private ShadowField<DoubleProperty, Number> frameHeight = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FRAME_HEIGHT);

	public final double getFrameHeight() {
		return frameHeight.get().doubleValue();
	}

	public final void setFrameHeight(double frameHeight) {
		this.frameHeight.set(frameHeight);
	}

	public final DoubleProperty frameHeightProperty() {
		return frameHeight.property();
	}

	private ShadowField<BooleanProperty, Boolean> minimized = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MINIMIZED);

	public final boolean isMinimized() {
		return minimized.get();
	}

	public final void setMinimized(boolean isMinimized) {
		minimized.set(isMinimized);
	}

	public final BooleanProperty minimizedProperty() {
		return minimized.property();
	}

	private ShadowField<DoubleProperty, Number> minimizedX = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public final double getMinimizedX() {
		return minimizedX.get().doubleValue();
	}

	public final void setMinimizedX(double minimizedX) {
		this.minimizedX.set(minimizedX);
	}

	public final DoubleProperty minimizedXProperty() {
		return minimizedX.property();
	}

	private ShadowField<DoubleProperty, Number> minimizedY = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public final double getMinimizedY() {
		return minimizedY.get().doubleValue();
	}

	public final void setMinimizedY(double minimizedY) {
		this.minimizedY.set(minimizedY);
	}

	public final DoubleProperty minimizedYProperty() {
		return minimizedY.property();
	}

	private ShadowField<DoubleProperty, Number> minimizedWidth = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public final double getMinimizedWidth() {
		return minimizedWidth.get().doubleValue();
	}

	public final void setMinimizedWidth(double minimizedWidth) {
		this.minimizedWidth.set(minimizedWidth);
	}

	public final DoubleProperty minimizedWidthProperty() {
		return minimizedWidth.property();
	}

	private ShadowField<DoubleProperty, Number> minimizedHeight = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), 0);

	public final double getMinimizedHeight() {
		return minimizedHeight.get().doubleValue();
	}

	public final void setMinimizedHeight(double minimizedHeight) {
		this.minimizedHeight.set(minimizedHeight);
	}

	public final DoubleProperty minimizedHeightProperty() {
		return minimizedHeight.property();
	}

	private ShadowField<FloatProperty, Number> videoScaling = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_VIDEO_SCALING);

	public final float getVideoScaling() {
		return videoScaling.get().floatValue();
	}

	public final void setVideoScaling(float videoScaling) {
		this.videoScaling.set(videoScaling);
	}

	public final FloatProperty videoScalingProperty() {
		return videoScaling.property();
	}

	private ShadowField<ObjectProperty<Boolean>, Boolean> showMonitor = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_SHOW_MONITOR);

	public final boolean isShowMonitor() {
		return showMonitor.get();
	}

	public final void setShowMonitor(boolean showMonitor) {
		this.showMonitor.set(showMonitor);
	}

	public final ObjectProperty<Boolean> showMonitorProperty() {
		return showMonitor.property();
	}

	private ShadowField<BooleanProperty, Boolean> palEmulation = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PAL_EMULATION);

	@Override
	public final boolean isPalEmulation() {
		return palEmulation.get();
	}

	@Override
	public final void setPalEmulation(boolean isPalEmulation) {
		palEmulation.set(isPalEmulation);
	}

	public final BooleanProperty palEmulationProperty() {
		return palEmulation.property();
	}

	private ShadowField<FloatProperty, Number> brightness = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BRIGHTNESS);

	@Override
	public final float getBrightness() {
		return brightness.get().floatValue();
	}

	@Override
	public final void setBrightness(float brightness) {
		this.brightness.set(brightness);
	}

	public final FloatProperty brightnessProperty() {
		return brightness.property();
	}

	private ShadowField<FloatProperty, Number> contrast = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_CONTRAST);

	@Override
	public final float getContrast() {
		return contrast.get().floatValue();
	}

	@Override
	public final void setContrast(float contrast) {
		this.contrast.set(contrast);
	}

	public final FloatProperty contrastProperty() {
		return contrast.property();
	}

	private ShadowField<FloatProperty, Number> gamma = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_GAMMA);

	@Override
	public final float getGamma() {
		return gamma.get().floatValue();
	}

	@Override
	public final void setGamma(float gamma) {
		this.gamma.set(gamma);
	}

	public final FloatProperty gammaProperty() {
		return gamma.property();
	}

	private ShadowField<FloatProperty, Number> saturation = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SATURATION);

	@Override
	public final float getSaturation() {
		return saturation.get().floatValue();
	}

	@Override
	public final void setSaturation(float saturation) {
		this.saturation.set(saturation);
	}

	public final FloatProperty saturationProperty() {
		return saturation.property();
	}

	private ShadowField<FloatProperty, Number> phaseShift = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_PHASE_SHIFT);

	@Override
	public final float getPhaseShift() {
		return phaseShift.get().floatValue();
	}

	@Override
	public final void setPhaseShift(float phaseShift) {
		this.phaseShift.set(phaseShift);
	}

	public final FloatProperty phaseShiftProperty() {
		return phaseShift.property();
	}

	private ShadowField<FloatProperty, Number> offset = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_OFFSET);

	@Override
	public final float getOffset() {
		return offset.get().floatValue();
	}

	@Override
	public final void setOffset(float offset) {
		this.offset.set(offset);
	}

	public final FloatProperty offsetProperty() {
		return offset.property();
	}

	private ShadowField<FloatProperty, Number> tint = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_TINT);

	@Override
	public final float getTint() {
		return tint.get().floatValue();
	}

	@Override
	public final void setTint(float tint) {
		this.tint.set(tint);
	}

	public final FloatProperty tintProperty() {
		return tint.property();
	}

	private ShadowField<FloatProperty, Number> blur = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLUR);

	@Override
	public final float getBlur() {
		return blur.get().floatValue();
	}

	@Override
	public final void setBlur(float blur) {
		this.blur.set(blur);
	}

	public final FloatProperty blurProperty() {
		return blur.property();
	}

	private ShadowField<FloatProperty, Number> bleed = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLEED);

	@Override
	public final float getBleed() {
		return bleed.get().floatValue();
	}

	@Override
	public final void setBleed(float bleed) {
		this.bleed.set(bleed);
	}

	public final FloatProperty bleedProperty() {
		return bleed.property();
	}

	private ShadowField<BooleanProperty, Boolean> turboTape = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_TURBO_TAPE);

	@Override
	public final boolean isTurboTape() {
		return turboTape.get();
	}

	@Override
	public final void setTurboTape(boolean turboTape) {
		this.turboTape.set(turboTape);
	}

	public final BooleanProperty turboTapeProperty() {
		return turboTape.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
