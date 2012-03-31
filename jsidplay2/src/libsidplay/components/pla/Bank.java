package libsidplay.components.pla;

public abstract class Bank {
	/**
	 * Bank write. Default throws a RuntimException.
	 * 
	 * Override this method if you expect write operations on your bank.
	 * Leave unimplemented if it's logically/operationally impossible for
	 * writes to ever arrive to bank.
	 *
	 * @param address address to write to 
	 * @param value value to write
	 */
	public void write(int address, byte value) {
		throw new RuntimeException("Bank doesn't implement write()");
	}

	/**
	 * Bank read. Default throws a RuntimeException, so you probably
	 * should override this method, except if the Bank is only used in
	 * write context.
	 *
	 * @param address value to read from
	 * @return value at address
	 */
	public byte read(int address) {
		throw new RuntimeException("Bank doesn't implement read()");
	}
}
