package sidplay.ini;

import sidplay.ini.intf.IC1541Section;
import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;

/**
 * C1541 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniC1541Section extends IniSection implements IC1541Section {

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
		return iniReader.getPropertyBool("C1541", "DriveOn", false);
	}

	/**
	 * Turn drive on.
	 * 
	 * @param on
	 *            drive turned on?
	 */
	@Override
	public final void setDriveOn(final boolean on) {
		iniReader.setProperty("C1541", "DriveOn", on);
	}

	/**
	 * Drive sound turned on?
	 * 
	 * @return drive sound turned on?
	 */
	@Override
	public final boolean isDriveSoundOn() {
		return iniReader.getPropertyBool("C1541", "DriveSound", false);
	}

	/**
	 * Turn drive sound on.
	 * 
	 * @param on
	 *            drive sound turned on?
	 */
	@Override
	public final void setDriveSoundOn(final boolean on) {
		iniReader.setProperty("C1541", "DriveSound", on);
	}

	/**
	 * Is the parallel cable plugged in?
	 * 
	 * @return parallel cable plugged in?
	 */
	@Override
	public final boolean isParallelCable() {
		return iniReader.getPropertyBool("C1541", "ParallelCable", false);
	}

	/**
	 * Plug in parallel cable.
	 * 
	 * @param on
	 *            parallel cable plugged in?
	 */
	@Override
	public final void setParallelCable(final boolean on) {
		iniReader.setProperty("C1541", "ParallelCable", on);
	}

	/**
	 * Is RAM expansion at some particular slot enabled
	 *
	 * @return state
	 * 			  enabled?
	 */
	@Override
	public final boolean isRamExpansionEnabled0() {
		return iniReader.getPropertyBool("C1541", "RamExpand0x2000", false);
	}
	@Override
	public final boolean isRamExpansionEnabled1() {
		return iniReader.getPropertyBool("C1541", "RamExpand0x4000", false);
	}
	@Override
	public final boolean isRamExpansionEnabled2() {
		return iniReader.getPropertyBool("C1541", "RamExpand0x6000", false);
	}
	@Override
	public final boolean isRamExpansionEnabled3() {
		return iniReader.getPropertyBool("C1541", "RamExpand0x8000", false);
	}
	@Override
	public final boolean isRamExpansionEnabled4() {
		return iniReader.getPropertyBool("C1541", "RamExpand0xA000", false);
	}

	/**
	 * Enable 8K Ram expansion.
	 *
	 * @param on
	 *            enable 8K Ram expansion
	 */
	@Override
	public final void setRamExpansion0(final boolean on) {
		iniReader.setProperty("C1541", "RamExpand0x2000", on);
	}
	@Override
	public final void setRamExpansion1(final boolean on) {
		iniReader.setProperty("C1541", "RamExpand0x4000", on);
	}
	@Override
	public final void setRamExpansion2(final boolean on) {
		iniReader.setProperty("C1541", "RamExpand0x6000", on);
	}
	@Override
	public final void setRamExpansion3(final boolean on) {
		iniReader.setProperty("C1541", "RamExpand0x8000", on);
	}
	@Override
	public final void setRamExpansion4(final boolean on) {
		iniReader.setProperty("C1541", "RamExpand0xA000", on);
	}

	/**
	 * Set 40 tracks disk image extension policy.
	 * 
	 * @param policy
	 *            policy to extend disk image
	 */
	@Override
	public final void setExtendImagePolicy(final ExtendImagePolicy policy) {
		iniReader.setProperty("C1541", "DiskExtendPolicy", policy);
	}

	/**
	 * Get 40 tracks disk image extension policy.
	 * 
	 * @return disk image extension policy
	 */
	@Override
	public final ExtendImagePolicy getExtendImagePolicy() {
		return iniReader.getPropertyEnum("C1541", "DiskExtendPolicy", ExtendImagePolicy.EXTEND_ACCESS);
	}

	/**
	 * Set type of floppy.
	 * 
	 * @param floppyType
	 *            type of floppy
	 */
	@Override
	public final void setFloppyType(final FloppyType floppyType) {
		iniReader.setProperty("C1541", "FloppyType", floppyType);
	}

	/**
	 * Get type of floppy.
	 * 
	 * @return type of floppy
	 */
	@Override
	public final FloppyType getFloppyType() {
		return iniReader.getPropertyEnum("C1541", "FloppyType", FloppyType.C1541);
	}
}
