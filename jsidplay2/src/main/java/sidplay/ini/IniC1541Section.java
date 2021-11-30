package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_DRIVE_ON;
import static sidplay.ini.IniDefaults.DEFAULT_FLOPPY_TYPE;
import static sidplay.ini.IniDefaults.DEFAULT_JIFFYDOS_INSTALLED;
import static sidplay.ini.IniDefaults.DEFAULT_PARALLEL_CABLE;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X2000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X4000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X6000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0X8000;
import static sidplay.ini.IniDefaults.DEFAULT_RAM_EXPAND_0XA000;

import libsidplay.components.c1541.FloppyType;
import libsidplay.config.IC1541Section;
import sidplay.ini.converter.BeanToStringConverter;

/**
 * C1541 section of the INI file.
 *
 * @author Ken Händel
 *
 */
public class IniC1541Section extends IniSection implements IC1541Section {

	private static final String SECTION_ID = "C1541";

	protected IniC1541Section(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Drive turned on?
	 *
	 * @return drive turned on?
	 */
	@Override
	public final boolean isDriveOn() {
		return iniReader.getPropertyBool(SECTION_ID, "DriveOn", DEFAULT_DRIVE_ON);
	}

	/**
	 * Turn drive on.
	 *
	 * @param on drive turned on?
	 */
	@Override
	public final void setDriveOn(final boolean on) {
		iniReader.setProperty(SECTION_ID, "DriveOn", on);
	}

	/**
	 * Is the parallel cable plugged in?
	 *
	 * @return parallel cable plugged in?
	 */
	@Override
	public final boolean isParallelCable() {
		return iniReader.getPropertyBool(SECTION_ID, "ParallelCable", DEFAULT_PARALLEL_CABLE);
	}

	/**
	 * Plug in parallel cable.
	 *
	 * @param on parallel cable plugged in?
	 */
	@Override
	public final void setParallelCable(final boolean on) {
		iniReader.setProperty(SECTION_ID, "ParallelCable", on);
	}

	@Override
	public final boolean isJiffyDosInstalled() {
		return iniReader.getPropertyBool(SECTION_ID, "JiffyDos Installed", DEFAULT_JIFFYDOS_INSTALLED);
	}

	@Override
	public final void setJiffyDosInstalled(boolean on) {
		iniReader.setProperty(SECTION_ID, "JiffyDos Installed", on);
	}

	/**
	 * Is RAM expansion at some particular slot enabled
	 *
	 * @return state enabled?
	 */
	@Override
	public final boolean isRamExpansionEnabled0() {
		return iniReader.getPropertyBool(SECTION_ID, "RamExpand0x2000", DEFAULT_RAM_EXPAND_0X2000);
	}

	/**
	 * Enable 8K Ram expansion.
	 *
	 * @param on enable 8K Ram expansion
	 */
	@Override
	public final void setRamExpansionEnabled0(final boolean on) {
		iniReader.setProperty(SECTION_ID, "RamExpand0x2000", on);
	}

	@Override
	public final boolean isRamExpansionEnabled1() {
		return iniReader.getPropertyBool(SECTION_ID, "RamExpand0x4000", DEFAULT_RAM_EXPAND_0X4000);
	}

	@Override
	public final void setRamExpansionEnabled1(final boolean on) {
		iniReader.setProperty(SECTION_ID, "RamExpand0x4000", on);
	}

	@Override
	public final boolean isRamExpansionEnabled2() {
		return iniReader.getPropertyBool(SECTION_ID, "RamExpand0x6000", DEFAULT_RAM_EXPAND_0X6000);
	}

	@Override
	public final void setRamExpansionEnabled2(final boolean on) {
		iniReader.setProperty(SECTION_ID, "RamExpand0x6000", on);
	}

	@Override
	public final boolean isRamExpansionEnabled3() {
		return iniReader.getPropertyBool(SECTION_ID, "RamExpand0x8000", DEFAULT_RAM_EXPAND_0X8000);
	}

	@Override
	public final void setRamExpansionEnabled3(final boolean on) {
		iniReader.setProperty(SECTION_ID, "RamExpand0x8000", on);
	}

	@Override
	public final boolean isRamExpansionEnabled4() {
		return iniReader.getPropertyBool(SECTION_ID, "RamExpand0xA000", DEFAULT_RAM_EXPAND_0XA000);
	}

	@Override
	public final void setRamExpansionEnabled4(final boolean on) {
		iniReader.setProperty(SECTION_ID, "RamExpand0xA000", on);
	}

	/**
	 * Get type of floppy.
	 *
	 * @return type of floppy
	 */
	@Override
	public final FloppyType getFloppyType() {
		return iniReader.getPropertyEnum(SECTION_ID, "FloppyType", DEFAULT_FLOPPY_TYPE, FloppyType.class);
	}

	/**
	 * Set type of floppy.
	 *
	 * @param floppyType type of floppy
	 */
	@Override
	public final void setFloppyType(final FloppyType floppyType) {
		iniReader.setProperty(SECTION_ID, "FloppyType", floppyType);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}

}
