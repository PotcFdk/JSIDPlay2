package applet;

import libsidplay.common.ISID2Types.Clock;
import libsidplay.components.c1541.C1541.FloppyType;

/**
 * Implement this to react on menu selections.
 * 
 * @author Ken Händel
 * 
 */
public interface IMenuListener {

	/**
	 * Load a program file (one-filer).
	 */
	void loadProgram();

	/**
	 * Load a REU video file (cartridge+program).
	 */
	void loadVideo();

	/**
	 * Reset C64.
	 */
	void reset();

	/**
	 * Quit player.
	 */
	void quit();

	/**
	 * Set video standard.
	 * 
	 * @param standard
	 *            video standard
	 */
	void setVideoStandard(Clock standard);

	/**
	 * Open sound settings.
	 */
	void soundSettings();

	/**
	 * Open emulation settings.
	 */
	void emulationSettings();

	/**
	 * Open joystick settings.
	 */
	void joystickSettings();

	/**
	 * Display about window
	 */
	void aboutView();

	/**
	 * Turn disk drive on.
	 * 
	 * @param state
	 *            on/off
	 */
	void turnDriveOn(boolean state);

	/**
	 * Turn on disk drive sound.
	 * 
	 * @param state
	 *            enable drive sound
	 */
	void driveSound(boolean state);

	/**
	 * Parallel floppy cable.
	 * 
	 * @param state
	 *            plug in floppy cable
	 */
	void parallelCable(boolean state);

	/**
	 * Set type of floppy.
	 * 
	 * @param floppyType
	 *            type of floppy
	 */
	void setFloppyType(FloppyType floppyType);

	/**
	 * Enable 8K Ram expansion.
	 * 
	 * @param selector
	 *            which 8KB RAM bank to expand (0-5), starting at 0x2000
	 *            increasing in 8KB steps up to 0xA000.
	 * @param expand
	 *            enable 8K Ram expansion
	 */
	void setRamExpansion(int selector, boolean expand);

	/**
	 * Insert a disk into floppy drive.
	 */
	void insertDisk();

	/**
	 * Eject disk from floppy drive.
	 */
	void ejectDisk();

	/**
	 * Turn printer drive on.
	 * 
	 * @param state
	 *            on/off
	 */
	void turnPrinterOn(boolean state);

	/**
	 * Insert a tape to the tape drive.
	 */
	void insertTape();

	/**
	 * Eject tape from tape drive.
	 */
	void ejectTape();

	/**
	 * Press record on tape.
	 */
	void record();

	/**
	 * Press play on tape.
	 */
	void play();

	/**
	 * Press rewind on tape.
	 */
	void rewind();

	/**
	 * Press forward on tape.
	 */
	void forward();

	/**
	 * Press stop on tape.
	 */
	void stopTape();

	/**
	 * Reset tape counter.
	 */
	void resetCounter();

	/**
	 * Insert a cartridge into expansion port.
	 */
	void insertCartridge();

	/**
	 * Insert GEORAM.
	 */
	void insertGEORAM();

	/**
	 * Insert GEORAM.
	 * 
	 * @param sizeKB
	 *            size in KB
	 */
	void insertGEORAM(int sizeKB);
	
	/**
	 * Eject cartridge from expansion port.
	 */
	void ejectCartridge();

	/**
	 * Press freeze on the cartridge.
	 */
	void freeze();

	/**
	 * Open Memory disassembler tool.
	 */
	void memory();

	/**
	 * Open SID Dump tool.
	 */
	void siddump();

	/**
	 * Open SID register tool.
	 */
	void sidregisters();

	/**
	 * Pause SID player.
	 * 
	 * @param true (pause), false (continue)
	 */
	void pause(boolean state);

	/**
	 * Play previous song.
	 */
	void previous();

	/**
	 * Play next song.
	 */
	void next();

	/**
	 * Play normal speed.
	 */
	void normalSpeed();

	/**
	 * Play fast forward speed.
	 */
	void fastForward();

}
