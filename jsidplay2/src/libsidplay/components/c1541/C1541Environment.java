package libsidplay.components.c1541;

/**
 * Connection between the C64 and the floppy disk drive.
 * 
 * @author Ken Händel
 * 
 */
public interface C1541Environment {
	/**
	 * Write to IEC bus.
	 * 
	 * @param data
	 *            data to write
	 */
	void writeToIECBus(byte data);

	/**
	 * Read from IEC bus.
	 * 
	 * @return bus value
	 */
	byte readFromIECBus();
}
