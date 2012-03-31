package resid_builder;


public class FilterConfig {
	/* distortion tunables */
	private float attenuation, nonlinearity, voiceNonlinearity;
	
	/* type 3 tunables */
	private float baseresistance, offset, steepness, minimumfetresistance;

	/* type 4 tunables */
	private float k, b;

	/* resonance factor */
	private float resonanceFactor;
	
	protected FilterConfig() {
	}
	
	public float getAttenuation() {
		return attenuation;
	}

	public void setAttenuation(float attenuation) {
		this.attenuation = attenuation;
	}

	public float getNonlinearity() {
		return nonlinearity;
	}

	public void setNonlinearity(float nonlinearity) {
		this.nonlinearity = nonlinearity;
	}

	public float getVoiceNonlinearity() {
		return voiceNonlinearity;
	}

	public void setVoiceNonlinearity(float voiceNonlinearity) {
		this.voiceNonlinearity = voiceNonlinearity;
	}

	public float getBaseresistance() {
		return baseresistance;
	}

	public void setBaseresistance(float baseresistance) {
		this.baseresistance = baseresistance;
	}

	public float getOffset() {
		return offset;
	}

	public void setOffset(float offset) {
		this.offset = offset;
	}

	public float getSteepness() {
		return steepness;
	}

	public void setSteepness(float steepness) {
		this.steepness = steepness;
	}

	public float getMinimumfetresistance() {
		return minimumfetresistance;
	}

	public void setMinimumfetresistance(float minimumfetresistance) {
		this.minimumfetresistance = minimumfetresistance;
	}

	public float getK() {
		return k;
	}

	public void setK(float k) {
		this.k = k;
	}

	public float getB() {
		return b;
	}

	public void setB(float b) {
		this.b = b;
	}

	public float getResonanceFactor() {
		return resonanceFactor;
	}

	public void setResonanceFactor(float resonanceFactor) {
		this.resonanceFactor = resonanceFactor;
	}
}