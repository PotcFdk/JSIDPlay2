package sidplay.ini;

import sidplay.ini.intf.IJoystickSection;

/**
 * Joystick section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniJoystickSection extends IniSection implements IJoystickSection {

	protected IniJoystickSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Get name of the device for Joystick.
	 *
	 * @return name of the Joystick device
	 */
	@Override
	public final String getDeviceName1() {
		return iniReader.getPropertyString("Joystick", "Port1Name", null);
	}

	@Override
	public final String getDeviceName2() {
		return iniReader.getPropertyString("Joystick", "Port2Name", null);
	}

	/**
	 * Set name of the device for Joystick.
	 *
	 * @param deviceName
	 *            name of the Joystick device
	 */
	@Override
	public final void setDeviceName1(final String deviceName) {
		iniReader.setProperty("Joystick", "Port1Name", deviceName);
	}

	@Override
	public final void setDeviceName2(final String deviceName) {
		iniReader.setProperty("Joystick", "Port2Name", deviceName);
	}

	/**
	 * Get component name of Joystick direction up.
	 *
	 * @return component name of Joystick direction up
	 */
	@Override
	public final String getComponentNameUp1() {
		return iniReader.getPropertyString("Joystick", "Port1UpName", null);
	}

	@Override
	public final String getComponentNameUp2() {
		return iniReader.getPropertyString("Joystick", "Port2UpName", null);
	}

	/**
	 * Set component name of Joystick direction up.
	 *
	 * @param componentName
	 *            component name of Joystick direction up
	 */
	@Override
	public final void setComponentNameUp1(final String componentName) {
		iniReader.setProperty("Joystick", "Port1UpName", componentName);
	}

	@Override
	public final void setComponentNameUp2(final String componentName) {
		iniReader.setProperty("Joystick", "Port2UpName", componentName);
	}

	/**
	 * Get component name of Joystick direction down.
	 *
	 * @return component name of Joystick direction down
	 */
	@Override
	public final String getComponentNameDown1() {
		return iniReader.getPropertyString("Joystick", "Port1DownName", null);
	}

	@Override
	public final String getComponentNameDown2() {
		return iniReader.getPropertyString("Joystick", "Port2DownName", null);
	}

	/**
	 * Set component name of Joystick direction down.
	 *
	 * @param componentName
	 *            component name of Joystick direction down
	 */
	@Override
	public final void setComponentNameDown1(final String componentName) {
		iniReader.setProperty("Joystick", "Port1DownName", componentName);
	}

	@Override
	public final void setComponentNameDown2(final String componentName) {
		iniReader.setProperty("Joystick", "Port2DownName", componentName);
	}

	/**
	 * Get component name of Joystick direction left.
	 *
	 * @return component name of Joystick direction left
	 */
	@Override
	public final String getComponentNameLeft1() {
		return iniReader.getPropertyString("Joystick", "Port1LeftName", null);
	}

	@Override
	public final String getComponentNameLeft2() {
		return iniReader.getPropertyString("Joystick", "Port2LeftName", null);
	}

	/**
	 * Set component name of Joystick direction left.
	 *
	 * @param componentName
	 *            component name of Joystick direction left
	 */
	@Override
	public final void setComponentNameLeft1(final String componentName) {
		iniReader.setProperty("Joystick", "Port1LeftName", componentName);
	}

	@Override
	public final void setComponentNameLeft2(final String componentName) {
		iniReader.setProperty("Joystick", "Port2LeftName", componentName);
	}

	/**
	 * Get component name of Joystick direction right.
	 *
	 * @return component name of Joystick direction right
	 */
	@Override
	public final String getComponentNameRight1() {
		return iniReader.getPropertyString("Joystick", "Port1RightName", null);
	}

	@Override
	public final String getComponentNameRight2() {
		return iniReader.getPropertyString("Joystick", "Port2RightName", null);
	}

	/**
	 * Set component name of Joystick direction right.
	 *
	 * @param componentName
	 *            component name of Joystick direction right
	 */
	@Override
	public final void setComponentNameRight1(final String componentName) {
		iniReader.setProperty("Joystick", "Port1RightName", componentName);
	}

	@Override
	public final void setComponentNameRight2(final String componentName) {
		iniReader.setProperty("Joystick", "Port2RightName", componentName);
	}

	/**
	 * Get component name of Joystick fire button.
	 *
	 * @return component name of Joystick fire button
	 */
	@Override
	public final String getComponentNameBtn1() {
		return iniReader.getPropertyString("Joystick", "Port1BtnName", null);
	}

	@Override
	public final String getComponentNameBtn2() {
		return iniReader.getPropertyString("Joystick", "Port2BtnName", null);
	}

	/**
	 * Set component name of Joystick fire button.
	 *
	 * @param componentName
	 *            component name of Joystick fire button
	 */
	@Override
	public final void setComponentNameBtn1(final String componentName) {
		iniReader.setProperty("Joystick", "Port1BtnName", componentName);
	}

	@Override
	public final void setComponentNameBtn2(final String componentName) {
		iniReader.setProperty("Joystick", "Port2BtnName", componentName);
	}

	/**
	 * Get component value of Joystick direction up.
	 *
	 * @return component value of Joystick direction up
	 */
	@Override
	public final float getComponentValueUp1() {
		return iniReader.getPropertyFloat("Joystick", "Port1UpValue", 1.0f);
	}

	@Override
	public final float getComponentValueUp2() {
		return iniReader.getPropertyFloat("Joystick", "Port2UpValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction up.
	 *
	 * @param componentValue
	 *            component value of Joystick direction up
	 */
	@Override
	public final void setComponentValueUp1(final float componentValue) {
		iniReader.setProperty("Joystick", "Port1UpValue", componentValue);
	}

	@Override
	public final void setComponentValueUp2(final float componentValue) {
		iniReader.setProperty("Joystick", "Port2UpValue", componentValue);
	}

	/**
	 * Get component value of Joystick direction down.
	 *
	 * @return component value of Joystick direction down
	 */
	@Override
	public final float getComponentValueDown1() {
		return iniReader.getPropertyFloat("Joystick", "Port1DownValue", 1.0f);
	}

	@Override
	public final float getComponentValueDown2() {
		return iniReader.getPropertyFloat("Joystick", "Port2DownValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction down.
	 *
	 * @param componentValue
	 *            component value of Joystick direction down
	 */
	@Override
	public final void setComponentValueDown1(final float componentValue) {
		iniReader.setProperty("Joystick", "Port1DownValue", componentValue);
	}

	@Override
	public final void setComponentValueDown2(final float componentValue) {
		iniReader.setProperty("Joystick", "Port2DownValue", componentValue);
	}

	/**
	 * Get component value of Joystick direction left.
	 *
	 * @return component value of Joystick direction ledt
	 */
	@Override
	public final float getComponentValueLeft1() {
		return iniReader.getPropertyFloat("Joystick", "Port1LeftValue", 1.0f);
	}

	@Override
	public final float getComponentValueLeft2() {
		return iniReader.getPropertyFloat("Joystick", "Port2LeftValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction left.
	 *
	 * @param componentValue
	 *            component value of Joystick direction left
	 */
	@Override
	public final void setComponentValueLeft1(final float componentValue) {
		iniReader.setProperty("Joystick", "Port1LeftValue", componentValue);
	}

	@Override
	public final void setComponentValueLeft2(final float componentValue) {
		iniReader.setProperty("Joystick", "Port2LeftValue", componentValue);
	}

	/**
	 * Get component value of Joystick direction right.
	 *
	 * @return component value of Joystick direction right
	 */
	@Override
	public final float getComponentValueRight1() {
		return iniReader.getPropertyFloat("Joystick", "Port1RightValue", 1.0f);
	}

	@Override
	public final float getComponentValueRight2() {
		return iniReader.getPropertyFloat("Joystick", "Port2RightValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction right.
	 *
	 * @param componentValue
	 *            component value of Joystick direction right
	 */
	@Override
	public final void setComponentValueRight1(final float componentValue) {
		iniReader.setProperty("Joystick", "Port1RightValue", componentValue);
	}

	@Override
	public final void setComponentValueRight2(final float componentValue) {
		iniReader.setProperty("Joystick", "Port2RightValue", componentValue);
	}

	/**
	 * Get component value of Joystick fire button.
	 *
	 * @return component value of Joystick fire button
	 */
	@Override
	public final float getComponentValueBtn1() {
		return iniReader.getPropertyFloat("Joystick", "Port1BtnValue", 1.0f);
	}

	@Override
	public final float getComponentValueBtn2() {
		return iniReader.getPropertyFloat("Joystick", "Port2BtnValue", 1.0f);
	}

	/**
	 * Set component value of Joystick fire button.
	 *
	 * @param componentValue
	 *            component value of Joystick fire button
	 */
	@Override
	public final void setComponentValueBtn1(final float componentValue) {
		iniReader.setProperty("Joystick", "Port1BtnValue", componentValue);
	}

	@Override
	public final void setComponentValueBtn2(final float componentValue) {
		iniReader.setProperty("Joystick", "Port2BtnValue", componentValue);
	}

}
