package ui.entities.config;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;

import sidplay.ini.intf.ISidPlay2Section;
import ui.favorites.PlaybackType;
import ui.favorites.RepeatType;
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

	private boolean enableDatabase = true;

	@Override
	public boolean isEnableDatabase() {
		return enableDatabase;
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		this.enableDatabase = isEnableDatabase;
	}

	private int playLength;

	@Override
	public int getPlayLength() {
		return playLength;
	}

	@Override
	public void setPlayLength(int playLength) {
		this.playLength = playLength;
	}

	private int recordLength;

	@Override
	public int getRecordLength() {
		return recordLength;
	}

	@Override
	public void setRecordLength(int recordLength) {
		this.recordLength = recordLength;
	}

	@Enumerated(EnumType.STRING)
	private PlaybackType playbackType = PlaybackType.NORMAL;

	public PlaybackType getPlaybackType() {
		return playbackType;
	}

	public void setPlaybackType(PlaybackType playbackType) {
		this.playbackType = playbackType;
	}

	@Enumerated(EnumType.STRING)
	private RepeatType repeatType = RepeatType.REPEAT_OFF;

	public RepeatType getRepeatType() {
		return repeatType;
	}

	public void setRepeatType(RepeatType repeatType) {
		this.repeatType = repeatType;
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
		this.cgscFile = new TFile(cgsc);
	}

	@Transient
	private volatile File cgscFile;

	public File getCgscFile() {
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
		this.hvscFile= new TFile(hvsc);
	}

	@Transient
	private volatile File hvscFile;

	public File getHvscFile() {
		return hvscFile;
	}

	private boolean single;

	@Override
	public boolean isSingle() {
		return single;
	}

	@Override
	public void setSingle(boolean isSingle) {
		this.single = isSingle;
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
	
	@Transient
	private volatile File lastDirectoryFile;

	@Override
	public String getLastDirectory() {
		return lastDirectory;
	}

	@Override
	public void setLastDirectory(String lastDirectory) {
		this.lastDirectory = lastDirectory;
		lastDirectoryFile = new TFile(lastDirectory);
	}

	public File getLastDirectoryFolder() {
		if (lastDirectoryFile != null && lastDirectoryFile.isFile()) {
			return lastDirectoryFile.getParentFile();
		}
		return lastDirectoryFile;
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

	private int frameHeight = 768;

	@Override
	public int getFrameHeight() {
		return frameHeight;
	}

	@Override
	public void setFrameHeight(int frameHeight) {
		this.frameHeight = frameHeight;
	}

	private double videoScaling = 2.0;

	public double getVideoScaling() {
		return videoScaling;
	}

	public void setVideoScaling(double videoScaling) {
		this.videoScaling = videoScaling;
	}
}
