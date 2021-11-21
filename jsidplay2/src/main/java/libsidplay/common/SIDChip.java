package libsidplay.common;

import java.util.function.IntConsumer;

/**
 * Interface for the SID chip re-implementation.
 *
 * @author ken
 *
 */
public interface SIDChip {
	/** Max SID filter FC value. */
	int FC_MAX = 2048;

	/** SID base address */
	int DEF_BASE_ADDRESS = 0xd400;

	/** Number of SID chip registers. */
	int REG_COUNT = 32;

	void clock(int piece, IntConsumer sample);

	byte read(int register);

	void write(int register, byte value);

	void reset();

	void mute(int voiceNo, boolean mute);

	void input(int input);

	void setClockFrequency(double clockFrequency);

	void setChipModel(ChipModel model);

	void setDigiBoost(boolean digiBoost);

}
