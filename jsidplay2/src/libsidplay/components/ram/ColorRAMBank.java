package libsidplay.components.ram;

import java.util.Arrays;

import libsidplay.components.pla.Bank;

public final class ColorRAMBank extends Bank {
	private final byte[] ram = new byte[0x400];

	public void reset() {
		Arrays.fill(ram, (byte) 0);
	}
		
	@Override
	public void write(int address, byte value) {
		ram[address & 0x3ff] = (byte) (value & 0xf);
	}

	@Override
	public byte read(int address) {
		return ram[address & 0x3ff];
	}
}
