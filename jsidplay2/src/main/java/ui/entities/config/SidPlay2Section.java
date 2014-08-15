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

import sidplay.ini.intf.ISidPlay2Section;
import ui.favorites.PlaybackType;
import de.schlichtherle.truezip.file.TFile;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	private int version;

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	private BooleanProperty enableDatabaseProperty = new SimpleBooleanProperty(true);

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

	private IntegerProperty defaultPlayLengthProperty = new SimpleIntegerProperty(3 * 60);

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

	private PlaybackType playbackType = PlaybackType.NORMAL;

	@Enumerated(EnumType.STRING)
	public PlaybackType getPlaybackType() {
		return playbackType;
	}

	public void setPlaybackType(PlaybackType playbackType) {
		this.playbackType = playbackType;
	}

	private boolean loop;

	public boolean isLoop() {
		return loop;
	}

	public void setLoop(boolean loop) {
		this.loop = loop;
	}

	private String HVMEC;

	@Override
	public String getHVMEC() {
		return HVMEC;
	}

	@Override
	public void setHVMEC(String hVMEC) {
		HVMEC = hVMEC;
	}

	private String demos;

	@Override
	public String getDemos() {
		return demos;
	}

	@Override
	public void setDemos(String demos) {
		this.demos = demos;
	}

	private String mags;

	@Override
	public String getMags() {
		return mags;
	}

	@Override
	public void setMags(String mags) {
		this.mags = mags;
	}

	private String cgsc;

	@Override
	public String getCgsc() {
		return cgsc;
	}

	@Override
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

	private BooleanProperty singleProperty = new SimpleBooleanProperty();

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

	private boolean enableProxy;

	@Override
	public boolean isEnableProxy() {
		return enableProxy;
	}

	@Override
	public void setEnableProxy(boolean isEnableProxy) {
		this.enableProxy = isEnableProxy;
	}

	private String proxyHostname;

	@Override
	public String getProxyHostname() {
		return proxyHostname;
	}

	@Override
	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}

	private int proxyPort = 80;

	@Override
	public int getProxyPort() {
		return proxyPort;
	}

	@Override
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

	@Override
	public int getFrameX() {
		return frameX;
	}

	@Override
	public void setFrameX(int frameX) {
		this.frameX = frameX;
	}

	private int frameY;

	@Override
	public int getFrameY() {
		return frameY;
	}

	@Override
	public void setFrameY(int frameY) {
		this.frameY = frameY;
	}

	private int frameWidth = 1024;

	@Override
	public int getFrameWidth() {
		return frameWidth;
	}

	@Override
	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	private int frameHeight = 830;

	@Override
	public int getFrameHeight() {
		return frameHeight;
	}

	@Override
	public void setFrameHeight(int frameHeight) {
		this.frameHeight = frameHeight;
	}

	private Boolean fullScreen;

	public Boolean getFullScreen() {
		return fullScreen;
	}

	public void setFullScreen(Boolean fullScreen) {
		this.fullScreen = fullScreen;
	}

	private double videoScaling = 2.0;

	public double getVideoScaling() {
		return videoScaling;
	}

	public void setVideoScaling(double videoScaling) {
		this.videoScaling = videoScaling;
	}
}
