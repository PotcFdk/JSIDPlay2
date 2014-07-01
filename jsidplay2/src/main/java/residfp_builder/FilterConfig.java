package residfp_builder;

import sidplay.ini.IniReader;

public class FilterConfig {
	static final String FILTER_8580 = "Filter8580";
	static final String FILTER_6581 = "Filter6581";

	private final IniReader ini;
	private final String heading;
	
	/* distortion tunables */
	private float attenuation, nonlinearity, voiceNonlinearity;
	
	/* type 3 tunables */
	private float baseresistance, offset, steepness, minimumfetresistance;

	/* type 4 tunables */
	private float k, b;

	/* resonance factor */
	private float resonanceFactor;
	
	protected FilterConfig(IniReader ini, String heading) {
		this.ini = ini;
		this.heading = heading;
	}

	public static FilterConfig read(final IniReader ini, final String heading) {
		FilterConfig m_filter = new FilterConfig(ini, heading);

		m_filter.resonanceFactor = (float) ini.getPropertyFloat(heading, "ResonanceFactor", 1.0f);
		/*
		 * Ensure that all parameters are zeroed if missing. These are only relevant for 6581, and must not be set for 8580 filter type.
		 */
		double propertyDouble;
		propertyDouble = ini.getPropertyFloat(heading, "Attenuation", 0);
		if (propertyDouble != 0) {
			m_filter.attenuation = (float) propertyDouble;
		}

		propertyDouble = ini.getPropertyFloat(heading, "Nonlinearity", 0);
		if (propertyDouble != 0) {
			m_filter.nonlinearity = (float) propertyDouble;
		}

		propertyDouble = ini.getPropertyFloat(heading, "VoiceNonlinearity", 0);
		if (propertyDouble != 0) {
			m_filter.voiceNonlinearity = (float) propertyDouble;
		}

		propertyDouble = ini.getPropertyFloat(heading, "Type3BaseResistance", 0);
		if (propertyDouble != 0) {
			m_filter.baseresistance = (float) propertyDouble;
			propertyDouble = ini.getPropertyFloat(heading, "Type3Offset", 0);
			m_filter.offset = (float) propertyDouble;
			propertyDouble = ini.getPropertyFloat(heading, "Type3Steepness", 0);
			m_filter.steepness = (float) propertyDouble;
			propertyDouble = ini.getPropertyFloat(heading, "Type3MinimumFETResistance", 0);
			m_filter.minimumfetresistance = (float) propertyDouble;
		} else {
			propertyDouble = ini.getPropertyFloat(heading, "Type4K", 0);
			if (propertyDouble != 0) {
				m_filter.k = (float) propertyDouble;
				propertyDouble = ini.getPropertyFloat(heading, "Type4B", 0);
				m_filter.b = (float) propertyDouble;
			}
		}
		
		return m_filter;
	}
	
	public float getAttenuation() {
		return attenuation;
	}

	public void setAttenuation(float attenuation) {
		this.attenuation = attenuation;
		ini.setProperty(heading, "Attenuation", String.valueOf(attenuation));
	}

	public float getNonlinearity() {
		return nonlinearity;
	}

	public void setNonlinearity(float nonlinearity) {
		this.nonlinearity = nonlinearity;
		ini.setProperty(heading, "Nonlinearity", String.valueOf(nonlinearity));
	}

	public float getVoiceNonlinearity() {
		return voiceNonlinearity;
	}

	public void setVoiceNonlinearity(float voiceNonlinearity) {
		this.voiceNonlinearity = voiceNonlinearity;
		ini.setProperty(heading, "VoiceNonlinearity", String.valueOf(voiceNonlinearity));
	}

	public float getBaseresistance() {
		return baseresistance;
	}

	public void setBaseresistance(float baseresistance) {
		this.baseresistance = baseresistance;
		ini.setProperty(heading, "Type3BaseResistance", String.valueOf(baseresistance));
	}

	public float getOffset() {
		return offset;
	}

	public void setOffset(float offset) {
		this.offset = offset;
		ini.setProperty(heading, "Type3Offset", String.valueOf(offset));
	}

	public float getSteepness() {
		return steepness;
	}

	public void setSteepness(float steepness) {
		this.steepness = steepness;
		ini.setProperty(heading, "Type3Steepness", String.valueOf(steepness));
	}

	public float getMinimumfetresistance() {
		return minimumfetresistance;
	}

	public void setMinimumfetresistance(float minimumfetresistance) {
		this.minimumfetresistance = minimumfetresistance;
		ini.setProperty(heading, "Type3MinimumFETResistance", String.valueOf(minimumfetresistance));
	}

	public float getK() {
		return k;
	}

	public void setK(float k) {
		this.k = k;
		ini.setProperty(heading, "Type4K", String.valueOf(k));
	}

	public float getB() {
		return b;
	}

	public void setB(float b) {
		this.b = b;
		ini.setProperty(heading, "Type4B", String.valueOf(b));
	}

	public float getResonanceFactor() {
		return resonanceFactor;
	}

	public void setResonanceFactor(float resonanceFactor) {
		this.resonanceFactor = resonanceFactor;
		ini.setProperty(heading, "ResonanceFactor", String.valueOf(resonanceFactor));
	}
}