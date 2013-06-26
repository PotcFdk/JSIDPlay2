package libsidplay.components.ram;

import java.util.Arrays;

import libsidplay.components.pla.Bank;

/**
 * Area backed by RAM
 * 
 * @author Antti Lankila
 */
public final class SystemRAMBank extends Bank {
	/** C64 RAM area */
	private final byte ram[] = new byte[65536];
	
	public void reset() {
		// Initialize RAM with powerup pattern
		Arrays.fill(ram, (byte) 0);
		for (int i = 0x07c0; i < 0x10000; i += 128) {
			Arrays.fill(ram, i, i + 64, (byte) 0xff);
		}
	}
		
	@Override
	public byte read(int address) {
		return ram[address];
	}
		
	@Override
	public void write(int address, byte value) {
		ram[address] = value;
	}
		
	public byte[] array() {
		return ram;
	}
}
