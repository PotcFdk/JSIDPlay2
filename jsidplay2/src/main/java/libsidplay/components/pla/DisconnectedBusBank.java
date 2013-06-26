package libsidplay.components.pla;

import libsidplay.components.mos656x.VIC;

/**
 * When nobody is supplying real chips for IO1/IO2, the reads read stale
 * bus data from VIC's previous memory interaction.
 * 
 * @author Antti Lankila
 */
public final class DisconnectedBusBank extends Bank {
	private final VIC vic;

	public DisconnectedBusBank(VIC vic2) {
		this.vic = vic2;
	}
	
	@Override
	public byte read(int address) {
		return vic.getLastReadByte();
	}

	@Override
	public void write(int address, byte value) {
	}
}
