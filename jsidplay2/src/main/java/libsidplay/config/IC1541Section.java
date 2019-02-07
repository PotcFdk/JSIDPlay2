package libsidplay.config;

import libsidplay.components.c1541.FloppyType;

public interface IC1541Section {

	/**
	 * Drive turned on?
	 * 
	 * @return drive turned on?
	 */
	boolean isDriveOn();

	/**
	 * Turn drive on.
	 * 
	 * @param on drive turned on?
	 */
	void setDriveOn(boolean on);

	/**
	 * Is the parallel cable plugged in?
	 * 
	 * @return parallel cable plugged in?
	 */
	boolean isParallelCable();

	/**
	 * Plug in parallel cable.
	 * 
	 * @param on parallel cable plugged in?
	 */
	void setParallelCable(boolean on);

	/**
	 * Is floppy speeder Jiffydos installed?
	 * 
	 * @return floppy speeder Jiffydos installed?
	 */
	boolean isJiffyDosInstalled();

	/**
	 * Install floppy speeder Jiffydos.
	 * 
	 * @param on Floppy speeder Jiffydos installed?
	 */
	void setJiffyDosInstalled(boolean on);

	/**
	 * Is RAM expansion at some particular slot enabled
	 *
	 * @return state enabled?
	 */
	boolean isRamExpansionEnabled0();

	boolean isRamExpansionEnabled1();

	boolean isRamExpansionEnabled2();

	boolean isRamExpansionEnabled3();

	boolean isRamExpansionEnabled4();

	/**
	 * Enable 8K Ram expansion.
	 *
	 * @param on enable 8K Ram expansion
	 */
	void setRamExpansionEnabled0(boolean on);

	void setRamExpansionEnabled1(boolean on);

	void setRamExpansionEnabled2(boolean on);

	void setRamExpansionEnabled3(boolean on);

	void setRamExpansionEnabled4(boolean on);

	/**
	 * Set type of floppy.
	 * 
	 * @param floppyType type of floppy
	 */
	void setFloppyType(FloppyType floppyType);

	/**
	 * Get type of floppy.
	 * 
	 * @return type of floppy
	 */
	FloppyType getFloppyType();

	default boolean isRamExpansion(int selector) {
		switch (selector) {
		case 0:
			return isRamExpansionEnabled0();
		case 1:
			return isRamExpansionEnabled1();
		case 2:
			return isRamExpansionEnabled2();
		case 3:
			return isRamExpansionEnabled3();
		case 4:
			return isRamExpansionEnabled4();

		default:
			throw new RuntimeException(String.format("Maximum Ram Expansions exceeded: %d!", selector));
		}
	}

}