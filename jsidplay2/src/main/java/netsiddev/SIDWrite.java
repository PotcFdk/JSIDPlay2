/**
 * 
 */
package netsiddev;

public final class SIDWrite {
	private static final byte PURE_DELAY = -1;
	private static final byte END = -2;

	private final int chip;
	private final byte reg;
	private final byte value;
	private final int cycles;

	/**
	 * This command is a general write command to SID.
	 * Reg must be between 0 .. 0x1f and cycles > 0.
	 * 
	 * @param chip    The specified SID chip to write to.
	 * @param reg     The SID register to write to.
	 * @param data    The data to write to the specified SID register.
	 * @param cycles  Cycles to spend on writing the data.
	 * @throws InvalidCommandException
	 */
	public SIDWrite(final int chip, final byte reg, final byte data, final int cycles) throws InvalidCommandException {
		if (reg < 0 || reg > 0x1f) {
			throw new InvalidCommandException("Register value is not between 0 .. 0x1f: " + reg);
		}
		if (cycles < 0) {
			throw new InvalidCommandException("Cycle interval must be >= 0: " + cycles);
		}
		
		this.chip = chip;
		this.reg = reg;
		this.value = data;
		this.cycles = cycles;
	}

	private SIDWrite(final int sid, final byte cmd, final int cycles) throws InvalidCommandException {
		if (cycles <= 0) {
			throw new InvalidCommandException("Cycle interval must be > 0: " + cycles);
		}

		this.chip = sid;
		this.reg = cmd;
		this.value = 0;
		this.cycles = cycles;
	}

	private SIDWrite(final int sid, final byte cmd) {
		this.chip = sid;
		this.reg = cmd;
		this.value = 0;
		this.cycles = 0;
	}

	/**
	 * This command instructs AudioGeneratorThread about the need to execute a pure delay on specified SID.
	 * Throws if cycles < 0.
	 * 
	 * @param sid    The SID to execute a pure delay on.
	 * @param cycles Amount of cycles to execute the pure delay for.
	 *
	 * @return A new SIDWrite instance.
	 * @throws InvalidCommandException
	 */
	public static SIDWrite makePureDelay(final int sid, final int cycles) throws InvalidCommandException {
		return new SIDWrite(sid, PURE_DELAY, cycles);
	}
	
	/**
	 * Is command a no-write command?
	 * 
	 * @return True if the command is a no-write command; false otherwise.
	 */
	protected boolean isPureDelay() {
		return reg == PURE_DELAY;
	}

	/**
	 * This command instructs AudioGeneratorThread to exit cleanly.
	 * 
	 * @return A new SIDWrite instance.
	 */
	public static SIDWrite makeEnd() {
		return new SIDWrite(0, END);
	}

	/**
	 * Is an "END" command?
	 * 
	 * @return True if the reg is an END command; false otherwise.
	 */
	protected boolean isEnd() {
		return reg == END;
	}

	/**
	 * Gets the SID chip being used in this SIDWrite instance.
	 *
	 * @return The SID chip being used in this SIDWrite instance.
	 */
	protected int getChip() {
		return chip;
	}

	/**
	 * Gets the register being written to in this SIDWrite instance.
	 *
	 * @return The register being written to in this SIDWrite instance.
	 */
	protected byte getRegister() {
		return reg;
	}

	/**
	 * Gets the value being written to the register in this SIDWrite instance.
	 *
	 * @return The value being written to the register in this SIDWrite instance.
	 */
	protected byte getValue() {
		return value;
	}

	/**
	 * Gets the number of cycles writing will take in this SIDWrite instance.
	 *
	 * @return The number of cycles writing will take in this SIDWrite instance.
	 */
	protected int getCycles() {
		return cycles;
	}
}