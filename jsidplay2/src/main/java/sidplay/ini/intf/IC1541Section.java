package sidplay.ini.intf;

import libsidplay.components.c1541.C1541.FloppyType;

public interface IC1541Section {

	public static final boolean DEFAULT_DRIVE_ON = false;
	public static final boolean DEFAULT_PARALLEL_CABLE = false;
	public static final boolean DEFAULT_RAM_EXPAND_0X2000 = false;
	public static final boolean DEFAULT_RAM_EXPAND_0X4000 = false;
	public static final boolean DEFAULT_RAM_EXPAND_0X6000 = false;
	public static final boolean DEFAULT_RAM_EXPAND_0X8000 = false;
	public static final boolean DEFAULT_RAM_EXPAND_0XA000 = false;
	public static final FloppyType DEFAULT_FLOPPY_TYPE = FloppyType.C1541;

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
	 * @return state enabled?
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