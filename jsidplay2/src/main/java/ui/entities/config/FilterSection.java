package ui.entities.config;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Entity;
import javax.persistence.Id;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.config.IFilterSection;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Entity
@Access(AccessType.PROPERTY)
public class FilterSection implements IFilterSection {

	public FilterSection() {
	}

	public FilterSection(IFilterSection f) {
		if (f.isReSIDFilter8580()) {
			setName(f.getName());
			setFilter8580CurvePosition(f.getFilter8580CurvePosition());
		} else if (f.isReSIDFilter6581()) {
			setName(f.getName());
			setFilter6581CurvePosition(f.getFilter6581CurvePosition());
		} else if (f.isReSIDfpFilter8580()) {
			setName(f.getName());
			setK(f.getK());
			setB(f.getB());
			setVoiceNonlinearity(f.getVoiceNonlinearity());
			setResonanceFactor(f.getResonanceFactor());
		} else if (f.isReSIDfpFilter6581()) {
			setName(f.getName());
			setAttenuation(f.getAttenuation());
			setNonlinearity(f.getNonlinearity());
			setVoiceNonlinearity(f.getVoiceNonlinearity());
			setBaseresistance(f.getBaseresistance());
			setOffset(f.getOffset());
			setSteepness(f.getSteepness());
			setMinimumfetresistance(f.getMinimumfetresistance());
			setResonanceFactor(f.getResonanceFactor());
		}
	}

	private ShadowField<StringProperty, String> name = new ShadowField<>(SimpleStringProperty::new, null);

	@Id
	@Override
	public String getName() {
		return name.get();
	}

	@Override
	public void setName(String name) {
		this.name.set(name);
	}

	public final StringProperty nameProperty() {
		return name.property();
	}

	private ShadowField<FloatProperty, Number> filter8580CurvePosition = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getFilter8580CurvePosition() {
		return filter8580CurvePosition.get().floatValue();
	}

	@Override
	public void setFilter8580CurvePosition(float filter8580CurvePosition) {
		this.filter8580CurvePosition.set(filter8580CurvePosition);
	}

	public final FloatProperty filter8580CurvePositionProperty() {
		return filter8580CurvePosition.property();
	}

	private ShadowField<FloatProperty, Number> filter6581CurvePosition = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getFilter6581CurvePosition() {
		return filter6581CurvePosition.get().floatValue();
	}

	@Override
	public void setFilter6581CurvePosition(float filter6581CurvePosition) {
		this.filter6581CurvePosition.set(filter6581CurvePosition);
	}

	public final FloatProperty filter6581CurvePositionProperty() {
		return filter6581CurvePosition.property();
	}

	private ShadowField<FloatProperty, Number> attenuation = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getAttenuation() {
		return attenuation.get().floatValue();
	}

	@Override
	public void setAttenuation(float attenuation) {
		this.attenuation.set(attenuation);
	}

	public final FloatProperty attenuationProperty() {
		return attenuation.property();
	}

	private ShadowField<FloatProperty, Number> nonlinearity = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getNonlinearity() {
		return nonlinearity.get().floatValue();
	}

	@Override
	public void setNonlinearity(float nonlinearity) {
		this.nonlinearity.set(nonlinearity);
	}

	public final FloatProperty nonlinearityProperty() {
		return nonlinearity.property();
	}

	private ShadowField<FloatProperty, Number> voiceNonlinearity = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getVoiceNonlinearity() {
		return voiceNonlinearity.get().floatValue();
	}

	@Override
	public void setVoiceNonlinearity(float voiceNonlinearity) {
		this.voiceNonlinearity.set(voiceNonlinearity);
	}

	public final FloatProperty voiceNonlinearityProperty() {
		return voiceNonlinearity.property();
	}

	private ShadowField<FloatProperty, Number> baseresistance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getBaseresistance() {
		return baseresistance.get().floatValue();
	}

	@Override
	public void setBaseresistance(float baseresistance) {
		this.baseresistance.set(baseresistance);
	}

	public final FloatProperty baseresistanceProperty() {
		return baseresistance.property();
	}

	private ShadowField<FloatProperty, Number> offset = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getOffset() {
		return offset.get().floatValue();
	}

	@Override
	public void setOffset(float offset) {
		this.offset.set(offset);
	}

	public final FloatProperty offsetProperty() {
		return offset.property();
	}

	private ShadowField<FloatProperty, Number> steepness = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getSteepness() {
		return steepness.get().floatValue();
	}

	@Override
	public void setSteepness(float steepness) {
		this.steepness.set(steepness);
	}

	public final FloatProperty steepnessProperty() {
		return steepness.property();
	}

	private ShadowField<FloatProperty, Number> minimumfetresistance = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getMinimumfetresistance() {
		return minimumfetresistance.get().floatValue();
	}

	@Override
	public void setMinimumfetresistance(float minimumfetresistance) {
		this.minimumfetresistance.set(minimumfetresistance);
	}

	public final FloatProperty minimumfetresistanceProperty() {
		return minimumfetresistance.property();
	}

	private ShadowField<FloatProperty, Number> k = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getK() {
		return k.get().floatValue();
	}

	@Override
	public void setK(float k) {
		this.k.set(k);
	}

	public final FloatProperty kProperty() {
		return k.property();
	}

	private ShadowField<FloatProperty, Number> b = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getB() {
		return b.get().floatValue();
	}

	@Override
	public void setB(float b) {
		this.b.set(b);
	}

	public final FloatProperty bProperty() {
		return b.property();
	}

	private ShadowField<FloatProperty, Number> resonanceFactor = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), 0);

	@Override
	public float getResonanceFactor() {
		return resonanceFactor.get().floatValue();
	}

	@Override
	public void setResonanceFactor(float resonanceFactor) {
		this.resonanceFactor.set(resonanceFactor);
	}

	public final FloatProperty resonanceFactorProperty() {
		return resonanceFactor.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
