package libsidplay.common;

import resid_builder.resid.ChipModel;
import resid_builder.resid.SamplingMethod;

public interface SIDChip {

	int clock(int piece, int[] audioBuffer, int offset);

	void clockSilent(int cycles);

	byte read(int register);

	void write(int register, byte value);

	void reset();

	void mute(int voiceNo, boolean mute);
	void input(int input);

	void setSamplingParameters(double clockFrequency, SamplingMethod method,
			double samplingFrequency, double highestAccurateFrequency);

	ChipModel getChipModel();
}
