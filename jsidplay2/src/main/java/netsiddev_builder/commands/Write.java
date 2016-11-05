package netsiddev_builder.commands;

public class Write {

	private int cycles;
	private byte reg;
	private byte data;

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
