package applet.entities.config;

import java.io.File;

import javax.persistence.Embeddable;
import javax.swing.JFileChooser;

import sidplay.ini.intf.ISidPlay2Section;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigField;
import applet.config.annotations.ConfigTransient;

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

	@ConfigDescription(descriptionKey = "JSIDPLAY2_ENABLE_DATABASE_DESC", toolTipKey = "JSIDPLAY2_ENABLE_DATABASE_TOOLTIP")
	private boolean enableDatabase = true;

	@Override
	public boolean isEnableDatabase() {
		return enableDatabase;
	}

	@Override
	public void setEnableDatabase(boolean isEnableDatabase) {
		this.enableDatabase = isEnableDatabase;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_PLAY_LENGTH_DESC", toolTipKey = "JSIDPLAY2_PLAY_LENGTH_TOOLTIP")
	private int playLength;

	@Override
	public int getPlayLength() {
		return playLength;
	}

	@Override
	public void setPlayLength(int playLength) {
		this.playLength = playLength;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_RECORD_LENGTH_DESC", toolTipKey = "JSIDPLAY2_RECORD_LENGTH_TOOLTIP")
	private int recordLength;

	@Override
	public int getRecordLength() {
		return recordLength;
	}

	@Override
	public void setRecordLength(int recordLength) {
		this.recordLength = recordLength;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_HVMEC_DESC", toolTipKey = "JSIDPLAY2_HVMEC_TOOLTIP")
	private String HVMEC;

	@Override
	public String getHVMEC() {
		return HVMEC;
	}

	@Override
	public void setHVMEC(String hVMEC) {
		HVMEC = hVMEC;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_DEMOS_DESC", toolTipKey = "JSIDPLAY2_DEMOS_TOOLTIP")
	private String demos;

	@Override
	public String getDemos() {
		return demos;
	}

	@Override
	public void setDemos(String demos) {
		this.demos = demos;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_MAGS_DESC", toolTipKey = "JSIDPLAY2_MAGS_TOOLTIP")
	private String mags;

	@Override
	public String getMags() {
		return mags;
	}

	@Override
	public void setMags(String mags) {
		this.mags = mags;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_CGSC_DESC", toolTipKey = "JSIDPLAY2_CGSC_TOOLTIP")
	private String cgsc;

	@Override
	public String getCgsc() {
		return cgsc;
	}

	@Override
	public void setCgsc(String cgsc) {
		this.cgsc = cgsc;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.FILES_AND_DIRECTORIES)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_HVSC_DESC", toolTipKey = "JSIDPLAY2_HVSC_TOOLTIP")
	private String hvsc;

	@Override
	public String getHvsc() {
		return hvsc;
	}

	@Override
	public void setHvsc(String hvsc) {
		this.hvsc = hvsc;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_SINGLE_DESC", toolTipKey = "JSIDPLAY2_SINGLE_TOOLTIP")
	private boolean single;

	@Override
	public boolean isSingle() {
		return single;
	}

	@Override
	public void setSingle(boolean isSingle) {
		this.single = isSingle;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_ENABLE_PROXY_DESC", toolTipKey = "JSIDPLAY2_ENABLE_PROXY_TOOLTIP")
	private boolean enableProxy;

	@Override
	public boolean isEnableProxy() {
		return enableProxy;
	}

	@Override
	public void setEnableProxy(boolean isEnableProxy) {
		this.enableProxy = isEnableProxy;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_PROXY_HOSTNAME_DESC", toolTipKey = "JSIDPLAY2_PROXY_HOSTNAME_TOOLTIP")
	private String proxyHostname;

	@Override
	public String getProxyHostname() {
		return proxyHostname;
	}

	@Override
	public void setProxyHostname(String proxyHostname) {
		this.proxyHostname = proxyHostname;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_PROXY_PORT_DESC", toolTipKey = "JSIDPLAY2_PROXY_PORT_TOOLTIP")
	private int proxyPort = 80;

	@Override
	public int getProxyPort() {
		return proxyPort;
	}

	@Override
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.DIRECTORIES_ONLY)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_LAST_DIRECTORY_DESC", toolTipKey = "JSIDPLAY2_LAST_DIRECTORY_TOOLTIP")
	private String lastDirectory;

	@Override
	public String getLastDirectory() {
		return lastDirectory;
	}

	@Override
	public void setLastDirectory(String lastDirectory) {
		this.lastDirectory = lastDirectory;
	}

	@ConfigField(uiClass = File.class, filter = JFileChooser.DIRECTORIES_ONLY)
	@ConfigDescription(descriptionKey = "JSIDPLAY2_TMP_DIR_DESC", toolTipKey = "JSIDPLAY2_TMP_DIR_TOOLTIP")
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

	@ConfigDescription(descriptionKey = "JSIDPLAY2_FRAME_X_DESC", toolTipKey = "JSIDPLAY2_FRAME_X_TOOLTIP")
	private int frameX;

	@Override
	public int getFrameX() {
		return frameX;
	}

	@Override
	public void setFrameX(int frameX) {
		this.frameX = frameX;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_FRAME_Y_DESC", toolTipKey = "JSIDPLAY2_FRAME_Y_TOOLTIP")
	private int frameY;

	@Override
	public int getFrameY() {
		return frameY;
	}

	@Override
	public void setFrameY(int frameY) {
		this.frameY = frameY;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_FRAME_WIDTH_DESC", toolTipKey = "JSIDPLAY2_FRAME_WIDTH_TOOLTIP")
	private int frameWidth = 1024;

	@Override
	public int getFrameWidth() {
		return frameWidth;
	}

	@Override
	public void setFrameWidth(int frameWidth) {
		this.frameWidth = frameWidth;
	}

	@ConfigDescription(descriptionKey = "JSIDPLAY2_FRAME_HEIGHT_DESC", toolTipKey = "JSIDPLAY2_FRAME_HEIGHT_TOOLTIP")
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
