package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;

import libsidplay.config.IFilterSection;

@Entity
@Access(AccessType.PROPERTY)
public class FilterSection implements IFilterSection {

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

	public float getOffset() {
		return offset;
	}

	public void setOffset(float offset) {
		this.offset = offset;
	}

	private float steepness;

	public float getSteepness() {
		return steepness;
	}

	public void setSteepness(float steepness) {
		this.steepness = steepness;
	}

	private float minimumfetresistance;

	public float getMinimumfetresistance() {
		return minimumfetresistance;
	}

	public void setMinimumfetresistance(float minimumfetresistance) {
		this.minimumfetresistance = minimumfetresistance;
	}

	private float k;

	public float getK() {
		return k;
	}

	public void setK(float k) {
		this.k = k;
	}

	private float b;

	public float getB() {
		return b;
	}

	public void setB(float b) {
		this.b = b;
	}

	private float resonanceFactor;

	public float getResonanceFactor() {
		return resonanceFactor;
	}

	public void setResonanceFactor(float resonanceFactor) {
		this.resonanceFactor = resonanceFactor;
	}
}
