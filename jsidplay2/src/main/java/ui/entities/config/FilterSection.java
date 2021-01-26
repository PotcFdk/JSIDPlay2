package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;

import libsidplay.config.IFilterSection;
import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
public class FilterSection implements IFilterSection {

	public FilterSection() {
	}

	public FilterSection(IFilterSection f) {
		if (f.isReSIDFilter8580()) {
			name = f.getName();
			filter8580CurvePosition = f.getFilter8580CurvePosition();
		} else if (f.isReSIDFilter6581()) {
			name = f.getName();
			filter6581CurvePosition = f.getFilter6581CurvePosition();
		} else if (f.isReSIDfpFilter8580()) {
			name = f.getName();
			k = f.getK();
			b = f.getB();
			voiceNonlinearity = f.getVoiceNonlinearity();
			resonanceFactor = f.getResonanceFactor();
		} else if (f.isReSIDfpFilter6581()) {
			name = f.getName();
			attenuation = f.getAttenuation();
			nonlinearity = f.getNonlinearity();
			voiceNonlinearity = f.getVoiceNonlinearity();
			baseresistance = f.getBaseresistance();
			offset = f.getOffset();
			steepness = f.getSteepness();
			minimumfetresistance = f.getMinimumfetresistance();
			resonanceFactor = f.getResonanceFactor();
		}
	}

	private String name;

	@Id
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	private float filter8580CurvePosition;

	@Override
	public float getFilter8580CurvePosition() {
		return filter8580CurvePosition;
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		this.filter8580CurvePosition = filter8580CurvePosition;
	}

	private float filter6581CurvePosition;

	@Override
	public float getFilter6581CurvePosition() {
		return filter6581CurvePosition;
	}

	@Override
	public void setFilter6581CurvePosition(float filter6581CurvePosition) {
		this.filter6581CurvePosition = filter6581CurvePosition;
	}

	private float attenuation;

	@Override
	public float getAttenuation() {
		return attenuation;
	}

	@Override
	public void setAttenuation(float attenuation) {
		this.attenuation = attenuation;
	}

	private float nonlinearity;

	@Override
	public float getNonlinearity() {
		return nonlinearity;
	}

	@Override
	public void setNonlinearity(float nonlinearity) {
		this.nonlinearity = nonlinearity;
	}

	private float voiceNonlinearity;

	@Override
	public float getVoiceNonlinearity() {
		return voiceNonlinearity;
	}

	@Override
	public void setVoiceNonlinearity(float voiceNonlinearity) {
		this.voiceNonlinearity = voiceNonlinearity;
	}

	private float baseresistance;

	@Override
	public float getBaseresistance() {
		return baseresistance;
	}

	@Override
	public void setBaseresistance(float baseresistance) {
		this.baseresistance = baseresistance;
	}

	private float offset;

	@Override
	public float getOffset() {
		return offset;
	}

	@Override
	public void setOffset(float offset) {
		this.offset = offset;
	}

	private float steepness;

	@Override
	public float getSteepness() {
		return steepness;
	}

	@Override
	public void setSteepness(float steepness) {
		this.steepness = steepness;
	}

	private float minimumfetresistance;

	@Override
	public float getMinimumfetresistance() {
		return minimumfetresistance;
	}

	@Override
	public void setMinimumfetresistance(float minimumfetresistance) {
		this.minimumfetresistance = minimumfetresistance;
	}

	private float k;

	@Override
	public float getK() {
		return k;
	}

	@Override
	public void setK(float k) {
		this.k = k;
	}

	private float b;

	@Override
	public float getB() {
		return b;
	}

	@Override
	public void setB(float b) {
		this.b = b;
	}

	private float resonanceFactor;

	@Override
	public float getResonanceFactor() {
		return resonanceFactor;
	}

	@Override
	public void setResonanceFactor(float resonanceFactor) {
		this.resonanceFactor = resonanceFactor;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
