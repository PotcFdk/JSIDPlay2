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
	 * @param chip
	 * @param reg
	 * @param data
	 * @param cycles
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
	 * @param sid
	 * @param cycles
	 * @return
	 * @throws InvalidCommandException
	 */
	public static SIDWrite makePureDelay(final int sid, final int cycles) throws InvalidCommandException {
		return new SIDWrite(sid, PURE_DELAY, cycles);
	}
	
	/**
	 * Is command a no-write command?
	 * 
	 * @return
	 */
	protected boolean isPureDelay() {
		return reg == PURE_DELAY;
	}

	/**
	 * This command instructs AudioGeneratorThread to exit cleanly.
	 * 
	 * @return
	 */
	public static SIDWrite makeEnd() {
		return new SIDWrite(0, END);
	}

	/**
	 * Is an "END" command?
	 * 
	 * @return
	 */
	protected boolean isEnd() {
		return reg == END;
	}
	
	protected int getChip() {
		return chip;
	}
	
	protected byte getRegister() {
		return reg;
	}
	
	protected byte getValue() {
		return value;
	}
	
	protected int getCycles() {
		return cycles;
	}
}