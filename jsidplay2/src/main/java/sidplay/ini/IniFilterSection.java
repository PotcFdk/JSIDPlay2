package sidplay.ini;

import libsidplay.config.IFilterSection;

public class IniFilterSection implements IFilterSection {
	private final IniReader ini;

	public IniFilterSection(IniReader ini, String name) {
		this.ini = ini;
		this.name = name;
	}

	private String name;

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public float getFilter8580CurvePosition() {
		return ini.getPropertyFloat(name, "Filter8580CurvePosition", 0);
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		ini.setProperty(name, "Filter8580CurvePosition",
				filter8580CurvePosition);
	}

	@Override
	public float getFilter6581CurvePosition() {
		return ini.getPropertyFloat(name, "Filter6581CurvePosition", 0);
	}

	@Override
	public void setFilter6581CurvePosition(float filter6581CurvePosition) {
		ini.setProperty(name, "Filter6581CurvePosition",
				filter6581CurvePosition);
	}

	public float getAttenuation() {
		return ini.getPropertyFloat(name, "Attenuation", 0);
	}

	public void setAttenuation(float attenuation) {
		ini.setProperty(name, "Attenuation", String.valueOf(attenuation));
	}

	public float getNonlinearity() {
		return ini.getPropertyFloat(name, "Nonlinearity", 0);
	}

	public void setNonlinearity(float nonlinearity) {
		ini.setProperty(name, "Nonlinearity", String.valueOf(nonlinearity));
	}

	public float getVoiceNonlinearity() {
		return ini.getPropertyFloat(name, "VoiceNonlinearity", 0);
	}

	public void setVoiceNonlinearity(float voiceNonlinearity) {
		ini.setProperty(name, "VoiceNonlinearity",
				String.valueOf(voiceNonlinearity));
	}

	public float getBaseresistance() {
		return ini.getPropertyFloat(name, "Type3BaseResistance", 0);
	}

	public void setBaseresistance(float baseresistance) {
		ini.setProperty(name, "Type3BaseResistance",
				String.valueOf(baseresistance));
	}

	public float getOffset() {
		return ini.getPropertyFloat(name, "Type3Offset", 0);
	}

	public void setOffset(float offset) {
		ini.setProperty(name, "Type3Offset", String.valueOf(offset));
	}

	public float getSteepness() {
		return ini.getPropertyFloat(name, "Type3Steepness", 0);
	}

	public void setSteepness(float steepness) {
		ini.setProperty(name, "Type3Steepness", String.valueOf(steepness));
	}

	public float getMinimumfetresistance() {
		return ini.getPropertyFloat(name, "Type3MinimumFETResistance", 0);
	}

	public void setMinimumfetresistance(float minimumfetresistance) {
		ini.setProperty(name, "Type3MinimumFETResistance",
				String.valueOf(minimumfetresistance));
	}

	public float getK() {
		return ini.getPropertyFloat(name, "Type4K", 0);
	}

	public void setK(float k) {
		ini.setProperty(name, "Type4K", String.valueOf(k));
	}

	public float getB() {
		return ini.getPropertyFloat(name, "Type4B", 0);
	}

	public void setB(float b) {
		ini.setProperty(name, "Type4B", String.valueOf(b));
	}

	public float getResonanceFactor() {
		return ini.getPropertyFloat(name, "ResonanceFactor", 0);
	}

	public void setResonanceFactor(float resonanceFactor) {
		ini.setProperty(name, "ResonanceFactor",
				String.valueOf(resonanceFactor));
	}
}