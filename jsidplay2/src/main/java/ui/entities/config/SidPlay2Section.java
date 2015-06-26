package ui.entities.config;

import java.io.File;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import libsidplay.config.ISidPlay2Section;
import ui.favorites.PlaybackType;
import de.schlichtherle.truezip.file.TFile;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	public static final int DEFAULT_PROXY_PORT = 80;
	public static final PlaybackType DEFAULT_PLAYBACK_TYPE = PlaybackType.PLAYBACK_OFF;
	public static final int DEFAULT_FRAME_WIDTH = 1024;
	public static final int DEFAULT_FRAME_HEIGHT = 830;
	public static final boolean DEFAULT_FULL_SCREEN = false;
	public static final float DEFAULT_VIDEO_SCALING = 2.f;

	private int version;

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	private BooleanProperty enableDatabaseProperty = new SimpleBooleanProperty(
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
		return enableDatabaseProperty;
	}

	private IntegerProperty defaultPlayLengthProperty = new SimpleIntegerProperty(
			DEFAULT_PLAY_LENGTH);

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

	private IntegerProperty fadeInTimeProperty = new SimpleIntegerProperty(
			DEFAULT_FADE_IN_TIME);

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
	
	private IntegerProperty fadeOutTimeProperty = new SimpleIntegerProperty(
			DEFAULT_FADE_OUT_TIME);

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

	private BooleanProperty singleProperty = new SimpleBooleanProperty(
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
		return singleProperty;
	}

	private String HVMEC;

	public String getHVMEC() {
		return HVMEC;
	}

	public void setHVMEC(String hVMEC) {
		HVMEC = hVMEC;
	}

	private String demos;

	public String getDemos() {
		return demos;
	}

	public void setDemos(String demos) {
		this.demos = demos;
	}

	private String mags;

	public String getMags() {
		return mags;
	}

	public void setMags(String mags) {
		this.mags = mags;
	}

	private String cgsc;

	public String getCgsc() {
		return cgsc;
	}

	public void setCgsc(String cgsc) {
		this.cgsc = cgsc;
		getCgscFile();
	}

	private volatile File cgscFile;

	@Transient
	public File getCgscFile() {
		if (cgscFile == null && cgsc != null) {
			cgscFile = new TFile(cgsc);
		}
		return cgscFile;
	}

	private String hvsc;

	@Override
	public String getHvsc() {
		return hvsc;
	}

	@Override
	public void setHvsc(String hvsc) {
		this.hvsc = hvsc;
		getHvscFile();
	}

	private volatile File hvscFile;

	@Transient
	public File getHvscFile() {
		if (hvscFile == null && hvsc != null) {
			hvscFile = new TFile(hvsc);
		}
		return hvscFile;
	}

	private String gameBase64;

	public String getGameBase64() {
		return gameBase64;
	}

	public void setGameBase64(String gameBase64) {
		this.gameBase64 = gameBase64;
		getGameBase64File();
	}

	private volatile File gameBase64File;

	@Transient
	public File getGameBase64File() {
		if (gameBase64File == null && gameBase64 != null) {
			gameBase64File = new TFile(gameBase64);
		}
		return gameBase64File;
	}

	private boolean enableProxy = DEFAULT_ENABLE_PROXY;

	public boolean isEnableProxy() {
		return enableProxy;
	}

	public void setEnableProxy(boolean isEnableProxy) {
		this.enableProxy = isEnableProxy;
	}

	private String proxyHostname;

	public String getProxyHostname() {
		return proxyHostname;
	}

	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}

	private int proxyPort = DEFAULT_PROXY_PORT;

	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
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

	private String tmpDir = System.getProperty("user.home")
			+ System.getProperty("file.separator") + ".jsidplay2";

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

	private double videoScaling = DEFAULT_VIDEO_SCALING;

	public double getVideoScaling() {
		return videoScaling;
	}

	public void setVideoScaling(double videoScaling) {
		this.videoScaling = videoScaling;
	}

	private BooleanProperty turboTapeProperty = new SimpleBooleanProperty(
			DEFAULT_TURBO_TAPE);

	@Override
	public boolean isTurboTape() {
		return turboTapeProperty.get();
	}

	@Override
	public void setTurboTape(boolean turboTape) {
		turboTapeProperty.set(turboTape);
	}

}
