package sidplay.ini.intf;

import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.C1541.FloppyType;

public interface IC1541Section {

	/**
	 * Drive turned on?
	 * 
	 * @return drive turned on?
	 */
	public boolean isDriveOn();

	/**
	 * Turn drive on.
	 * 
	 * @param on
	 *            drive turned on?
	 */
	public void setDriveOn(boolean on);

	/**
	 * Drive sound turned on?
	 * 
	 * @return drive sound turned on?
	 */
	public boolean isDriveSoundOn();

	/**
	 * Turn drive sound on.
	 * 
	 * @param on
	 *            drive sound turned on?
	 */
	public void setDriveSoundOn(boolean on);

	/**
	 * Is the parallel cable plugged in?
	 * 
	 * @return parallel cable plugged in?
	 */
	public boolean isParallelCable();

	/**
	 * Plug in parallel cable.
	 * 
	 * @param on
	 *            parallel cable plugged in?
	 */
	public void setParallelCable(boolean on);

	/**
	 * Is RAM expansion at some particular slot enabled
	 *
	 * @return state
	 * 			  enabled?
	 */
	public boolean isRamExpansionEnabled0();
	public boolean isRamExpansionEnabled1();
	public boolean isRamExpansionEnabled2();
	public boolean isRamExpansionEnabled3();
	public boolean isRamExpansionEnabled4();

	/**
	 * Enable 8K Ram expansion.
	 *
	 * @param on
	 *            enable 8K Ram expansion
	 */
	public void setRamExpansionEnabled0(boolean on);
	public void setRamExpansionEnabled1(boolean on);
	public void setRamExpansionEnabled2(boolean on);
	public void setRamExpansionEnabled3(boolean on);
	public void setRamExpansionEnabled4(boolean on);

	/**
	 * Set 40 tracks disk image extension policy.
	 * 
	 * @param policy
	 *            policy to extend disk image
	 */
	public void setExtendImagePolicy(ExtendImagePolicy policy);

	/**
	 * Get 40 tracks disk image extension policy.
	 * 
	 * @return disk image extension policy
	 */
	public ExtendImagePolicy getExtendImagePolicy();

	/**
	 * Set type of floppy.
	 * 
	 * @param floppyType
	 *            type of floppy
	 */
	public void setFloppyType(FloppyType floppyType);

	/**
	 * Get type of floppy.
	 * 
	 * @return type of floppy
	 */
	public FloppyType getFloppyType();

}