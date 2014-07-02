package sidplay.ini;

import libsidplay.common.CPUClock;
import resid_builder.resid.ChipModel;
import sidplay.ini.intf.IEmulationSection;

/**
 * Emulation section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniEmulationSection extends IniSection implements IEmulationSection {
	protected IniEmulationSection(IniReader iniReader) {
		super(iniReader);
	}
		
	/**
	 * Getter of the default clock speed.
	 * 
	 * @return the default clock speed
	 */
	@Override
	public final CPUClock getDefaultClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "DefaultClockSpeed", CPUClock.PAL);
	}

	/**
	 * Setter of the default clock speed.
	 * 
	 * @param speed
	 *            default clock speed
	 */
	@Override
	public final void setDefaultClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "DefaultClockSpeed", speed);
	}

	/**
	 * Getter of user the clock speed.
	 * 
	 * @return the user clock speed
	 */
	@Override
	public final CPUClock getUserClockSpeed() {
		return iniReader.getPropertyEnum("Emulation", "UserClockSpeed", null, CPUClock.class);
	}

	/**
	 * Setter of the user clock speed.
	 * 
	 * @param speed
	 *            user clock speed
	 */
	@Override
	public final void setUserClockSpeed(final CPUClock speed) {
		iniReader.setProperty("Emulation", "UserClockSpeed", speed);
	}

	/**
	 * Getter of the default SID model.
	 * 
	 * @return the default SID model
	 */
	@Override
	public final ChipModel getDefaultSidModel() {
		return iniReader.getPropertyEnum("Emulation", "DefaultSidModel", ChipModel.MOS6581);
	}

	/**
	 * Setter of the default SID model.
	 * 
	 * @param model the default SID model
	 */
	@Override
	public final void setDefaultSidModel(ChipModel model) {
		iniReader.setProperty("Emulation", "DefaultSidModel", model);
	}

	/**
	 * Getter of the user SID model.
	 * 
	 * @return the user SID model
	 */
	@Override
	public final ChipModel getUserSidModel() {
		return iniReader.getPropertyEnum("Emulation", "UserSidModel", null, ChipModel.class);
	}

	/**
	 * Setter of the user SID model.
	 * 
	 * @param model
	 *            user SID model
	 */
	@Override
	public final void setUserSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "UserSidModel", model);
	}

	/**
	 * Getter of the chip to be used for MOS6581.
	 * 
	 * @return the chip to be used for MOS6581
	 */
	@Override
	public final int getHardsid6581() {
		return iniReader.getPropertyInt("Emulation", "HardSID6581", -1);
	}

	/**
	 * Setter of the chip to be used for MOS6581.
	 * 
	 * @param chip
	 *            the chip to be used for MOS6581
	 */
	@Override
	public final void setHardsid6581(final int chip) {
		iniReader.setProperty("Emulation", "HardSID6581", chip);
	}

	/**
	 * Getter of the chip to be used for CSG8580.
	 * 
	 * @return the chip to be used for CSG8580
	 */
	@Override
	public final int getHardsid8580() {
		return iniReader.getPropertyInt("Emulation", "HardSID8580", -1);
	}

	/**
	 * Setter of the chip to be used for CSG8580.
	 * 
	 * @param chip
	 *            the chip to be used for CSG8580
	 */
	@Override
	public final void setHardsid8580(final int chip) {
		iniReader.setProperty("Emulation", "HardSID8580", chip);
	}

	/**
	 * Is SID filter enabled?
	 * 
	 * @return filter enabled
	 */
	@Override
	public final boolean isFilter() {
		return iniReader.getPropertyBool("Emulation", "UseFilter", true);
	}

	/**
	 * Setter of the filter enable.
	 * 
	 * @param enable
	 *            the filter enable
	 */
	@Override
	public final void setFilter(final boolean enable) {
		iniReader.setProperty("Emulation", "UseFilter", enable);
	}

	/**
	 * Getter of the filter setting of MOS6581.
	 * 
	 * @return the filter setting of MOS6581
	 */
	@Override
	public final String getFilter6581() {
		return iniReader.getPropertyString("Emulation", "Filter6581", null);
	}

	/**
	 * Setter of the filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581
	 */
	@Override
	public final void setFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "Filter6581", filterName);
	}

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	@Override
	public final String getFilter8580() {
		return iniReader.getPropertyString("Emulation", "Filter8580", null);
	}

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	@Override
	public final void setFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "Filter8580", filterName);
	}

	/**
	 * Getter of the filter setting of MOS6581.
	 * 
	 * @return the filter setting of MOS6581
	 */
	@Override
	public final String getReSIDfpFilter6581() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter6581", null);
	}

	/**
	 * Setter of the filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581
	 */
	@Override
	public final void setReSIDfpFilter6581(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter6581", filterName);
	}

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	@Override
	public final String getReSIDfpFilter8580() {
		return iniReader.getPropertyString("Emulation", "ReSIDfp_Filter8580", null);
	}

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	@Override
	public final void setReSIDfpFilter8580(final String filterName) {
		iniReader.setProperty("Emulation", "ReSIDfp_Filter8580", filterName);
	}

	/**
	 * Getter of the enable SID digi-boost.
	 * 
	 * @return the enable SID digi-boost
	 */
	@Override
	public final boolean isDigiBoosted8580() {
		return iniReader.getPropertyBool("Emulation", "DigiBoosted8580", false);
	}

	/**
	 * setter of the enable SID digi-boost.
	 * 
	 * @param boost
	 *            the enable SID digi-boost
	 */
	@Override
	public final void setDigiBoosted8580(final boolean boost) {
		iniReader.setProperty("Emulation", "DigiBoosted8580", boost);
	}

	/**
	 * Getter of the stereo SID base address.
	 * 
	 * @return the stereo SID base address
	 */
	@Override
	public final int getDualSidBase() {
		return iniReader.getPropertyInt("Emulation", "dualSidBase", 0xd420);
	}

	/**
	 * Setter of the stereo SID base address.
	 * 
	 * @param base
	 *            stereo SID base address
	 */
	@Override
	public final void setDualSidBase(final int base) {
		iniReader.setProperty("Emulation", "dualSidBase", String.format("0x%04x", base));
	}

	/**
	 * Getter of the forced playback stereo mode.
	 * 
	 * @return the forced playback stereo mode
	 */
	@Override
	public final boolean isForceStereoTune() {
		return iniReader.getPropertyBool("Emulation", "forceStereoTune", false);
	}

	/**
	 * Setter of the forced playback stereo mode.
	 * 
	 * @param force
	 *            forced playback stereo mode
	 */
	@Override
	public final void setForceStereoTune(final boolean force) {
		iniReader.setProperty("Emulation", "forceStereoTune", force);
	}

	/**
	 * Getter of the the stereo SID model.
	 * 
	 * @return the stereo SID model
	 */
	@Override
	public final ChipModel getStereoSidModel() {
		if (iniReader.getPropertyString("Emulation", "StereoSidModel", null) == null) {
			// Special case: property not set? return null
			return null;
		}
		return iniReader.getPropertyEnum("Emulation", "StereoSidModel", ChipModel.MOS6581);
	}

	/**
	 * Setter of the the stereo SID model.
	 * 
	 * @param model
	 *            the the stereo SID model
	 */
	@Override
	public final void setStereoSidModel(final ChipModel model) {
		iniReader.setProperty("Emulation", "StereoSidModel", model);
	}

}