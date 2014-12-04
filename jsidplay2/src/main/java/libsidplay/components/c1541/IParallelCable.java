package libsidplay.components.c1541;

/**
 * Parallel cable is a custom modification between C64 and C1541, connecting the
 * two via C64's user port and extra wiring attached to the one of the VIA
 * chips. In addition of 8 data lines, 2 signal wires are connected to let both
 * parties be aware of when the other is ready.
 * 
 * The pulse() is used to indicate the drive that C64 is ready; the signal is
 * constructed by the CIA automatically on any use of register PRB.
 * 
 * The other direction is handled via the handshake parameter on the
 * driveWrite().
 * 
 * @author Ken Händel
 */
public interface IParallelCable {
	/**
	 * Floppy writes data.
	 * 
	 * @param data
	 *            written data
	 * @param handshake
	 *            handshake?
	 * @param dnr
	 *            drive ID
	 */
	void driveWrite(byte data, boolean handshake, int dnr);

	/**
	 * Floppy reads data.
	 * 
	 * @param handshake
	 *            handshake?
	 * @return read data
	 */
	byte driveRead(boolean handshake);

	/**
	 * C64 writes data.
	 * 
	 * @param data
	 *            written data
	 */
	void c64Write(byte data);

	/**
	 * C64 reads data.
	 * 
	 * @return read data
	 */
	byte c64Read();

	/**
	 * CIA synchronization.
	 */
	void pulse();
}
