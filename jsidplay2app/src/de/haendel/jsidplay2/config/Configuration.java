package de.haendel.jsidplay2.config;

public class Configuration implements IConfiguration {

	private String hostname;

	public final String getHostname() {
		return hostname;
	}

	public final void setHostname(String hostname) {
		this.hostname = hostname;
	}

	private String port;

	public final String getPort() {
		return port;
	}

	public final void setPort(String port) {
		this.port = port;
	}

	private String username;

	public final String getUsername() {
		return username;
	}

	public final void setUsername(String username) {
		this.username = username;
	}

	private String password;

	public final String getPassword() {
		return password;
	}

	public final void setPassword(String password) {
		this.password = password;
	}

	private String emulation;

	@Override
	public String getDefaultEmulation() {
		return emulation;
	}

	@Override
	public void setDefaultEmulation(String emulation) {
		this.emulation = emulation;
	}

	private boolean enableDatabase;

	@Override
	public boolean isEnableDatabase() {
		return enableDatabase;
	}

	@Override
	public void setEnableDatabase(boolean enableDatabase) {
		this.enableDatabase = enableDatabase;
	}

	private String defaultLength;

	@Override
	public String getDefaultLength() {
		return defaultLength;
	}

	@Override
	public void setDefaultLength(String defaultLength) {
		this.defaultLength = defaultLength;
	}

	private String defaultModel;

	@Override
	public String getDefaultModel() {
		return defaultModel;
	}

	@Override
	public void setDefaultModel(String defaultModel) {
		this.defaultModel = defaultModel;
	}

	private boolean singleSong;

	@Override
	public boolean isSingleSong() {
		return singleSong;
	}

	@Override
	public void setSingleSong(boolean singleSong) {
		this.singleSong = singleSong;
	}

	private boolean loop;

	@Override
	public boolean isLoop() {
		return loop;
	}

	@Override
	public void setLoop(boolean loop) {
		this.loop = loop;

	}

	private String filter6581;

	@Override
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581 = filter6581;

	}

	private String filter8580;

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580 = filter8580;
	}

	private String reSIDfpFilter6581;

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581;
	}

	@Override
	public void setReSIDfpFilter6581(String residFpFilter6581) {
		this.reSIDfpFilter6581 = residFpFilter6581;
	}

	private String reSIDfpFilter8580;

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580;
	}

	@Override
	public void setReSIDfpFilter8580(String residFpFilter8580) {
		this.reSIDfpFilter8580 = residFpFilter8580;
	}

	private String stereoFilter6581;

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581;
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581 = filter6581;

	}

	private String stereoFilter8580;

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580;
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580 = filter8580;
	}

	private String reSIDfpStereoFilter6581;

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public void setReSIDfpStereoFilter6581(String residFpFilter6581) {
		this.reSIDfpStereoFilter6581 = residFpFilter6581;
	}

	private String reSIDfpStereoFilter8580;

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public void setReSIDfpStereoFilter8580(String residFpFilter8580) {
		this.reSIDfpStereoFilter8580 = residFpFilter8580;
	}

	private boolean digiBoosted8580;

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580;
	}

	@Override
	public void setDigiBoosted8580(boolean digiBoosted) {
		this.digiBoosted8580 = digiBoosted;
	}

	private String samplingMethod;

	@Override
	public String getSamplingMethod() {
		return samplingMethod;
	}

	@Override
	public void setSamplingMethod(String samplingMethod) {
		this.samplingMethod = samplingMethod;
	}

	private String frequency;

	@Override
	public String getFrequency() {
		return frequency;
	}

	@Override
	public void setFrequency(String frequency) {
		this.frequency = frequency;
	}

	private String bufferSize;

	@Override
	public String getBufferSize() {
		return bufferSize;
	}

	@Override
	public void setBufferSize(String bufferSize) {
		this.bufferSize = bufferSize;
	}

	private String cbr;

	@Override
	public String getCbr() {
		return cbr;
	}

	@Override
	public void setCbr(String cbr) {
		this.cbr = cbr;
	}

	private String vbr;

	@Override
	public String getVbr() {
		return vbr;
	}

	@Override
	public void setVbr(String vbr) {
		this.vbr = vbr;
	}

}
