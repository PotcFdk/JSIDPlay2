package libsidplay.components.c1541;

/**
 * This class represents a disconnected parallel port.
 * 
 * @author Antti Lankila
 */
public class DisconnectedParallelCable implements IParallelCable {
	/**
	 * Writes of the floppy go to nowhere.
	 * 
	 * @see libsidplay.components.c1541.IParallelCable#driveWrite(byte, boolean,
	 *      int)
	 */
	@Override
	public void driveWrite(byte data, boolean handshake, int dnr) {
	}

	/**
	 * Reads of the floppy always 0xff.
	 * 
	 * @see libsidplay.components.c1541.IParallelCable#driveRead(boolean)
	 */
	@Override
	public byte driveRead(boolean handshake) {
		return (byte) 0xff;
	}

	/**
	 * Writes of the CPU go to nowhere.
	 * 
	 * @see libsidplay.components.c1541.IParallelCable#c64Write(byte)
	 */
	@Override
	public void c64Write(byte data) {
	}

	/**
	 * Reads of the CPU always 0xff.
	 * 
	 * @see libsidplay.components.c1541.IParallelCable#c64Read()
	 */
	@Override
	public byte c64Read() {
		return (byte) 0xff;
	}

	/**
	 * CIA signals are ignored.
	 * 
	 * @see libsidplay.components.c1541.IParallelCable#pulse()
	 */
	@Override
	public void pulse() {
	}
}
