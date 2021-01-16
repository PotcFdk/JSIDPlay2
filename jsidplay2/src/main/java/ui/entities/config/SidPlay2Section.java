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
import static sidplay.ini.IniDefaults.DEFAULT_TURBO_TAPE;

import java.io.File;

import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import de.schlichtherle.truezip.file.TFile;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.config.ISidPlay2Section;
import ui.common.converter.FileAttributeConverter;
import ui.common.converter.FileXmlAdapter;
import ui.common.properties.ShadowField;
import ui.favorites.PlaybackType;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	public static final PlaybackType DEFAULT_PLAYBACK_TYPE = PlaybackType.PLAYBACK_OFF;
	public static final boolean DEFAULT_ENABLE_PROXY = false;
	public static final int DEFAULT_PROXY_PORT = 80;
	public static final String DEFAULT_TMP_DIR = System.getProperty("user.home") + System.getProperty("file.separator")
			+ ".jsidplay2";
	public static final int DEFAULT_FRAME_X = 0;
	public static final int DEFAULT_FRAME_Y = 0;
	public static final int DEFAULT_FRAME_WIDTH = 1310;
	public static final int DEFAULT_FRAME_HEIGHT = 1024;
	public static final boolean DEFAULT_MINIMIZED = false;
	public static final float DEFAULT_VIDEO_SCALING = 2f;
	public static final boolean DEFAULT_SHOW_MONITOR = true;

	private int version;

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	private ShadowField<BooleanProperty, Boolean> enableDatabase = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_ENABLE_DATABASE);

	@Override
	public boolean isEnableDatabase() {
		return enableDatabase.get();
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		enableDatabase.set(isEnableDatabase);
	}

	public BooleanProperty enableDatabaseProperty() {
		return enableDatabase.property();
	}

	private ShadowField<DoubleProperty, Number> startTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_START_TIME);

	@Override
	public double getStartTime() {
		return startTime.get().doubleValue();
	}

	@Override
	public void setStartTime(double startTime) {
		this.startTime.set(startTime);
	}

	public DoubleProperty startTimeProperty() {
		return startTime.property();
	}

	private ShadowField<DoubleProperty, Number> defaultPlayLength = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_PLAY_LENGTH);

	@Override
	public double getDefaultPlayLength() {
		return defaultPlayLength.get().doubleValue();
	}

	@Override
	public void setDefaultPlayLength(double defaultPlayLength) {
		this.defaultPlayLength.set(defaultPlayLength);
	}

	public DoubleProperty defaultPlayLengthProperty() {
		return defaultPlayLength.property();
	}

	private ShadowField<DoubleProperty, Number> fadeInTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_IN_TIME);

	@Override
	public double getFadeInTime() {
		return fadeInTime.get().doubleValue();
	}

	@Override
	public void setFadeInTime(double fadeInTime) {
		this.fadeInTime.set(fadeInTime);
	}

	public DoubleProperty fadeInTimeProperty() {
		return fadeInTime.property();
	}

	private ShadowField<DoubleProperty, Number> fadeOutTime = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_OUT_TIME);

	@Override
	public double getFadeOutTime() {
		return fadeOutTime.get().doubleValue();
	}

	@Override
	public void setFadeOutTime(double fadeOutTime) {
		this.fadeOutTime.set(fadeOutTime);
	}

	public DoubleProperty fadeOutTimeProperty() {
		return fadeOutTime.property();
	}

	private ShadowField<ObjectProperty<PlaybackType>, PlaybackType> playbackType = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_PLAYBACK_TYPE);

	@Enumerated(EnumType.STRING)
	public PlaybackType getPlaybackType() {
		return playbackType.get();
	}

	public void setPlaybackType(PlaybackType playbackType) {
		this.playbackType.set(playbackType);
	}

	public ObjectProperty<PlaybackType> playbackTypeProperty() {
		return playbackType.property();
	}

	private ShadowField<BooleanProperty, Boolean> loop = new ShadowField<>(SimpleBooleanProperty::new, DEFAULT_LOOP);

	@Override
	public boolean isLoop() {
		return loop.get();
	}

	@Override
	public void setLoop(boolean loop) {
		this.loop.set(loop);
	}

	public BooleanProperty loopProperty() {
		return loop.property();
	}

	private ShadowField<BooleanProperty, Boolean> single = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_SINGLE_TRACK);

	@Override
	public boolean isSingle() {
		return single.get();
	}

	@Override
	public void setSingle(boolean isSingle) {
		single.set(isSingle);
	}

	public BooleanProperty singleProperty() {
		return single.property();
	}

	private ShadowField<ObjectProperty<File>, File> hvmec = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getHVMEC() {
		return hvmec.get();
	}

	public void setHVMEC(File hVMEC) {
		hvmec.set(hVMEC);
	}

	public ObjectProperty<File> hvmecProperty() {
		return hvmec.property();
	}

	private ShadowField<ObjectProperty<File>, File> demos = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getDemos() {
		return demos.get();
	}

	public void setDemos(File demos) {
		this.demos.set(demos);
	}

	public ObjectProperty<File> demosProperty() {
		return demos.property();
	}

	private ShadowField<ObjectProperty<File>, File> mags = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getMags() {
		return mags.get();
	}

	public void setMags(File mags) {
		this.mags.set(mags);
	}

	public ObjectProperty<File> magsProperty() {
		return mags.property();
	}

	private ShadowField<ObjectProperty<File>, File> cgsc = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getCgsc() {
		return cgsc.get();
	}

	public void setCgsc(File cgsc) {
		this.cgsc.set(cgsc);
	}

	public ObjectProperty<File> cgscProperty() {
		return cgsc.property();
	}

	private ShadowField<ObjectProperty<File>, File> hvsc = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@Override
	public File getHvsc() {
		return hvsc.get();
	}

	@Override
	public void setHvsc(File hvsc) {
		this.hvsc.set(hvsc);
	}

	public ObjectProperty<File> hvscProperty() {
		return hvsc.property();
	}

	private ShadowField<ObjectProperty<File>, File> gameBase64 = new ShadowField<>(SimpleObjectProperty::new, null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getGameBase64() {
		return gameBase64.get();
	}

	public void setGameBase64(File gameBase64) {
		this.gameBase64.set(gameBase64);
	}

	public ObjectProperty<File> gameBase64Property() {
		return gameBase64.property();
	}

	private ShadowField<BooleanProperty, Boolean> enableProxy = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_ENABLE_PROXY);

	public boolean isEnableProxy() {
		return enableProxy.get();
	}

	public void setEnableProxy(boolean isEnableProxy) {
		single.set(isEnableProxy);
	}

	public BooleanProperty enableProxyProperty() {
		return enableProxy.property();
	}

	private ShadowField<StringProperty, String> proxyHostname = new ShadowField<>(SimpleStringProperty::new, null);

	public StringProperty proxyHostnameProperty() {
		return proxyHostname.property();
	}

	public String getProxyHostname() {
		return proxyHostname.get();
	}

	public void setProxyHostname(String hostname) {
		this.proxyHostname.set(hostname);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> proxyPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_PROXY_PORT);

	public ObjectProperty<Integer> proxyPortProperty() {
		return proxyPort.property();
	}

	public int getProxyPort() {
		return proxyPort.get();
	}

	public void setProxyPort(int port) {
		this.proxyPort.set(port);
	}

	private ShadowField<ObjectProperty<File>, File> lastDirectory = new ShadowField<>(SimpleObjectProperty::new, null);

	@Override
	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	public File getLastDirectory() {
		return lastDirectory.get();
	}

	@Override
	public void setLastDirectory(File lastDirectory) {
		this.lastDirectory.set(lastDirectory);
	}

	public ObjectProperty<File> lastDirectoryProperty() {
		return lastDirectory.property();
	}

	private ShadowField<StringProperty, String> tmpDir = new ShadowField<>(SimpleStringProperty::new, DEFAULT_TMP_DIR);

	@Override
	public String getTmpDir() {
		return tmpDir.get();
	}

	@Override
	public void setTmpDir(String tmpDir) {
		this.tmpDir.set(tmpDir);
	}

	public StringProperty tmpDirProperty() {
		return tmpDir.property();
	}

	private ShadowField<IntegerProperty, Number> frameX = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_X);

	public int getFrameX() {
		return frameX.get().intValue();
	}

	public void setFrameX(int frameX) {
		this.frameX.set(frameX);
	}

	public IntegerProperty frameXProperty() {
		return frameX.property();
	}

	private ShadowField<IntegerProperty, Number> frameY = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_Y);

	public int getFrameY() {
		return frameY.get().intValue();
	}

	public void setFrameY(int frameY) {
		this.frameY.set(frameY);
	}

	public IntegerProperty frameYProperty() {
		return frameY.property();
	}

	private ShadowField<IntegerProperty, Number> frameWidth = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_WIDTH);

	public int getFrameWidth() {
		return frameWidth.get().intValue();
	}

	public void setFrameWidth(int frameWidth) {
		this.frameWidth.set(frameWidth);
	}

	public IntegerProperty frameWidthProperty() {
		return frameWidth.property();
	}

	private ShadowField<IntegerProperty, Number> frameHeight = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_HEIGHT);

	public int getFrameHeight() {
		return frameHeight.get().intValue();
	}

	public void setFrameHeight(int frameHeight) {
		this.frameHeight.set(frameHeight);
	}

	public IntegerProperty frameHeightProperty() {
		return frameHeight.property();
	}

	private ShadowField<BooleanProperty, Boolean> minimized = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MINIMIZED);

	public boolean isMinimized() {
		return minimized.get();
	}

	public void setMinimized(boolean isMinimized) {
		minimized.set(isMinimized);
	}

	public BooleanProperty minimizedProperty() {
		return minimized.property();
	}

	private ShadowField<IntegerProperty, Number> minimizedWidth = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), 0);

	public int getMinimizedWidth() {
		return minimizedWidth.get().intValue();
	}

	public void setMinimizedWidth(int minimizedWidth) {
		this.minimizedWidth.set(minimizedWidth);
	}

	public IntegerProperty minimizedWidthProperty() {
		return minimizedWidth.property();
	}

	private ShadowField<IntegerProperty, Number> minimizedProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), 0);

	public int getMinimizedHeight() {
		return minimizedProperty.get().intValue();
	}

	public void setMinimizedHeight(int minimizedHeight) {
		this.minimizedProperty.set(minimizedHeight);
	}

	private ShadowField<FloatProperty, Number> videoScaling = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_VIDEO_SCALING);

	public float getVideoScaling() {
		return videoScaling.get().floatValue();
	}

	public void setVideoScaling(float videoScaling) {
		this.videoScaling.set(videoScaling);
	}

	public FloatProperty videoScalingProperty() {
		return videoScaling.property();
	}

	private ShadowField<ObjectProperty<Boolean>, Boolean> showMonitor = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_SHOW_MONITOR);

	public boolean isShowMonitor() {
		return showMonitor.get();
	}

	public void setShowMonitor(boolean showMonitor) {
		this.showMonitor.set(showMonitor);
	}

	public ObjectProperty<Boolean> showMonitorProperty() {
		return showMonitor.property();
	}

	private ShadowField<BooleanProperty, Boolean> palEmulation = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PAL_EMULATION);

	public BooleanProperty palEmulationProperty() {
		return palEmulation.property();
	}

	@Override
	public boolean isPalEmulation() {
		return palEmulation.get();
	}

	@Override
	public void setPalEmulation(boolean isPalEmulation) {
		palEmulation.set(isPalEmulation);
	}

	private ShadowField<FloatProperty, Number> brightness = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BRIGHTNESS);

	@Override
	public float getBrightness() {
		return brightness.get().floatValue();
	}

	@Override
	public void setBrightness(float brightness) {
		this.brightness.set(brightness);
	}

	public final FloatProperty brightnessProperty() {
		return brightness.property();
	}

	private ShadowField<FloatProperty, Number> contrast = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_CONTRAST);

	@Override
	public float getContrast() {
		return contrast.get().floatValue();
	}

	@Override
	public void setContrast(float contrast) {
		this.contrast.set(contrast);
	}

	public final FloatProperty contrastProperty() {
		return contrast.property();
	}

	private ShadowField<FloatProperty, Number> gamma = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_GAMMA);

	@Override
	public float getGamma() {
		return gamma.get().floatValue();
	}

	@Override
	public void setGamma(float gamma) {
		this.gamma.set(gamma);
	}

	public final FloatProperty gammaProperty() {
		return gamma.property();
	}

	private ShadowField<FloatProperty, Number> saturation = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SATURATION);

	@Override
	public float getSaturation() {
		return saturation.get().floatValue();
	}

	@Override
	public void setSaturation(float saturation) {
		this.saturation.set(saturation);
	}

	public final FloatProperty saturationProperty() {
		return saturation.property();
	}

	private ShadowField<FloatProperty, Number> phaseShift = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_PHASE_SHIFT);

	@Override
	public float getPhaseShift() {
		return phaseShift.get().floatValue();
	}

	@Override
	public void setPhaseShift(float phaseShift) {
		this.phaseShift.set(phaseShift);
	}

	public final FloatProperty phaseShiftProperty() {
		return phaseShift.property();
	}

	private ShadowField<FloatProperty, Number> offset = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_OFFSET);

	@Override
	public float getOffset() {
		return offset.get().floatValue();
	}

	@Override
	public void setOffset(float offset) {
		this.offset.set(offset);
	}

	public final FloatProperty offsetProperty() {
		return offset.property();
	}

	private ShadowField<FloatProperty, Number> tint = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_TINT);

	@Override
	public float getTint() {
		return tint.get().floatValue();
	}

	@Override
	public void setTint(float tint) {
		this.tint.set(tint);
	}

	public final FloatProperty tintProperty() {
		return tint.property();
	}

	private ShadowField<FloatProperty, Number> blur = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLUR);

	@Override
	public float getBlur() {
		return blur.get().floatValue();
	}

	@Override
	public void setBlur(float blur) {
		this.blur.set(blur);
	}

	public final FloatProperty blurProperty() {
		return blur.property();
	}

	private ShadowField<FloatProperty, Number> bleed = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLEED);

	@Override
	public float getBleed() {
		return bleed.get().floatValue();
	}

	@Override
	public void setBleed(float bleed) {
		this.bleed.set(bleed);
	}

	public final FloatProperty bleedProperty() {
		return bleed.property();
	}

	private ShadowField<BooleanProperty, Boolean> turboTape = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_TURBO_TAPE);

	@Override
	public boolean isTurboTape() {
		return turboTape.get();
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		this.turboTape.set(turboTape);
	}

	public BooleanProperty turboTapeProperty() {
		return turboTape.property();
	}

	@Transient
	public File getLastDirectoryFolder() {
		if (getLastDirectory() != null && new TFile(getLastDirectory()).isDirectory()) {
			return new TFile(getLastDirectory());
		}
		return null;
	}

}
