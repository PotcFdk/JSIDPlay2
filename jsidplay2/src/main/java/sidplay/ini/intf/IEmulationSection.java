package sidplay.ini.intf;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;

public interface IEmulationSection {
	public static final Emulation DEFAULT_EMULATION = Emulation.RESID;
	public static final CPUClock DEFAULT_CLOCK_SPEED = CPUClock.PAL;
	public static final ChipModel DEFAULT_SID_MODEL = ChipModel.MOS6581;
	public static final int DEFAULT_HARD_SID_6581 = 1;
	public static final int DEFAULT_HARD_SID_8580 = 2;
	public static final boolean DEFAULT_USE_FILTER = true;
	public static final boolean DEFAULT_USE_STEREO_FILTER = true;
	public static final boolean DEFAULT_USE_3SID_FILTER = true;
	public static final int DEFAULT_SID_NUM_TO_READ = 0;
	public static final boolean DEFAULT_DIGI_BOOSTED_8580 = false;
	public static final int DEFAULT_DUAL_SID_BASE = 0xd420;
	public static final int DEFAULT_THIRD_SID_BASE = 0xd440;
	public static final boolean DEFAULT_FORCE_STEREO_TUNE = false;
	public static final boolean DEFAULT_FORCE_3SID_TUNE = false;
	public static final ChipModel DEFAULT_STEREO_SID_MODEL = ChipModel.MOS6581;
	public static final ChipModel DEFAULT_3RD_SID_MODEL = ChipModel.MOS6581;


	public static final String DEFAULT_FILTER_6581 = "FilterAverage6581";
	public static final String DEFAULT_STEREO_FILTER_6581 = "FilterAverage6581";
	public static final String DEFAULT_3SID_FILTER_6581 = "FilterAverage6581";

	public static final String DEFAULT_FILTER_8580 = "FilterAverage8580";
	public static final String DEFAULT_STEREO_FILTER_8580 = "FilterAverage8580";
	public static final String DEFAULT_3SID_FILTER_8580 = "FilterAverage8580";

	public static final String DEFAULT_ReSIDfp_FILTER_6581 = "FilterAlankila6581R4AR_3789";
	public static final String DEFAULT_ReSIDfp_STEREO_FILTER_6581 = "FilterAlankila6581R4AR_3789";
	public static final String DEFAULT_ReSIDfp_3SID_FILTER_6581 = "FilterAlankila6581R4AR_3789";

	public static final String DEFAULT_ReSIDfp_FILTER_8580 = "FilterTrurl8580R5_3691";
	public static final String DEFAULT_ReSIDfp_STEREO_FILTER_8580 = "FilterTrurl8580R5_3691";
	public static final String DEFAULT_ReSIDfp_3SID_FILTER_8580 = "FilterTrurl8580R5_3691";

	/**
	 * Getter of the emulation to be used.
	 * 
	 * @return the emulation to be used
	 */
	public Emulation getEmulation();

	/**
	 * Setter of the emulation to be used.
	 * 
	 * @param emulation
	 *            emulation to be used
	 */
	public void setEmulation(Emulation emulation);

	/**
	 * Getter of the default clock speed.
	 * 
	 * @return the default clock speed
	 */
	public CPUClock getDefaultClockSpeed();

	/**
	 * Setter of the default clock speed.
	 * 
	 * @param speed
	 *            default clock speed
	 */
	public void setDefaultClockSpeed(CPUClock speed);

	/**
	 * Getter of user the clock speed.
	 * 
	 * @return the user clock speed
	 */
	public CPUClock getUserClockSpeed();

	/**
	 * Setter of the user clock speed.
	 * 
	 * @param speed
	 *            user clock speed
	 */
	public void setUserClockSpeed(CPUClock speed);

	/**
	 * Getter of the default SID model.
	 * 
	 * @return the default SID model
	 */
	public ChipModel getDefaultSidModel();

	/**
	 * Setter of the default SID model.
	 * 
	 * @param model
	 *            the default SID model
	 */
	public void setDefaultSidModel(ChipModel model);

	/**
	 * Getter of the user SID model.
	 * 
	 * @return the user SID model
	 */
	public ChipModel getUserSidModel();

	/**
	 * Setter of the user SID model.
	 * 
	 * @param model
	 *            user SID model
	 */
	public void setUserSidModel(ChipModel model);

	/**
	 * Getter of the chip to be used for MOS6581.
	 * 
	 * @return the chip to be used for MOS6581
	 */
	public int getHardsid6581();

	/**
	 * Setter of the chip to be used for MOS6581.
	 * 
	 * @param chip
	 *            the chip to be used for MOS6581
	 */
	public void setHardsid6581(int chip);

