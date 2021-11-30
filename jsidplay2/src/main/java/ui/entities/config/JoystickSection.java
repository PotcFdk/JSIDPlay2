package ui.entities.config;

import javax.persistence.Embeddable;

import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;
import ui.common.properties.ShadowField;

@Embeddable
public class JoystickSection {

	public static final float DEFAULT_COMPONENT_VALUE_UP_1 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_DOWN_1 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_LEFT_1 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_RIGHT_1 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_BTN_1 = 1.f;

	public static final float DEFAULT_COMPONENT_VALUE_UP_2 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_DOWN_2 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_LEFT_2 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_RIGHT_2 = 1.f;
	public static final float DEFAULT_COMPONENT_VALUE_BTN_2 = 1.f;

	private ShadowField<StringProperty, String> deviceName1 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getDeviceName1() {
		return deviceName1.get();
	}

	public final void setDeviceName1(String deviceName) {
		this.deviceName1.set(deviceName);
	}

	public final StringProperty deviceName1Property() {
		return deviceName1.property();
	}

	private ShadowField<StringProperty, String> deviceName2 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getDeviceName2() {
		return deviceName2.get();
	}

	public final void setDeviceName2(String deviceName) {
		this.deviceName2.set(deviceName);
	}

	public final StringProperty deviceName2Property() {
		return deviceName2.property();
	}

	private ShadowField<StringProperty, String> componentNameUp1 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameUp1() {
		return componentNameUp1.get();
	}

	public final void setComponentNameUp1(String componentNameUp) {
		this.componentNameUp1.set(componentNameUp);
	}

	public final StringProperty componentNameUp1Property() {
		return componentNameUp1.property();
	}

	private ShadowField<StringProperty, String> componentNameUp2 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameUp2() {
		return componentNameUp2.get();
	}

	public final void setComponentNameUp2(String componentNameUp) {
		this.componentNameUp2.set(componentNameUp);
	}

	public final StringProperty componentNameUp2Property() {
		return componentNameUp2.property();
	}

	private ShadowField<StringProperty, String> componentNameDown1 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameDown1() {
		return componentNameDown1.get();
	}

	public final void setComponentNameDown1(String componentNameDown) {
		this.componentNameDown1.set(componentNameDown);
	}

	public final StringProperty componentNameDown1Property() {
		return componentNameDown1.property();
	}

	private ShadowField<StringProperty, String> componentNameDown2 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameDown2() {
		return componentNameDown2.get();
	}

	public final void setComponentNameDown2(String componentNameDown) {
		this.componentNameDown2.set(componentNameDown);
	}

	public final StringProperty componentNameDown2Property() {
		return componentNameDown2.property();
	}

	private ShadowField<StringProperty, String> componentNameLeft1 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameLeft1() {
		return componentNameLeft1.get();
	}

	public final void setComponentNameLeft1(String componentNameLeft) {
		this.componentNameLeft1.set(componentNameLeft);
	}

	public final StringProperty componentNameLeft1Property() {
		return componentNameLeft1.property();
	}

	private ShadowField<StringProperty, String> componentNameLeft2 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameLeft2() {
		return componentNameLeft2.get();
	}

	public final void setComponentNameLeft2(String componentNameLeft) {
		this.componentNameLeft2.set(componentNameLeft);
	}

	public final StringProperty componentNameLeft2Property() {
		return componentNameLeft2.property();
	}

	private ShadowField<StringProperty, String> componentNameRight1 = new ShadowField<>(SimpleStringProperty::new,
			null);

	public final String getComponentNameRight1() {
		return componentNameRight1.get();
	}

	public final void setComponentNameRight1(String componentNameRight) {
		this.componentNameRight1.set(componentNameRight);
	}

	public final StringProperty componentNameRight1Property() {
		return componentNameRight1.property();
	}

	private ShadowField<StringProperty, String> componentNameRight2 = new ShadowField<>(SimpleStringProperty::new,
			null);

	public final String getComponentNameRight2() {
		return componentNameRight2.get();
	}

	public final void setComponentNameRight2(String componentNameRight) {
		this.componentNameRight2.set(componentNameRight);
	}

	public final StringProperty componentNameRight2Property() {
		return componentNameRight2.property();
	}

	private ShadowField<StringProperty, String> componentNameBtn1 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameBtn1() {
		return componentNameBtn1.get();
	}

	public final void setComponentNameBtn1(String componentNameBtn) {
		this.componentNameBtn1.set(componentNameBtn);
	}

	public final StringProperty componentNameBtn1Property() {
		return componentNameBtn1.property();
	}

