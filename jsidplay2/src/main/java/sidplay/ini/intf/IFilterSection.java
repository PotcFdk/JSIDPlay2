package sidplay.ini.intf;

public interface IFilterSection {

	public String getName();

	public void setName(String name);

	public float getFilter8580CurvePosition();

	public void setFilter8580CurvePosition(float filter8580CurvePosition);

	public float getFilter6581CurvePosition();

	public void setFilter6581CurvePosition(float filter6581CurvePosition);

	public float getAttenuation();

	public void setAttenuation(float attenuation);

	public float getNonlinearity();

	public void setNonlinearity(float nonlinearity);

	public float getVoiceNonlinearity();

	public void setVoiceNonlinearity(float voiceNonlinearity);

	public float getBaseresistance();

	public void setBaseresistance(float baseresistance);

	public float getOffset();

	public void setOffset(float offset);

	public float getSteepness();

	public void setSteepness(float steepness);

	public float getMinimumfetresistance();

	public void setMinimumfetresistance(float minimumfetresistance);

	public float getK();

	public void setK(float k);

	public float getB();

	public void setB(float b);

	public float getResonanceFactor();

	public void setResonanceFactor(float resonanceFactor);

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