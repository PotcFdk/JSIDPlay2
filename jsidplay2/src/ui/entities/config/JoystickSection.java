package ui.entities.config;

import javax.persistence.Embeddable;

import sidplay.ini.intf.IJoystickSection;

@Embeddable
public class JoystickSection implements IJoystickSection {

	private String deviceName1;

	@Override
	public String getDeviceName1() {
		return deviceName1;
	}

	@Override
	public void setDeviceName1(String deviceName) {
		this.deviceName1 = deviceName;
	}

	private String deviceName2;

	@Override
	public String getDeviceName2() {
		return deviceName2;
	}

	@Override
	public void setDeviceName2(String deviceName) {
		this.deviceName2 = deviceName;
	}

	private String componentNameUp1;

	@Override
	public String getComponentNameUp1() {
		return componentNameUp1;
	}

	@Override
	public void setComponentNameUp1(String componentNameUp) {
		this.componentNameUp1 = componentNameUp;
	}

	private String componentNameUp2;

	@Override
	public String getComponentNameUp2() {
		return componentNameUp2;
	}

	@Override
	public void setComponentNameUp2(String componentNameUp) {
		this.componentNameUp2 = componentNameUp;
	}

	private String componentNameDown1;

	@Override
	public String getComponentNameDown1() {
		return componentNameDown1;
	}

	@Override
	public void setComponentNameDown1(String componentNameDown) {
		this.componentNameDown1 = componentNameDown;
	}

	private String componentNameDown2;

	@Override
	public String getComponentNameDown2() {
		return componentNameDown2;
	}

	@Override
	public void setComponentNameDown2(String componentNameDown) {
		this.componentNameDown2 = componentNameDown;
	}

	private String componentNameLeft1;

	@Override
	public String getComponentNameLeft1() {
		return componentNameLeft1;
	}

	@Override
	public void setComponentNameLeft1(String componentNameLeft) {
		this.componentNameLeft1 = componentNameLeft;
	}

	private String componentNameLeft2;

	@Override
	public String getComponentNameLeft2() {
		return componentNameLeft2;
	}

	@Override
	public void setComponentNameLeft2(String componentNameLeft) {
		this.componentNameLeft2 = componentNameLeft;
	}

	private String componentNameRight1;

	@Override
	public String getComponentNameRight1() {
		return componentNameRight1;
	}

	@Override
	public void setComponentNameRight1(String componentNameRight) {
		this.componentNameRight1 = componentNameRight;
	}

	private String componentNameRight2;

	@Override
	public String getComponentNameRight2() {
		return componentNameRight2;
	}

	@Override
	public void setComponentNameRight2(String componentNameRight) {
		this.componentNameRight2 = componentNameRight;
	}

	private String componentNameBtn1;

	@Override
	public String getComponentNameBtn1() {
		return componentNameBtn1;
	}

	@Override
	public void setComponentNameBtn1(String componentNameBtn) {
		this.componentNameBtn1 = componentNameBtn;
	}

	private String componentNameBtn2;

	@Override
	public String getComponentNameBtn2() {
		return componentNameBtn2;
	}

	@Override
	public void setComponentNameBtn2(String componentNameBtn) {
		this.componentNameBtn2 = componentNameBtn;
	}

	private float componentValueUp1;

	@Override
	public float getComponentValueUp1() {
		return componentValueUp1;
	}

	@Override
	public void setComponentValueUp1(float componentValueUp) {
		this.componentValueUp1 = componentValueUp;
	}

	private float componentValueUp2;

	@Override
	public float getComponentValueUp2() {
		return componentValueUp2;
	}

	@Override
	public void setComponentValueUp2(float componentValueUp) {
		this.componentValueUp2 = componentValueUp;
	}

	private float componentValueDown1;

	@Override
	public float getComponentValueDown1() {
		return componentValueDown1;
	}

	@Override
	public void setComponentValueDown1(float componentValueDown) {
		this.componentValueDown1 = componentValueDown;
	}

	private float componentValueDown2;

	@Override
	public float getComponentValueDown2() {
		return componentValueDown2;
	}

	@Override
	public void setComponentValueDown2(float componentValueDown) {
		this.componentValueDown2 = componentValueDown;
	}

	private float componentValueLeft1;

	@Override
	public float getComponentValueLeft1() {
		return componentValueLeft1;
	}

	@Override
	public void setComponentValueLeft1(float componentValueLeft) {
		this.componentValueLeft1 = componentValueLeft;
	}

	private float componentValueLeft2;

	@Override
	public float getComponentValueLeft2() {
		return componentValueLeft2;
	}

	@Override
	public void setComponentValueLeft2(float componentValueLeft) {
		this.componentValueLeft2 = componentValueLeft;
	}

	private float componentValueRight1;

	@Override
	public float getComponentValueRight1() {
		return componentValueRight1;
	}

	@Override
	public void setComponentValueRight1(float componentValueRight) {
		this.componentValueRight1 = componentValueRight;
	}

	private float componentValueRight2;

	@Override
	public float getComponentValueRight2() {
		return componentValueRight2;
	}

	@Override
	public void setComponentValueRight2(float componentValueRight) {
		this.componentValueRight2 = componentValueRight;
	}

	private float componentValueBtn1;

	@Override
	public float getComponentValueBtn1() {
		return componentValueBtn1;
	}

	@Override
	public void setComponentValueBtn1(float componentValueBtn) {
		this.componentValueBtn1 = componentValueBtn;
	}

	private float componentValueBtn2;

	@Override
	public float getComponentValueBtn2() {
		return componentValueBtn2;
	}

	@Override
	public void setComponentValueBtn2(float componentValueBtn) {
		this.componentValueBtn2 = componentValueBtn;
	}

}