	private ShadowField<StringProperty, String> componentNameBtn2 = new ShadowField<>(SimpleStringProperty::new, null);

	public final String getComponentNameBtn2() {
		return componentNameBtn2.get();
	}

	public final void setComponentNameBtn2(String componentNameBtn) {
		this.componentNameBtn2.set(componentNameBtn);
	}

	public final StringProperty componentNameBtn2Property() {
		return componentNameBtn2.property();
	}

	private ShadowField<FloatProperty, Number> componentValueUp1 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_UP_1);

	public final float getComponentValueUp1() {
		return componentValueUp1.get().floatValue();
	}

	public final void setComponentValueUp1(float componentValueUp) {
		this.componentValueUp1.set(componentValueUp);
	}

	public final FloatProperty componentValueUp1Property() {
		return componentValueUp1.property();
	}

	private ShadowField<FloatProperty, Number> componentValueUp2 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_UP_2);

	public final float getComponentValueUp2() {
		return componentValueUp2.get().floatValue();
	}

	public final void setComponentValueUp2(float componentValueUp) {
		this.componentValueUp2.set(componentValueUp);
	}

	public final FloatProperty componentValueUp2Property() {
		return componentValueUp2.property();
	}

	private ShadowField<FloatProperty, Number> componentValueDown1 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_DOWN_1);

	public final float getComponentValueDown1() {
		return componentValueDown1.get().floatValue();
	}

	public final void setComponentValueDown1(float componentValueDown) {
		this.componentValueDown1.set(componentValueDown);
	}

	public final FloatProperty componentValueDown1Property() {
		return componentValueDown1.property();
	}

	private ShadowField<FloatProperty, Number> componentValueDown2 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_DOWN_2);

	public final float getComponentValueDown2() {
		return componentValueDown2.get().floatValue();
	}

	public final void setComponentValueDown2(float componentValueDown) {
		this.componentValueDown2.set(componentValueDown);
	}

	public final FloatProperty componentValueDown2Property() {
		return componentValueDown2.property();
	}

	private ShadowField<FloatProperty, Number> componentValueLeft1 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_LEFT_1);

	public final float getComponentValueLeft1() {
		return componentValueLeft1.get().floatValue();
	}

	public final void setComponentValueLeft1(float componentValueLeft) {
		this.componentValueLeft1.set(componentValueLeft);
	}

	public final FloatProperty componentValueLeft1Property() {
		return componentValueLeft1.property();
	}

	private ShadowField<FloatProperty, Number> componentValueLeft2 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_LEFT_2);

	public final float getComponentValueLeft2() {
		return componentValueLeft2.get().floatValue();
	}

	public final void setComponentValueLeft2(float componentValueLeft) {
		this.componentValueLeft2.set(componentValueLeft);
	}

	public final FloatProperty componentValueLeft2Property() {
		return componentValueLeft2.property();
	}

	private ShadowField<FloatProperty, Number> componentValueRight1 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_RIGHT_1);

	public final float getComponentValueRight1() {
		return componentValueRight1.get().floatValue();
	}

	public final void setComponentValueRight1(float componentValueRight) {
		this.componentValueRight1.set(componentValueRight);
	}

	public final FloatProperty componentValueRight1Property() {
		return componentValueRight1.property();
	}

	private ShadowField<FloatProperty, Number> componentValueRight2 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_RIGHT_2);

	public final float getComponentValueRight2() {
		return componentValueRight2.get().floatValue();
	}

	public final void setComponentValueRight2(float componentValueRight) {
		this.componentValueRight2.set(componentValueRight);
	}

	public final FloatProperty componentValueRight2Property() {
		return componentValueRight2.property();
	}

	private ShadowField<FloatProperty, Number> componentValueBtn1 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_BTN_1);

	public final float getComponentValueBtn1() {
		return componentValueBtn1.get().floatValue();
	}

	public final void setComponentValueBtn1(float componentValueBtn) {
		this.componentValueBtn1.set(componentValueBtn);
	}

	public final FloatProperty componentValueBtn1Property() {
		return componentValueBtn1.property();
	}

	private ShadowField<FloatProperty, Number> componentValueBtn2 = new ShadowField<>(
			number -> new SimpleFloatProperty(number.floatValue()), DEFAULT_COMPONENT_VALUE_BTN_2);

	public final float getComponentValueBtn2() {
		return componentValueBtn2.get().floatValue();
	}

	public final void setComponentValueBtn2(float componentValueBtn) {
		this.componentValueBtn2.set(componentValueBtn);
	}

	public final FloatProperty componentValueBtn2Property() {
		return componentValueBtn2.property();
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
