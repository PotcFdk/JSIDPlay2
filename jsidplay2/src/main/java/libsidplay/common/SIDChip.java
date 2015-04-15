package libsidplay.common;

import java.util.function.IntConsumer;

public interface SIDChip {
	/**
	 * Max SID filter FC value.
	 */
	public static final int FC_MAX = 2048;

	void clock(int piece, IntConsumer sample);

	void clockSilent(int cycles);

	byte read(int register);

	void write(int register, byte value);

	void reset();

	void mute(int voiceNo, boolean mute);
	void input(int input);

	void setClockFrequency(double clockFrequency);

	ChipModel getChipModel();
	void setChipModel(ChipModel model);
	
	int getInputDigiBoost();

	byte readENV(int voiceNum);

	byte readOSC(int voiceNum);
}
