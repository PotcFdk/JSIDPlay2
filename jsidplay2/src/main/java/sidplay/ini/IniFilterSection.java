package sidplay.ini;

import libsidplay.config.IFilterSection;
import sidplay.ini.converter.BeanToStringConverter;

public class IniFilterSection implements IFilterSection {
	private final IniReader ini;

	public IniFilterSection(IniReader ini, String name) {
		this.ini = ini;
		this.name = name;
	}

	private String name;

	@Override
	public final String getName() {
		return name;
	}

	@Override
	public final void setName(String name) {
		this.name = name;
	}

	@Override
	public final float getFilter8580CurvePosition() {
		return ini.getPropertyFloat(name, "Filter8580CurvePosition", 0);
	}

	@Override
	public final void setFilter8580CurvePosition(float filter8580CurvePosition) {
		ini.setProperty(name, "Filter8580CurvePosition", filter8580CurvePosition);
	}

	@Override
	public final float getFilter6581CurvePosition() {
		return ini.getPropertyFloat(name, "Filter6581CurvePosition", 0);
	}

	@Override
	public final void setFilter6581CurvePosition(float filter6581CurvePosition) {
		ini.setProperty(name, "Filter6581CurvePosition", filter6581CurvePosition);
	}

	@Override
	public final float getAttenuation() {
		return ini.getPropertyFloat(name, "Attenuation", 0);
	}

	@Override
	public final void setAttenuation(float attenuation) {
		ini.setProperty(name, "Attenuation", String.valueOf(attenuation));
	}

	@Override
	public final float getNonlinearity() {
		return ini.getPropertyFloat(name, "Nonlinearity", 0);
	}

	@Override
	public final void setNonlinearity(float nonlinearity) {
		ini.setProperty(name, "Nonlinearity", String.valueOf(nonlinearity));
	}

	@Override
	public final float getVoiceNonlinearity() {
		return ini.getPropertyFloat(name, "VoiceNonlinearity", 0);
	}

	@Override
	public final void setVoiceNonlinearity(float voiceNonlinearity) {
		ini.setProperty(name, "VoiceNonlinearity", String.valueOf(voiceNonlinearity));
	}

	@Override
	public final float getBaseresistance() {
		return ini.getPropertyFloat(name, "Type3BaseResistance", 0);
	}

	@Override
	public final void setBaseresistance(float baseresistance) {
		ini.setProperty(name, "Type3BaseResistance", String.valueOf(baseresistance));
	}

	@Override
	public final float getOffset() {
		return ini.getPropertyFloat(name, "Type3Offset", 0);
	}

	@Override
	public final void setOffset(float offset) {
		ini.setProperty(name, "Type3Offset", String.valueOf(offset));
	}

	@Override
	public final float getSteepness() {
		return ini.getPropertyFloat(name, "Type3Steepness", 0);
	}

	@Override
	public final void setSteepness(float steepness) {
		ini.setProperty(name, "Type3Steepness", String.valueOf(steepness));
	}

	@Override
	public final float getMinimumfetresistance() {
		return ini.getPropertyFloat(name, "Type3MinimumFETResistance", 0);
	}

	@Override
	public final void setMinimumfetresistance(float minimumfetresistance) {
		ini.setProperty(name, "Type3MinimumFETResistance", String.valueOf(minimumfetresistance));
	}

	@Override
	public final float getK() {
		return ini.getPropertyFloat(name, "Type4K", 0);
	}

	@Override
	public final void setK(float k) {
		ini.setProperty(name, "Type4K", String.valueOf(k));
	}

	@Override
	public final float getB() {
		return ini.getPropertyFloat(name, "Type4B", 0);
	}

	@Override
	public final void setB(float b) {
		ini.setProperty(name, "Type4B", String.valueOf(b));
	}

	@Override
	public final float getResonanceFactor() {
		return ini.getPropertyFloat(name, "ResonanceFactor", 0);
	}

	@Override
	public final void setResonanceFactor(float resonanceFactor) {
		ini.setProperty(name, "ResonanceFactor", String.valueOf(resonanceFactor));
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}