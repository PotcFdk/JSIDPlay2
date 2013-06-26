package sidplay.ini.intf;

public interface IJoystickSection {

	/**
	 * Get name of the device for Joystick.
	 *
	 * @return name of the Joystick device
	 */
	public String getDeviceName1();

	public String getDeviceName2();

	/**
	 * Set name of the device for Joystick.
	 *
	 * @param deviceName
	 *            name of the Joystick device
	 */
	public void setDeviceName1(String deviceName);

	public void setDeviceName2(String deviceName);

	/**
	 * Get component name of Joystick direction up.
	 *
	 * @return component name of Joystick direction up
	 */
	public String getComponentNameUp1();

	public String getComponentNameUp2();

	/**
	 * Set component name of Joystick direction up.
	 *
	 * @param componentName
	 *            component name of Joystick direction up
	 */
	public void setComponentNameUp1(String componentName);

	public void setComponentNameUp2(String componentName);

	/**
	 * Get component name of Joystick direction down.
	 *
	 * @return component name of Joystick direction down
	 */
	public String getComponentNameDown1();

	public String getComponentNameDown2();

	/**
	 * Set component name of Joystick direction down.
	 *
	 * @param componentName
	 *            component name of Joystick direction down
	 */
	public void setComponentNameDown1(String componentName);

	public void setComponentNameDown2(String componentName);

	/**
	 * Get component name of Joystick direction left.
	 *
	 * @return component name of Joystick direction ledt
	 */
	public String getComponentNameLeft1();

	public String getComponentNameLeft2();

	/**
	 * Set component name of Joystick direction left.
	 *
	 * @param componentName
	 *            component name of Joystick direction left
	 */
	public void setComponentNameLeft1(String componentName);

	public void setComponentNameLeft2(String componentName);

	/**
	 * Get component name of Joystick direction right.
	 *
	 * @return component name of Joystick direction right
	 */
	public String getComponentNameRight1();

	public String getComponentNameRight2();

	/**
	 * Set component name of Joystick direction right.
	 *
	 * @param componentName
	 *            component name of Joystick direction right
	 */
	public void setComponentNameRight1(String componentName);

	public void setComponentNameRight2(String componentName);

	/**
	 * Get component name of Joystick fire button.
	 *
	 * @return component name of Joystick fire button
	 */
	public String getComponentNameBtn1();

	public String getComponentNameBtn2();

	/**
	 * Set component name of Joystick fire button.
	 *
	 * @param componentName
	 *            component name of Joystick fire button
	 */
	public void setComponentNameBtn1(String componentName);

	public void setComponentNameBtn2(String componentName);

	/**
	 * Get component value of Joystick direction up.
	 *
	 * @return component value of Joystick direction up
	 */
	public float getComponentValueUp1();

	public float getComponentValueUp2();

	/**
	 * Set component value of Joystick direction up.
	 *
	 * @param componentValue
	 *            component value of Joystick direction up
	 */
	public void setComponentValueUp1(float componentValue);

	public void setComponentValueUp2(float componentValue);

	/**
	 * Get component value of Joystick direction down.
	 *
	 * @return component value of Joystick direction down
	 */
	public float getComponentValueDown1();

	public float getComponentValueDown2();

	/**
	 * Set component value of Joystick direction down.
	 *
	 * @param componentValue
	 *            component value of Joystick direction down
	 */
	public void setComponentValueDown1(float componentValue);

	public void setComponentValueDown2(float componentValue);

	/**
	 * Get component value of Joystick direction left.
	 *
	 * @return component value of Joystick direction ledt
	 */
	public float getComponentValueLeft1();

	public float getComponentValueLeft2();

	/**
	 * Set component value of Joystick direction left.
	 *
	 * @param componentValue
	 *            component value of Joystick direction left
	 */
	public void setComponentValueLeft1(float componentValue);

	public void setComponentValueLeft2(float componentValue);

	/**
	 * Get component value of Joystick direction right.
	 *
	 * @return component value of Joystick direction right
	 */
	public float getComponentValueRight1();

	public float getComponentValueRight2();

	/**
	 * Set component value of Joystick direction right.
	 *
	 * @param componentValue
	 *            component value of Joystick direction right
	 */
	public void setComponentValueRight1(float componentValue);

	public void setComponentValueRight2(float componentValue);

	/**
	 * Get component value of Joystick fire button.
	 *
	 * @return component value of Joystick fire button
	 */
	public float getComponentValueBtn1();

	public float getComponentValueBtn2();

	/**
	 * Set component value of Joystick fire button.
	 *
	 * @param componentValue
	 *            component value of Joystick fire button
	 */
	public void setComponentValueBtn1(float componentValue);

	public void setComponentValueBtn2(float componentValue);

}