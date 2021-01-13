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
import java.net.InetSocketAddress;
import java.net.Proxy;

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

	private static final String DEF_TMP_DIR = System.getProperty("user.home") + System.getProperty("file.separator")
			+ ".jsidplay2";
	boolean DEFAULT_ENABLE_PROXY = false;
	public static final int DEFAULT_PROXY_PORT = 80;
	public static final PlaybackType DEFAULT_PLAYBACK_TYPE = PlaybackType.PLAYBACK_OFF;
	public static final int DEF_FRAME_X = 0;
	public static final int DEF_FRAME_Y = 0;
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

	private ShadowField<BooleanProperty, Boolean> enableDatabaseProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_ENABLE_DATABASE);

	@Override
	public boolean isEnableDatabase() {
		return enableDatabaseProperty.get();
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		enableDatabaseProperty.set(isEnableDatabase);
	}

	public BooleanProperty enableDatabaseProperty() {
		return enableDatabaseProperty.property();
	}

	private ShadowField<DoubleProperty, Number> startTimeProperty = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_START_TIME);

	@Override
	public double getStartTime() {
		return startTimeProperty.get().doubleValue();
	}

	@Override
	public void setStartTime(double startTime) {
		startTimeProperty.set(startTime);
	}

	public DoubleProperty startTimeProperty() {
		return startTimeProperty.property();
	}

	private ShadowField<DoubleProperty, Number> defaultPlayLengthProperty = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_PLAY_LENGTH);

	@Override
	public double getDefaultPlayLength() {
		return defaultPlayLengthProperty.get().doubleValue();
	}

	@Override
	public void setDefaultPlayLength(double defaultPlayLength) {
		defaultPlayLengthProperty.set(defaultPlayLength);
	}

	public DoubleProperty defaultPlayLengthProperty() {
		return defaultPlayLengthProperty.property();
	}

	private ShadowField<DoubleProperty, Number> fadeInTimeProperty = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_IN_TIME);

	@Override
	public double getFadeInTime() {
		return fadeInTimeProperty.get().doubleValue();
	}

	@Override
	public void setFadeInTime(double fadeInTime) {
		fadeInTimeProperty.set(fadeInTime);
	}

	public DoubleProperty fadeInTimeProperty() {
		return fadeInTimeProperty.property();
	}

	private ShadowField<DoubleProperty, Number> fadeOutTimeProperty = new ShadowField<>(
			number -> new SimpleDoubleProperty(number.doubleValue()), DEFAULT_FADE_OUT_TIME);

	@Override
	public double getFadeOutTime() {
		return fadeOutTimeProperty.get().doubleValue();
	}

	@Override
	public void setFadeOutTime(double fadeOutTime) {
		fadeOutTimeProperty.set(fadeOutTime);
	}

	public DoubleProperty fadeOutTimeProperty() {
		return fadeOutTimeProperty.property();
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

	private ShadowField<BooleanProperty, Boolean> loopProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_LOOP);

	@Override
	public boolean isLoop() {
		return loopProperty.get();
	}

	@Override
	public void setLoop(boolean loop) {
		this.loopProperty.set(loop);
	}

	public BooleanProperty loopProperty() {
		return loopProperty.property();
	}

	private ShadowField<BooleanProperty, Boolean> singleProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_SINGLE_TRACK);

	@Override
	public boolean isSingle() {
		return singleProperty.get();
	}

	@Override
	public void setSingle(boolean isSingle) {
		singleProperty.set(isSingle);
	}

	public BooleanProperty singleProperty() {
		return singleProperty.property();
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

	private ShadowField<BooleanProperty, Boolean> enableProxyProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_ENABLE_PROXY);

	public boolean isEnableProxy() {
		return enableProxyProperty.get();
	}

	public void setEnableProxy(boolean isEnableProxy) {
		singleProperty.set(isEnableProxy);
	}

	public BooleanProperty enableProxyProperty() {
		return enableProxyProperty.property();
	}

	private ShadowField<StringProperty, String> proxyHostnameProperty = new ShadowField<>(SimpleStringProperty::new,
			null);

	public StringProperty proxyHostnameProperty() {
		return proxyHostnameProperty.property();
	}

	public String getProxyHostname() {
		return proxyHostnameProperty.get();
	}

	public void setProxyHostname(String hostname) {
		this.proxyHostnameProperty.set(hostname);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> proxyPortProperty = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_PROXY_PORT);

	public ObjectProperty<Integer> proxyPortProperty() {
		return proxyPortProperty.property();
	}

	public int getProxyPort() {
		return proxyPortProperty.get();
	}

	public void setProxyPort(int port) {
		this.proxyPortProperty.set(port);
	}

	@Transient
	public Proxy getProxy() {
		if (isEnableProxy()) {
			return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(getProxyHostname(), getProxyPort()));
		} else {
			return Proxy.NO_PROXY;
		}
	}

	private ShadowField<StringProperty, String> lastDirectoryProperty = new ShadowField<>(SimpleStringProperty::new,
			null);

	@Override
	public String getLastDirectory() {
		return lastDirectoryProperty.get();
	}

	@Override
	public void setLastDirectory(String lastDirectory) {
		this.lastDirectoryProperty.set(lastDirectory);
	}

	public StringProperty lastDirectoryProperty() {
		return lastDirectoryProperty.property();
	}

	@Transient
	public File getLastDirectoryFolder() {
		if (getLastDirectory() != null && new TFile(getLastDirectory()).isDirectory()) {
			return new TFile(getLastDirectory());
		}
		return null;
	}

	private ShadowField<StringProperty, String> tmpDirProperty = new ShadowField<>(SimpleStringProperty::new,
			DEF_TMP_DIR);

	@Override
	public String getTmpDir() {
		return tmpDirProperty.get();
	}

	@Override
	public void setTmpDir(String tmpDir) {
		this.tmpDirProperty.set(tmpDir);
	}

	public StringProperty tmpDirProperty() {
		return tmpDirProperty.property();
	}

	private ShadowField<IntegerProperty, Number> frameXProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEF_FRAME_X);

	public int getFrameX() {
		return frameXProperty.get().intValue();
	}

	public void setFrameX(int frameX) {
		this.frameXProperty.set(frameX);
	}

	public IntegerProperty frameXProperty() {
		return frameXProperty.property();
	}

	private ShadowField<IntegerProperty, Number> frameYProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEF_FRAME_Y);

	public int getFrameY() {
		return frameYProperty.get().intValue();
	}

	public void setFrameY(int frameY) {
		this.frameYProperty.set(frameY);
	}

	public IntegerProperty frameYProperty() {
		return frameYProperty.property();
	}

	private ShadowField<IntegerProperty, Number> frameWidthProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_WIDTH);

	public int getFrameWidth() {
		return frameWidthProperty.get().intValue();
	}

	public void setFrameWidth(int frameWidth) {
		this.frameWidthProperty.set(frameWidth);
	}

	public IntegerProperty frameWidthProperty() {
		return frameWidthProperty.property();
	}

	private ShadowField<IntegerProperty, Number> frameHeightProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_FRAME_HEIGHT);

	public int getFrameHeight() {
		return frameHeightProperty.get().intValue();
	}

	public void setFrameHeight(int frameHeight) {
		this.frameHeightProperty.set(frameHeight);
	}

	public IntegerProperty frameHeightProperty() {
		return frameHeightProperty.property();
	}

	private ShadowField<BooleanProperty, Boolean> minimizedProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MINIMIZED);

	public boolean isMinimized() {
		return minimizedProperty.get();
	}

	public void setMinimized(boolean isMinimized) {
		minimizedProperty.set(isMinimized);
	}

	public BooleanProperty minimizedProperty() {
		return minimizedProperty.property();
	}

	private ShadowField<IntegerProperty, Number> minimizedWidthProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), 0);

	public int getMinimizedWidth() {
		return minimizedWidthProperty.get().intValue();
	}

	public void setMinimizedWidth(int minimizedWidth) {
		this.minimizedWidthProperty.set(minimizedWidth);
	}

	public IntegerProperty minimizedWidthProperty() {
		return minimizedWidthProperty.property();
	}

	private ShadowField<IntegerProperty, Number> minimizedHeightProperty = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), 0);

	public int getMinimizedHeight() {
		return minimizedHeightProperty.get().intValue();
	}

	public void setMinimizedHeight(int minimizedHeight) {
		this.minimizedHeightProperty.set(minimizedHeight);
	}

	private ShadowField<FloatProperty, Number> videoScalingProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_VIDEO_SCALING);

	public float getVideoScaling() {
		return videoScalingProperty.get().floatValue();
	}

	public void setVideoScaling(float videoScaling) {
		videoScalingProperty.set(videoScaling);
	}

	public FloatProperty videoScalingProperty() {
		return videoScalingProperty.property();
	}

	private ShadowField<ObjectProperty<Boolean>, Boolean> showMonitorProperty = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SHOW_MONITOR);

	public boolean isShowMonitor() {
		return showMonitorProperty.get();
	}

	public void setShowMonitor(boolean showMonitor) {
		showMonitorProperty.set(showMonitor);
	}

	public ObjectProperty<Boolean> showMonitorProperty() {
		return showMonitorProperty.property();
	}

	private ShadowField<BooleanProperty, Boolean> palEmulationProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_PAL_EMULATION);

	public BooleanProperty palEmulationProperty() {
		return palEmulationProperty.property();
	}

	@Override
	public boolean isPalEmulation() {
		return palEmulationProperty.get();
	}

	@Override
	public void setPalEmulation(boolean isPalEmulation) {
		palEmulationProperty.set(isPalEmulation);
	}

	private ShadowField<FloatProperty, Number> brightnessProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BRIGHTNESS);

	@Override
	public float getBrightness() {
		return brightnessProperty.get().floatValue();
	}

	@Override
	public void setBrightness(float brightness) {
		this.brightnessProperty.set(brightness);
	}

	public final FloatProperty brightnessProperty() {
		return brightnessProperty.property();
	}

	private ShadowField<FloatProperty, Number> contrastProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_CONTRAST);

	@Override
	public float getContrast() {
		return contrastProperty.get().floatValue();
	}

	@Override
	public void setContrast(float contrast) {
		this.contrastProperty.set(contrast);
	}

	public final FloatProperty contrastProperty() {
		return contrastProperty.property();
	}

	private ShadowField<FloatProperty, Number> gammaProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_GAMMA);

	@Override
	public float getGamma() {
		return gammaProperty.get().floatValue();
	}

	@Override
	public void setGamma(float gamma) {
		this.gammaProperty.set(gamma);
	}

	public final FloatProperty gammaProperty() {
		return gammaProperty.property();
	}

	private ShadowField<FloatProperty, Number> saturationProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_SATURATION);

	@Override
	public float getSaturation() {
		return saturationProperty.get().floatValue();
	}

	@Override
	public void setSaturation(float saturation) {
		this.saturationProperty.set(saturation);
	}

	public final FloatProperty saturationProperty() {
		return saturationProperty.property();
	}

	private ShadowField<FloatProperty, Number> phaseShiftProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_PHASE_SHIFT);

	@Override
	public float getPhaseShift() {
		return phaseShiftProperty.get().floatValue();
	}

	@Override
	public void setPhaseShift(float phaseShift) {
		this.phaseShiftProperty.set(phaseShift);
	}

	public final FloatProperty phaseShiftProperty() {
		return phaseShiftProperty.property();
	}

	private ShadowField<FloatProperty, Number> offsetProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_OFFSET);

	@Override
	public float getOffset() {
		return offsetProperty.get().floatValue();
	}

	@Override
	public void setOffset(float offset) {
		this.offsetProperty.set(offset);
	}

	public final FloatProperty offsetProperty() {
		return offsetProperty.property();
	}

	private ShadowField<FloatProperty, Number> tintProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_TINT);

	@Override
	public float getTint() {
		return tintProperty.get().floatValue();
	}

	@Override
	public void setTint(float tint) {
		this.tintProperty.set(tint);
	}

	public final FloatProperty tintProperty() {
		return tintProperty.property();
	}

	private ShadowField<FloatProperty, Number> blurProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLUR);

	@Override
	public float getBlur() {
		return blurProperty.get().floatValue();
	}

	@Override
	public void setBlur(float blur) {
		this.blurProperty.set(blur);
	}

	public final FloatProperty blurProperty() {
		return blurProperty.property();
	}

	private ShadowField<FloatProperty, Number> bleedProperty = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_BLEED);

	@Override
	public float getBleed() {
		return bleedProperty.get().floatValue();
	}

	@Override
	public void setBleed(float bleed) {
		this.bleedProperty.set(bleed);
	}

	public final FloatProperty bleedProperty() {
		return bleedProperty.property();
	}

	private ShadowField<BooleanProperty, Boolean> turboTapeProperty = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_TURBO_TAPE);

	@Override
	public boolean isTurboTape() {
		return turboTapeProperty.get();
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		turboTapeProperty.set(turboTape);
	}

	public BooleanProperty turboTapeProperty() {
		return turboTapeProperty.property();
	}
}
