package sidplay.ini.intf;

public interface IFilterSection {

	String getName();

	void setName(String name);

	float getFilter8580CurvePosition();

	void setFilter8580CurvePosition(float filter8580CurvePosition);

	float getFilter6581CurvePosition();

	void setFilter6581CurvePosition(float filter6581CurvePosition);

	float getAttenuation();

	void setAttenuation(float attenuation);

	float getNonlinearity();

	void setNonlinearity(float nonlinearity);

	float getVoiceNonlinearity();

	void setVoiceNonlinearity(float voiceNonlinearity);

	float getBaseresistance();

	void setBaseresistance(float baseresistance);

	float getOffset();

	void setOffset(float offset);

	float getSteepness();

	void setSteepness(float steepness);

	float getMinimumfetresistance();

	void setMinimumfetresistance(float minimumfetresistance);

	float getK();

	void setK(float k);

	float getB();

	void setB(float b);

	float getResonanceFactor();

	void setResonanceFactor(float resonanceFactor);

	default boolean isReSIDFilter6581() {
		return getFilter6581CurvePosition() != 0;
	}

	default boolean isReSIDFilter8580() {
		return getFilter8580CurvePosition() != 0;
	}

	default boolean isReSIDfpFilter6581() {
		return getBaseresistance() != 0;
	}

	default boolean isReSIDfpFilter8580() {
		return getK() != 0;
	}
}