package sidplay.ini;

import libsidplay.components.c1541.C1541.FloppyType;
import libsidplay.components.c1541.ExtendImagePolicy;

/**
 * C1541 section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniC1541Section extends IniSection {

	protected IniC1541Section(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Drive turned on?
	 * 
	 * @return drive turned on?
	 */
	public final boolean isDriveOn() {
		return iniReader.getPropertyBool("C1541", "DriveOn", false);
	}

	/**
	 * Turn drive on.
	 * 
	 * @param on
	 *            drive turned on?
	 */
	public final void setDriveOn(final boolean on) {
		iniReader.setProperty("C1541", "DriveOn", on);
	}

	/**
	 * Drive sound turned on?
	 * 
	 * @return drive sound turned on?
	 */
	public final boolean isDriveSoundOn() {
		return iniReader.getPropertyBool("C1541", "DriveSound", false);
	}

	/**
	 * Turn drive sound on.
	 * 
	 * @param driveSound
	 *            drive sound turned on?
	 */
	public final void setDriveSoundOn(final boolean on) {
		iniReader.setProperty("C1541", "DriveSound", on);
	}

	/**
	 * Is the parallel cable plugged in?
	 * 
	 * @return parallel cable plugged in?
	 */
	public final boolean isParallelCable() {
		return iniReader.getPropertyBool("C1541", "ParallelCable", false);
	}

	/**
	 * Plug in parallel cable.
	 * 
	 * @param cable
	 *            parallel cable plugged in?
	 */
	public final void setParallelCable(final boolean on) {
		iniReader.setProperty("C1541", "ParallelCable", on);
	}

	/**
	 * Is RAM expansion at some particular slot enabled
	 * 
	 * @param selector
	 *            which 8KB RAM bank to expand (0-5), starting at 0x2000
	 *            increasing in 8KB steps up to 0xA000.
	 * @return state
	 * 			  enabled?
	 */
	public final boolean isRamExpansionEnabled(final int selector) {
		return iniReader.getPropertyBool("C1541", String.format("RamExpand0x%X", 8192 * (selector + 1)), false);
	}

	/**
	 * Enable 8K Ram expansion.
	 * 
	 * @param selector
	 *            which 8KB RAM bank to expand (0-5), starting at 0x2000
	 *            increasing in 8KB steps up to 0xA000.
	 * @param state
	 *            enable 8K Ram expansion
	 */
	public final void setRamExpansion(final int selector, final boolean on) {
		iniReader.setProperty("C1541", String.format("RamExpand0x%X", 8192 * (selector + 1)), on);
	}

	/**
	 * Set 40 tracks disk image extension policy.
	 * 
	 * @param policy
	 *            policy to extend disk image
	 */
	public final void setExtendImagePolicy(final ExtendImagePolicy policy) {
		iniReader.setProperty("C1541", "DiskExtendPolicy", policy);
	}

	/**
	 * Get 40 tracks disk image extension policy.
	 * 
	 * @return disk image extension policy
	 */
	public final ExtendImagePolicy getExtendImagePolicy() {
		return iniReader.getPropertyEnum("C1541", "DiskExtendPolicy", ExtendImagePolicy.EXTEND_ACCESS);
	}

	/**
	 * Set type of floppy.
	 * 
	 * @param floppyType
	 *            type of floppy
	 */
	public final void setFloppyType(final FloppyType floppyType) {
		iniReader.setProperty("C1541", "FloppyType", floppyType);
	}

	/**
	 * Get type of floppy.
	 * 
	 * @return type of floppy
	 */
	public final FloppyType getFloppyType() {
		return iniReader.getPropertyEnum("C1541", "FloppyType", FloppyType.C1541);
	}
}
