package applet.entities.config;

import java.io.File;

import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.swing.JFileChooser;

import sidplay.ini.intf.ISidPlay2Section;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigFieldType;
import applet.config.annotations.ConfigTransient;
import applet.favorites.PlaybackType;
import applet.favorites.RepeatType;

@Embeddable
public class SidPlay2Section implements ISidPlay2Section {

	@ConfigTransient
	private int version;

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public void setVersion(int version) {
		this.version = version;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_ENABLE_DATABASE_DESC", toolTipBundleKey = "JSIDPLAY2_ENABLE_DATABASE_TOOLTIP")
	private boolean enableDatabase = true;

	@Override
	public boolean isEnableDatabase() {
		return enableDatabase;
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		this.enableDatabase = isEnableDatabase;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_PLAY_LENGTH_DESC", toolTipBundleKey = "JSIDPLAY2_PLAY_LENGTH_TOOLTIP")
	private int playLength;

	@Override
	public int getPlayLength() {
		return playLength;
	}

	@Override
	public void setPlayLength(int playLength) {
		this.playLength = playLength;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_RECORD_LENGTH_DESC", toolTipBundleKey = "JSIDPLAY2_RECORD_LENGTH_TOOLTIP")
	private int recordLength;

	@Override
	public int getRecordLength() {
		return recordLength;
	}

	@Override
	public void setRecordLength(int recordLength) {
		this.recordLength = recordLength;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_PLAYBACK_TYPE_DESC", toolTipBundleKey = "JSIDPLAY2_PLAYBACK_TYPE_TOOLTIP")
	@Enumerated(EnumType.STRING)
	private PlaybackType playbackType = PlaybackType.NORMAL;

	public PlaybackType getPlaybackType() {
		return playbackType;
	}

	public void setPlaybackType(PlaybackType playbackType) {
		this.playbackType = playbackType;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_REPEAT_TYPE_DESC", toolTipBundleKey = "JSIDPLAY2_REPEAT_TYPE_TOOLTIP")
	@Enumerated(EnumType.STRING)
	private RepeatType repeatType = RepeatType.REPEAT_OFF;

	public RepeatType getRepeatType() {
		return repeatType;
	}

	public void setRepeatType(RepeatType repeatType) {
		this.repeatType = repeatType;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(bundleKey = "JSIDPLAY2_HVMEC_DESC", toolTipBundleKey = "JSIDPLAY2_HVMEC_TOOLTIP")
	private String HVMEC;

	@Override
	public String getHVMEC() {
		return HVMEC;
	}

	@Override
	public void setHVMEC(String hVMEC) {
		HVMEC = hVMEC;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(bundleKey = "JSIDPLAY2_DEMOS_DESC", toolTipBundleKey = "JSIDPLAY2_DEMOS_TOOLTIP")
	private String demos;

	@Override
	public String getDemos() {
		return demos;
	}

	@Override
	public void setDemos(String demos) {
		this.demos = demos;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(bundleKey = "JSIDPLAY2_MAGS_DESC", toolTipBundleKey = "JSIDPLAY2_MAGS_TOOLTIP")
	private String mags;

	@Override
	public String getMags() {
		return mags;
	}

	@Override
	public void setMags(String mags) {
		this.mags = mags;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(bundleKey = "JSIDPLAY2_CGSC_DESC", toolTipBundleKey = "JSIDPLAY2_CGSC_TOOLTIP")
	private String cgsc;

	@Override
	public String getCgsc() {
		return cgsc;
	}

	@Override
	public void setCgsc(String cgsc) {
		this.cgsc = cgsc;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(bundleKey = "JSIDPLAY2_HVSC_DESC", toolTipBundleKey = "JSIDPLAY2_HVSC_TOOLTIP")
	private String hvsc;

	@Override
	public String getHvsc() {
		return hvsc;
	}

	@Override
	public void setHvsc(String hvsc) {
		this.hvsc = hvsc;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_SINGLE_DESC", toolTipBundleKey = "JSIDPLAY2_SINGLE_TOOLTIP")
	private boolean single;

	@Override
	public boolean isSingle() {
		return single;
	}

	@Override
	public void setSingle(boolean isSingle) {
		this.single = isSingle;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_ENABLE_PROXY_DESC", toolTipBundleKey = "JSIDPLAY2_ENABLE_PROXY_TOOLTIP")
	private boolean enableProxy;

	@Override
	public boolean isEnableProxy() {
		return enableProxy;
	}

	@Override
	public void setEnableProxy(boolean isEnableProxy) {
		this.enableProxy = isEnableProxy;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_PROXY_HOSTNAME_DESC", toolTipBundleKey = "JSIDPLAY2_PROXY_HOSTNAME_TOOLTIP")
	private String proxyHostname;

	@Override
	public String getProxyHostname() {
		return proxyHostname;
	}

	@Override
	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_PROXY_PORT_DESC", toolTipBundleKey = "JSIDPLAY2_PROXY_PORT_TOOLTIP")
	private int proxyPort = 80;

	@Override
	public int getProxyPort() {
		return proxyPort;
	}

	@Override
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.DIRECTORIES_ONLY)
	@ConfigDescription(bundleKey = "JSIDPLAY2_LAST_DIRECTORY_DESC", toolTipBundleKey = "JSIDPLAY2_LAST_DIRECTORY_TOOLTIP")
	private String lastDirectory;

	@Override
	public String getLastDirectory() {
		return lastDirectory;
	}

	@Override
	public void setLastDirectory(String lastDirectory) {
		this.lastDirectory = lastDirectory;
	}

	@ConfigFieldType(uiClass = File.class, filter = JFileChooser.DIRECTORIES_ONLY)
	@ConfigDescription(bundleKey = "JSIDPLAY2_TMP_DIR_DESC", toolTipBundleKey = "JSIDPLAY2_TMP_DIR_TOOLTIP")
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

	@ConfigDescription(bundleKey = "JSIDPLAY2_FRAME_X_DESC", toolTipBundleKey = "JSIDPLAY2_FRAME_X_TOOLTIP")
	private int frameX;

	@Override
	public int getFrameX() {
		return frameX;
	}

	@Override
	public void setFrameX(int frameX) {
		this.frameX = frameX;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_FRAME_Y_DESC", toolTipBundleKey = "JSIDPLAY2_FRAME_Y_TOOLTIP")
	private int frameY;

	@Override
	public int getFrameY() {
		return frameY;
	}

	@Override
	public void setFrameY(int frameY) {
		this.frameY = frameY;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_FRAME_WIDTH_DESC", toolTipBundleKey = "JSIDPLAY2_FRAME_WIDTH_TOOLTIP")
	private int frameWidth = 1024;

	@Override
	public int getFrameWidth() {
		return frameWidth;
	}

	@Override
	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	@ConfigDescription(bundleKey = "JSIDPLAY2_FRAME_HEIGHT_DESC", toolTipBundleKey = "JSIDPLAY2_FRAME_HEIGHT_TOOLTIP")
	private int frameHeight = 768;

	@Override
	public int getFrameHeight() {
		return frameHeight;
	}

	@Override
	public void setFrameHeight(int frameHeight) {
		this.frameHeight = frameHeight;
	}

}
