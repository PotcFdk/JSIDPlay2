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

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import de.schlichtherle.truezip.file.TFile;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.config.ISidPlay2Section;
import ui.common.FileToStringConverter;
import ui.favorites.PlaybackType;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	boolean DEFAULT_ENABLE_PROXY = false;
	public static final int DEFAULT_PROXY_PORT = 80;
	public static final PlaybackType DEFAULT_PLAYBACK_TYPE = PlaybackType.PLAYBACK_OFF;
	public static final int DEFAULT_FRAME_WIDTH = 1192;
	public static final int DEFAULT_FRAME_HEIGHT = 975;
	public static final int DEFAULT_FRAME_HEIGHT_MINIMIZED = 153;
	public static final boolean DEFAULT_FULL_SCREEN = false;
	public static final boolean DEFAULT_MINIMIZED = false;
	public static final float DEFAULT_VIDEO_SCALING = 2.5f;
	public static final boolean DEFAULT_SHOW_MONITOR = true;

	public SidPlay2Section() {
		Bindings.bindBidirectional(this.demos, demosFile, new FileToStringConverter());
		Bindings.bindBidirectional(this.hvmec, hvmecFile, new FileToStringConverter());
		Bindings.bindBidirectional(this.mags, magsFile, new FileToStringConverter());
		Bindings.bindBidirectional(this.hvsc, hvscFile, new FileToStringConverter());
		Bindings.bindBidirectional(this.cgsc, cgscFile, new FileToStringConverter());
		Bindings.bindBidirectional(this.gameBase64, gameBase64File, new FileToStringConverter());
	}

	private int version;

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	private BooleanProperty enableDatabaseProperty = new SimpleBooleanProperty(DEFAULT_ENABLE_DATABASE);

	@Override
	public boolean isEnableDatabase() {
		return enableDatabaseProperty.get();
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		enableDatabaseProperty.set(isEnableDatabase);
	}

	public BooleanProperty enableDatabaseProperty() {
		return enableDatabaseProperty;
	}

	private IntegerProperty startTimeProperty = new SimpleIntegerProperty(DEFAULT_START_TIME);

	@Override
	public int getStartTime() {
		return startTimeProperty.get();
	}

	@Override
	public void setStartTime(int startTime) {
		startTimeProperty.set(startTime);
	}

	public IntegerProperty startTimeProperty() {
		return startTimeProperty;
	}

	private IntegerProperty defaultPlayLengthProperty = new SimpleIntegerProperty(DEFAULT_PLAY_LENGTH);

	@Override
	public int getDefaultPlayLength() {
		return defaultPlayLengthProperty.get();
	}

	@Override
	public void setDefaultPlayLength(int defaultPlayLength) {
		defaultPlayLengthProperty.set(defaultPlayLength);
	}

	public IntegerProperty defaultPlayLengthProperty() {
		return defaultPlayLengthProperty;
	}

	private IntegerProperty fadeInTimeProperty = new SimpleIntegerProperty(DEFAULT_FADE_IN_TIME);

	@Override
	public int getFadeInTime() {
		return fadeInTimeProperty.get();
	}

	public void setFadeInTime(int fadeInTime) {
		fadeInTimeProperty.set(fadeInTime);
	}

	public IntegerProperty fadeInTimeProperty() {
		return fadeInTimeProperty;
	}

	private IntegerProperty fadeOutTimeProperty = new SimpleIntegerProperty(DEFAULT_FADE_OUT_TIME);

	@Override
	public int getFadeOutTime() {
		return fadeOutTimeProperty.get();
	}

	public void setFadeOutTime(int fadeOutTime) {
		fadeOutTimeProperty.set(fadeOutTime);
	}

	public IntegerProperty fadeOutTimeProperty() {
		return fadeOutTimeProperty;
	}

	private PlaybackType playbackType = DEFAULT_PLAYBACK_TYPE;

	@Enumerated(EnumType.STRING)
	public PlaybackType getPlaybackType() {
		return playbackType;
	}

	public void setPlaybackType(PlaybackType playbackType) {
		this.playbackType = playbackType;
	}

	private boolean loop = DEFAULT_LOOP;

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	private BooleanProperty singleProperty = new SimpleBooleanProperty(DEFAULT_SINGLE_TRACK);

	@Override
	public boolean isSingle() {
		return singleProperty.get();
	}

	@Override
	public void setSingle(boolean isSingle) {
		singleProperty.set(isSingle);
	}

	public BooleanProperty singleProperty() {
		return singleProperty;
	}

	private ObjectProperty<File> hvmecFile = new SimpleObjectProperty<File>();
	private StringProperty hvmec = new SimpleStringProperty();

	@Transient
	public File getHVMECFile() {
		return hvmecFile.get();
	}

	public String getHVMEC() {
		return hvmec.get();
	}

	public void setHVMEC(String hVMEC) {
		hvmec.set(hVMEC);
	}

	public StringProperty hvmecProperty() {
		return hvmec;
	}

	private ObjectProperty<File> demosFile = new SimpleObjectProperty<File>();
	private StringProperty demos = new SimpleStringProperty();

	@Transient
	public File getDemosFile() {
		return demosFile.get();
	}

	public String getDemos() {
		return demos.get();
	}

	public void setDemos(String demos) {
		this.demos.set(demos);
	}

	public StringProperty demosProperty() {
		return demos;
	}

	private ObjectProperty<File> magsFile = new SimpleObjectProperty<File>();
	private StringProperty mags = new SimpleStringProperty();

	public String getMags() {
		return mags.get();
	}

	@Transient
	public File getMagsFile() {
		return magsFile.get();
	}

	public void setMags(String mags) {
		this.mags.set(mags);
	}

	public StringProperty magsProperty() {
		return mags;
	}

	private ObjectProperty<File> cgscFile = new SimpleObjectProperty<File>();
	private StringProperty cgsc = new SimpleStringProperty();

	@Transient
	public File getCgscFile() {
		return cgscFile.get();
	}

	public String getCgsc() {
		return cgsc.get();
	}

	public void setCgsc(String cgsc) {
		this.cgsc.set(cgsc);
	}

	public StringProperty cgscProperty() {
		return cgsc;
	}

	private ObjectProperty<File> hvscFile = new SimpleObjectProperty<File>();
	private StringProperty hvsc = new SimpleStringProperty();

	@Transient
	public File getHvscFile() {
		return hvscFile.get();
	}

	@Override
	public String getHvsc() {
		return hvsc.get();
	}

	@Override
	public void setHvsc(String hvsc) {
		this.hvsc.set(hvsc);
	}

	public StringProperty hvscProperty() {
		return hvsc;
	}

	private ObjectProperty<File> gameBase64File = new SimpleObjectProperty<File>();
	private StringProperty gameBase64 = new SimpleStringProperty();

	@Transient
	public File getGameBase64File() {
		return gameBase64File.get();
	}

	public String getGameBase64() {
		return gameBase64.get();
	}

	public void setGameBase64(String gameBase64) {
		this.gameBase64.set(gameBase64);
	}

	private BooleanProperty enableProxyProperty = new SimpleBooleanProperty(DEFAULT_ENABLE_PROXY);

	public boolean isEnableProxy() {
		return enableProxyProperty.get();
	}

	public void setEnableProxy(boolean isEnableProxy) {
		singleProperty.set(isEnableProxy);
	}

	public BooleanProperty enableProxyProperty() {
		return enableProxyProperty;
	}

	private StringProperty proxyHostnameProperty = new SimpleStringProperty();

	public StringProperty proxyHostnameProperty() {
		return proxyHostnameProperty;
	}

	public String getProxyHostname() {
		return proxyHostnameProperty.get();
	}

	public void setProxyHostname(String hostname) {
		this.proxyHostnameProperty.set(hostname);
	}

	private ObjectProperty<Integer> proxyPortProperty = new SimpleObjectProperty<Integer>(DEFAULT_PROXY_PORT);

	public ObjectProperty<Integer> proxyPortProperty() {
		return proxyPortProperty;
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

	private String lastDirectory;

	@Override
	public String getLastDirectory() {
		return lastDirectory;
	}

	@Override
	public void setLastDirectory(String lastDirectory) {
		this.lastDirectory = lastDirectory;
	}

	@Transient
	public File getLastDirectoryFolder() {
		if (lastDirectory != null && new TFile(lastDirectory).isDirectory()) {
			return new TFile(lastDirectory);
		}
		return null;
	}

	private String tmpDir = System.getProperty("user.home") + System.getProperty("file.separator") + ".jsidplay2";

	@Override
	public String getTmpDir() {
		return tmpDir;
	}

	@Override
	public void setTmpDir(String tmpDir) {
		this.tmpDir = tmpDir;
	}

	private int frameX;

	public int getFrameX() {
		return frameX;
	}

	public void setFrameX(int frameX) {
		this.frameX = frameX;
	}

	private int frameY;

	public int getFrameY() {
		return frameY;
	}

	public void setFrameY(int frameY) {
		this.frameY = frameY;
	}

	private int frameWidth = DEFAULT_FRAME_WIDTH;

	public int getFrameWidth() {
		return frameWidth;
	}

	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	private int frameHeight = DEFAULT_FRAME_HEIGHT;

	public int getFrameHeight() {
		return frameHeight;
	}

	public void setFrameHeight(int frameHeight) {
		this.frameHeight = frameHeight;
	}

	private Boolean fullScreen = DEFAULT_FULL_SCREEN;

	public Boolean getFullScreen() {
		return fullScreen;
	}

	public void setFullScreen(Boolean fullScreen) {
		this.fullScreen = fullScreen;
	}

	private BooleanProperty minimizedProperty = new SimpleBooleanProperty(DEFAULT_MINIMIZED);

	public boolean isMinimized() {
		return minimizedProperty.get();
	}

	public void setMinimized(boolean isMinimized) {
		minimizedProperty.set(isMinimized);
	}

	public BooleanProperty minimizedProperty() {
		return minimizedProperty;
	}

	private FloatProperty videoScalingProperty = new SimpleFloatProperty(DEFAULT_VIDEO_SCALING);

	public float getVideoScaling() {
		return videoScalingProperty.get();
	}

	public void setVideoScaling(float videoScaling) {
		videoScalingProperty.set(videoScaling);
	}

	public FloatProperty videoScalingProperty() {
		return videoScalingProperty;
	}

	private ObjectProperty<Boolean> showMonitorProperty = new SimpleObjectProperty<Boolean>(DEFAULT_SHOW_MONITOR);

	public boolean isShowMonitor() {
		return showMonitorProperty.get();
	}

	public void setShowMonitor(boolean showMonitor) {
		showMonitorProperty.set(showMonitor);
	}

	public ObjectProperty<Boolean> showMonitorProperty() {
		return showMonitorProperty;
	}

	private FloatProperty brightnessProperty = new SimpleFloatProperty(DEFAULT_BRIGHTNESS);

	public float getBrightness() {
		return brightnessProperty.get();
	}

	public void setBrightness(float brightness) {
		this.brightnessProperty.set(brightness);
	}

	public final FloatProperty brightnessProperty() {
		return brightnessProperty;
	}

	private FloatProperty contrastProperty = new SimpleFloatProperty(DEFAULT_CONTRAST);

	public float getContrast() {
		return contrastProperty.get();
	}

	public void setContrast(float contrast) {
		this.contrastProperty.set(contrast);
	}

	public final FloatProperty contrastProperty() {
		return contrastProperty;
	}

	private FloatProperty gammaProperty = new SimpleFloatProperty(DEFAULT_GAMMA);

	public float getGamma() {
		return gammaProperty.get();
	}

	public void setGamma(float gamma) {
		this.gammaProperty.set(gamma);
	}

	public final FloatProperty gammaProperty() {
		return gammaProperty;
	}

	private FloatProperty saturationProperty = new SimpleFloatProperty(DEFAULT_SATURATION);

	public float getSaturation() {
		return saturationProperty.get();
	}

	public void setSaturation(float saturation) {
		this.saturationProperty.set(saturation);
	}

	public final FloatProperty saturationProperty() {
		return saturationProperty;
	}

	private FloatProperty phaseShiftProperty = new SimpleFloatProperty(DEFAULT_PHASE_SHIFT);

	public float getPhaseShift() {
		return phaseShiftProperty.get();
	}

	public void setPhaseShift(float phaseShift) {
		this.phaseShiftProperty.set(phaseShift);
	}

	public final FloatProperty phaseShiftProperty() {
		return phaseShiftProperty;
	}

	private FloatProperty offsetProperty = new SimpleFloatProperty(DEFAULT_OFFSET);

	public float getOffset() {
		return offsetProperty.get();
	}

	public void setOffset(float offset) {
		this.offsetProperty.set(offset);
	}

	public final FloatProperty offsetProperty() {
		return offsetProperty;
	}

	private FloatProperty tintProperty = new SimpleFloatProperty(DEFAULT_TINT);

	public float getTint() {
		return tintProperty.get();
	}

	public void setTint(float tint) {
		this.tintProperty.set(tint);
	}

	public final FloatProperty tintProperty() {
		return tintProperty;
	}

	private FloatProperty blurProperty = new SimpleFloatProperty(DEFAULT_BLUR);

	public float getBlur() {
		return blurProperty.get();
	}

	public void setBlur(float blur) {
		this.blurProperty.set(blur);
	}

	public final FloatProperty blurProperty() {
		return blurProperty;
	}

	private FloatProperty bleedProperty = new SimpleFloatProperty(DEFAULT_BLEED);

	public float getBleed() {
		return bleedProperty.get();
	}

	public void setBleed(float bleed) {
		this.bleedProperty.set(bleed);
	}

	public final FloatProperty bleedProperty() {
		return bleedProperty;
	}

	private BooleanProperty turboTapeProperty = new SimpleBooleanProperty(DEFAULT_TURBO_TAPE);

	@Override
	public boolean isTurboTape() {
		return turboTapeProperty.get();
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		turboTapeProperty.set(turboTape);
	}

	public BooleanProperty turboTapeProperty() {
		return turboTapeProperty;
	}
}
