package ui.entities.config;

import javax.persistence.Embeddable;

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

	private String deviceName1;

	public String getDeviceName1() {
		return deviceName1;
	}

	public void setDeviceName1(String deviceName) {
		this.deviceName1 = deviceName;
	}

	private String deviceName2;

	public String getDeviceName2() {
		return deviceName2;
	}

	public void setDeviceName2(String deviceName) {
		this.deviceName2 = deviceName;
	}

	private String componentNameUp1;

	public String getComponentNameUp1() {
		return componentNameUp1;
	}

	public void setComponentNameUp1(String componentNameUp) {
		this.componentNameUp1 = componentNameUp;
	}

	private String componentNameUp2;

	public String getComponentNameUp2() {
		return componentNameUp2;
	}

	public void setComponentNameUp2(String componentNameUp) {
		this.componentNameUp2 = componentNameUp;
	}

	private String componentNameDown1;

	public String getComponentNameDown1() {
		return componentNameDown1;
	}

	public void setComponentNameDown1(String componentNameDown) {
		this.componentNameDown1 = componentNameDown;
	}

	private String componentNameDown2;

	public String getComponentNameDown2() {
		return componentNameDown2;
	}

	public void setComponentNameDown2(String componentNameDown) {
		this.componentNameDown2 = componentNameDown;
	}

	private String componentNameLeft1;

	public String getComponentNameLeft1() {
		return componentNameLeft1;
	}

	public void setComponentNameLeft1(String componentNameLeft) {
		this.componentNameLeft1 = componentNameLeft;
	}

	private String componentNameLeft2;

	public String getComponentNameLeft2() {
		return componentNameLeft2;
	}

	public void setComponentNameLeft2(String componentNameLeft) {
		this.componentNameLeft2 = componentNameLeft;
	}

	private String componentNameRight1;

	public String getComponentNameRight1() {
		return componentNameRight1;
	}

	public void setComponentNameRight1(String componentNameRight) {
		this.componentNameRight1 = componentNameRight;
	}

	private String componentNameRight2;

	public String getComponentNameRight2() {
		return componentNameRight2;
	}

	public void setComponentNameRight2(String componentNameRight) {
		this.componentNameRight2 = componentNameRight;
	}

	private String componentNameBtn1;

	public String getComponentNameBtn1() {
		return componentNameBtn1;
	}

	public void setComponentNameBtn1(String componentNameBtn) {
		this.componentNameBtn1 = componentNameBtn;
	}

	private String componentNameBtn2;

	public String getComponentNameBtn2() {
		return componentNameBtn2;
	}

	public void setComponentNameBtn2(String componentNameBtn) {
		this.componentNameBtn2 = componentNameBtn;
	}

	private float componentValueUp1 = DEFAULT_COMPONENT_VALUE_UP_1;

	public float getComponentValueUp1() {
		return componentValueUp1;
	}

	public void setComponentValueUp1(float componentValueUp) {
		this.componentValueUp1 = componentValueUp;
	}

	private float componentValueUp2 = DEFAULT_COMPONENT_VALUE_UP_2;

	public float getComponentValueUp2() {
		return componentValueUp2;
	}

	public void setComponentValueUp2(float componentValueUp) {
		this.componentValueUp2 = componentValueUp;
	}

	private float componentValueDown1 = DEFAULT_COMPONENT_VALUE_DOWN_1;

	public float getComponentValueDown1() {
		return componentValueDown1;
	}

	public void setComponentValueDown1(float componentValueDown) {
		this.componentValueDown1 = componentValueDown;
	}

	private float componentValueDown2 = DEFAULT_COMPONENT_VALUE_DOWN_2;

	public float getComponentValueDown2() {
		return componentValueDown2;
	}

	public void setComponentValueDown2(float componentValueDown) {
		this.componentValueDown2 = componentValueDown;
	}

	private float componentValueLeft1 = DEFAULT_COMPONENT_VALUE_LEFT_1;

	public float getComponentValueLeft1() {
		return componentValueLeft1;
	}

	public void setComponentValueLeft1(float componentValueLeft) {
		this.componentValueLeft1 = componentValueLeft;
	}

	private float componentValueLeft2 = DEFAULT_COMPONENT_VALUE_LEFT_2;

	public float getComponentValueLeft2() {
		return componentValueLeft2;
	}

	public void setComponentValueLeft2(float componentValueLeft) {
		this.componentValueLeft2 = componentValueLeft;
	}

	private float componentValueRight1 = DEFAULT_COMPONENT_VALUE_RIGHT_1;

	public float getComponentValueRight1() {
		return componentValueRight1;
	}

	public void setComponentValueRight1(float componentValueRight) {
		this.componentValueRight1 = componentValueRight;
	}

	private float componentValueRight2 = DEFAULT_COMPONENT_VALUE_RIGHT_2;

	public float getComponentValueRight2() {
		return componentValueRight2;
	}

	public void setComponentValueRight2(float componentValueRight) {
		this.componentValueRight2 = componentValueRight;
	}

	private float componentValueBtn1 = DEFAULT_COMPONENT_VALUE_BTN_1;

	public float getComponentValueBtn1() {
		return componentValueBtn1;
	}

	public void setComponentValueBtn1(float componentValueBtn) {
		this.componentValueBtn1 = componentValueBtn;
	}

	private float componentValueBtn2 = DEFAULT_COMPONENT_VALUE_BTN_2;

	public float getComponentValueBtn2() {
		return componentValueBtn2;
	}

	public void setComponentValueBtn2(float componentValueBtn) {
		this.componentValueBtn2 = componentValueBtn;
	}

}
