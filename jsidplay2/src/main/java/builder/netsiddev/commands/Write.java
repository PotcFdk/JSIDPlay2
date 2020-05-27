package builder.netsiddev.commands;

public class Write {

	private final int cycles;
	private final byte reg;
	private final byte data;

	public Write(int cycles, byte reg, byte data) {
		this.cycles = cycles;
		this.reg = reg;
		this.data = data;
	}

	public int getCycles() {
		return cycles;
	}

	public byte getReg() {
		return reg;
	}

	public byte getData() {
		return data;
	}
}
