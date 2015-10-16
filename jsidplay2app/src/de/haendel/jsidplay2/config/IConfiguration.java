package de.haendel.jsidplay2.config;

public interface IConfiguration {
	String PAR_BUFFER_SIZE = "bufferSize";
	String PAR_EMULATION = "emulation";
	String PAR_ENABLE_DATABASE = "enableDatabase";
	String PAR_DEFAULT_PLAY_LENGTH = "defaultPlayLength";
	String PAR_DEFAULT_MODEL = "defaultSidModel";
	String PAR_SINGLE_SONG = "single";
	String PAR_LOOP = "loop";
	String PAR_SAMPLING_METHOD = "samplingMethod";
	String PAR_FREQUENCY = "frequency";
	String PAR_FILTER_6581 = "filter6581";
	String PAR_STEREO_FILTER_6581 = "stereoFilter6581";
	String PAR_THIRD_FILTER_6581 = "thirdFilter6581";
	String PAR_FILTER_8580 = "filter8580";
	String PAR_STEREO_FILTER_8580 = "stereoFilter8580";
	String PAR_THIRD_FILTER_8580 = "thirdFilter8580";
	String PAR_RESIDFP_FILTER_6581 = "reSIDfpFilter6581";
	String PAR_RESIDFP_STEREO_FILTER_6581 = "reSIDfpStereoFilter6581";
	String PAR_RESIDFP_THIRD_FILTER_6581 = "reSIDfpThirdFilter6581";
	String PAR_RESIDFP_FILTER_8580 = "reSIDfpFilter8580";
	String PAR_RESIDFP_STEREO_FILTER_8580 = "reSIDfpStereoFilter8580";
	String PAR_RESIDFP_THIRD_FILTER_8580 = "reSIDfpThirdFilter8580";
	String PAR_DIGI_BOOSTED_8580 = "digiBoosted8580";
	String PAR_IS_VBR = "isVbr";
	String PAR_CBR = "cbr";
	String PAR_VBR = "vbr";

	String RESID = "RESID";
	String RESIDFP = "RESIDFP";

	String MOS6581 = "MOS6581";
	String MOS8580 = "MOS8580";

	String DECIMATE = "DECIMATE";
	String RESAMPLE = "RESAMPLE";

	String _44100 = "44100";
	String _48000 = "48000";
	String _96000 = "96000";

	String DEFAULT_BUFFER_SIZE = "2500";
	String DEFAULT_PLAY_LENGTH = "300";
	String DEFAULT_ENABLE_DATABASE = Boolean.TRUE.toString();
	String DEFAULT_SINGLE_SONG = Boolean.FALSE.toString();
	String DEFAULT_LOOP = Boolean.FALSE.toString();
	String DEFAULT_DIGI_BOOSTED_8580 = Boolean.FALSE.toString();
	String DEFAULT_FILTER_6581 = "FilterAverage6581";
	String DEFAULT_FILTER_8580 = "FilterAverage8580";
	String DEFAULT_RESIDFP_FILTER_6581 = "FilterAlankila6581R4AR_3789";
	String DEFAULT_RESIDFP_FILTER_8580 = "FilterTrurl8580R5_3691";
	String DEFAULT_CBR = "64";
	String DEFAULT_VBR = "0";

	String getHostname();

	void setHostname(String hostname);

	String getPort();

	void setPort(String port);

	String getUsername();

	void setUsername(String username);

	String getPassword();

	void setPassword(String password);

	String getBufferSize();

	void setBufferSize(String bufferSize);

	String getDefaultEmulation();

	void setDefaultEmulation(String emulation);

	boolean isEnableDatabase();

	void setEnableDatabase(boolean enableDatabase);

	String getDefaultLength();

	void setDefaultLength(String defaultLength);

	String getDefaultModel();

	void setDefaultModel(String defaultModel);

	boolean isSingleSong();

	void setSingleSong(boolean singleSong);

	boolean isLoop();

	void setLoop(boolean loop);

	String getFilter6581();

	void setFilter6581(String filter6581);

	String getFilter8580();

	void setFilter8580(String filter8580);

	String getReSIDfpFilter6581();

	void setReSIDfpFilter6581(String residFpFilter6581);

	String getReSIDfpFilter8580();

	void setReSIDfpFilter8580(String residFpFilter8580);

	String getStereoFilter6581();

	void setStereoFilter6581(String filter6581);

	String getStereoFilter8580();

	void setStereoFilter8580(String filter8580);

	String getReSIDfpStereoFilter6581();

	void setReSIDfpStereoFilter6581(String residFpFilter6581);

	String getReSIDfpStereoFilter8580();

	void setReSIDfpStereoFilter8580(String residFpFilter8580);



	String getThirdFilter6581();

	void setThirdFilter6581(String filter6581);

	String getThirdFilter8580();

	void setThirdFilter8580(String filter8580);

	String getReSIDfpThirdFilter6581();

	void setReSIDfpThirdFilter6581(String residFpFilter6581);

	String getReSIDfpThirdFilter8580();

	void setReSIDfpThirdFilter8580(String residFpFilter8580);


	
	boolean isDigiBoosted8580();

	void setDigiBoosted8580(boolean digiBoosted);

	String getSamplingMethod();

	void setSamplingMethod(String samplingMethod);

	String getFrequency();

	void setFrequency(String frequency);

	String getCbr();
	
	void setCbr(String newValue);

	String getVbr();
	
	void setVbr(String newValue);

}
