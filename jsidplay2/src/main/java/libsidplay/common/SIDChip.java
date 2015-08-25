package libsidplay.common;

import java.util.function.IntConsumer;

/**
 * Interface for the SID chip re-implementation.
 * 
 * @author ken
 *
 */
public interface SIDChip {
	/**
	 * Max SID filter FC value.
	 */
	static final int FC_MAX = 2048;

	void clock(int piece, IntConsumer sample);

	byte read(int register);

	void write(int register, byte value);

	void reset();

	void mute(int voiceNo, boolean mute);

	void input(int input);

	void setClockFrequency(double clockFrequency);

	ChipModel getChipModel();

	void setChipModel(ChipModel model);

	int getInputDigiBoost();

}