	/**
	 * Getter of the chip to be used for CSG8580.
	 * 
	 * @return the chip to be used for CSG8580
	 */
	public int getHardsid8580();

	/**
	 * Setter of the chip to be used for CSG8580.
	 * 
	 * @param chip
	 *            the chip to be used for CSG8580
	 */
	public void setHardsid8580(int chip);

	/**
	 * Is SID filter enabled?
	 * 
	 * @return filter enabled
	 */
	public boolean isFilter();

	public boolean isStereoFilter();

	public boolean isThirdSIDFilter();

	/**
	 * Setter of the filter enable.
	 * 
	 * @param enable
	 *            the filter enable
	 */
	public void setFilter(boolean enable);

	public void setStereoFilter(boolean enable);

	public void setThirdSIDFilter(boolean enable);

	/**
	 * Getter of the filter setting of MOS6581.
	 * 
	 * @return the filter setting of MOS6581
	 */
	public String getFilter6581();

	public String getStereoFilter6581();

	public String getThirdSIDFilter6581();

	/**
	 * Setter of the filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581
	 */
	public void setFilter6581(String filterName);

	public void setStereoFilter6581(String filterName);

	public void setThirdSIDFilter6581(String filterName);

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	public String getFilter8580();

	public String getStereoFilter8580();

	public String getThirdSIDFilter8580();

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	public void setFilter8580(String filterName);

	public void setStereoFilter8580(String filterName);

	public void setThirdSIDFilter8580(String filterName);

	/**
	 * Getter of the filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @return the filter setting of MOS6581 for ReSIDfp
	 */
	public String getReSIDfpFilter6581();

	public String getReSIDfpStereoFilter6581();

	public String getReSIDfp3rdSIDFilter6581();

	/**
	 * Setter of the filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581 for ReSIDfp
	 */
	public void setReSIDfpFilter6581(String filterName);

	public void setReSIDfpStereoFilter6581(String filterName);

	public void setReSIDfp3rdSIDFilter6581(String filterName);

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	public String getReSIDfpFilter8580();

	public String getReSIDfpStereoFilter8580();

	public String getReSIDfp3rdSIDFilter8580();

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	public void setReSIDfpFilter8580(String filterName);

	public void setReSIDfpStereoFilter8580(String filterName);

	public void setReSIDfp3rdSIDFilter8580(String filterName);

	/**
	 * Getter of the enable SID digi-boost.
	 * 
	 * @return the enable SID digi-boost
	 */
	public boolean isDigiBoosted8580();

	/**
	 * setter of the enable SID digi-boost.
	 * 
	 * @param boost
	 *            the enable SID digi-boost
	 */
	public void setDigiBoosted8580(boolean boost);

	/**
	 * Getter of the stereo SID base address.
	 * 
	 * @return the stereo SID base address
	 */
	public int getDualSidBase();

	public int getThirdSIDBase();

	/**
	 * Setter of the stereo SID base address.
	 * 
	 * @param base
	 *            stereo SID base address
	 */
	public void setDualSidBase(int base);

	public void setThirdSIDBase(int base);

	/**
	 * @return SID chip to read from (FakeStereo)
	 */
	public int getSidNumToRead();

	/**
	 * Setter of the SID chip to read from (FakeStereo).
	 * 
	 * @param sidNumToRead
	 *            SID chip to read from (FakeStereo)
	 */
	public void setSidNumToRead(int sidNumToRead);

	/**
	 * Getter of the forced playback stereo mode.
	 * 
	 * @return the forced playback stereo mode
	 */
	public boolean isForceStereoTune();

	public boolean isForce3SIDTune();

	/**
	 * Setter of the forced playback stereo mode.
	 * 
	 * @param force
	 *            forced playback stereo mode
	 */
	public void setForceStereoTune(boolean force);

	public void setForce3SIDTune(boolean force);

	/**
	 * Getter of the the stereo SID model.
	 * 
	 * @return the stereo SID model
	 */
	public ChipModel getStereoSidModel();

	/**
	 * Setter of the the stereo SID model.
	 * 
	 * @param model
	 *            the the stereo SID model
	 */
	public void setStereoSidModel(ChipModel model);

	/**
	 * Getter of the the stereo SID model.
	 * 
	 * @return the stereo SID model
	 */
	public ChipModel getThirdSIDModel();

	/**
	 * Setter of the the stereo SID model.
	 * 
	 * @param model
	 *            the the stereo SID model
	 */
	public void setThirdSIDModel(ChipModel model);

	public Emulation getUserEmulation();
	
	public void setUserEmulation(Emulation emulation);
	
	public Emulation getStereoEmulation();
	
	public void setStereoEmulation(Emulation emulation);
	
	public Emulation getThirdEmulation();
	
	public void setThirdEmulation(Emulation emulation);
	
}