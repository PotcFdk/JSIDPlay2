package sidplay.ini;

/**
 * Joystick section of the INI file.
 * 
 * @author Ken Händel
 * 
 */
public class IniJoystickSection extends IniSection {

	protected IniJoystickSection(IniReader iniReader) {
		super(iniReader);
	}

	/**
	 * Get name of the device for Joystick.
	 * 
	 * @param port
	 *            port 1..2
	 * @return name of the Joystick device
	 */
	public final String getDeviceName(final int port) {
		return iniReader.getPropertyString("Joystick", "Port" + port + "Name",
				null);
	}

	/**
	 * Set name of the device for Joystick.
	 * 
	 * @param port
	 *            port 1..2
	 * @param deviceName
	 *            name of the Joystick device
	 */
	public final void setDeviceName(final int port, final String deviceName) {
		iniReader.setProperty("Joystick", "Port" + port + "Name", deviceName);
	}

	/**
	 * Get component name of Joystick direction up.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component name of Joystick direction up
	 */
	public final String getComponentNameUp(final int port) {
		return iniReader.getPropertyString("Joystick",
				"Port" + port + "UpName", null);
	}

	/**
	 * Set component name of Joystick direction up.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentName
	 *            component name of Joystick direction up
	 */
	public final void setComponentNameUp(final int port,
			final String componentName) {
//		System.err.println("Set NameUp: " + port + ":"+componentName);
		iniReader.setProperty("Joystick", "Port" + port + "UpName",
				componentName);
	}

	/**
	 * Get component name of Joystick direction down.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component name of Joystick direction down
	 */
	public final String getComponentNameDown(final int port) {
		return iniReader.getPropertyString("Joystick", "Port" + port
				+ "DownName", null);
	}

	/**
	 * Set component name of Joystick direction down.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentName
	 *            component name of Joystick direction down
	 */
	public final void setComponentNameDown(final int port,
			final String componentName) {
//		System.err.println("Set NameDown: " + port + ":"+componentName);
		iniReader.setProperty("Joystick", "Port" + port + "DownName",
				componentName);
	}

	/**
	 * Get component name of Joystick direction left.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component name of Joystick direction ledt
	 */
	public final String getComponentNameLeft(final int port) {
		return iniReader.getPropertyString("Joystick", "Port" + port
				+ "LeftName", null);
	}

	/**
	 * Set component name of Joystick direction left.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentName
	 *            component name of Joystick direction left
	 */
	public final void setComponentNameLeft(final int port,
			final String componentName) {
//		System.err.println("Set NameLeft: " + port + ":"+componentName);
		iniReader.setProperty("Joystick", "Port" + port + "LeftName",
				componentName);
	}

	/**
	 * Get component name of Joystick direction right.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component name of Joystick direction right
	 */
	public final String getComponentNameRight(final int port) {
		return iniReader.getPropertyString("Joystick", "Port" + port
				+ "RightName", null);
	}

	/**
	 * Set component name of Joystick direction right.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentName
	 *            component name of Joystick direction right
	 */
	public final void setComponentNameRight(final int port,
			final String componentName) {
//		System.err.println("Set NameRight: " + port + ":"+componentName);
		iniReader.setProperty("Joystick", "Port" + port + "RightName",
				componentName);
	}

	/**
	 * Get component name of Joystick fire button.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component name of Joystick fire button
	 */
	public final String getComponentNameBtn(final int port) {
		return iniReader.getPropertyString("Joystick", "Port" + port
				+ "BtnName", null);
	}

	/**
	 * Set component name of Joystick fire button.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentName
	 *            component name of Joystick fire button
	 */
	public final void setComponentNameBtn(final int port,
			final String componentName) {
//		System.err.println("Set NameBtn: " + port + ":"+componentName);
		iniReader.setProperty("Joystick", "Port" + port + "BtnName",
				componentName);
	}

	/**
	 * Get component value of Joystick direction up.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component value of Joystick direction up
	 */
	public final float getComponentValueUp(final int port) {
		return iniReader.getPropertyFloat("Joystick", "Port" + port
				+ "UpValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction up.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentValue
	 *            component value of Joystick direction up
	 */
	public final void setComponentValueUp(final int port,
			final float componentValue) {
//		System.err.println("Set ValueUp: " + port + ":"+componentValue);
		iniReader.setProperty("Joystick", "Port" + port + "UpValue",
				componentValue);
	}

	/**
	 * Get component value of Joystick direction down.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component value of Joystick direction down
	 */
	public final float getComponentValueDown(final int port) {
		return iniReader.getPropertyFloat("Joystick", "Port" + port
				+ "DownValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction down.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentValue
	 *            component value of Joystick direction down
	 */
	public final void setComponentValueDown(final int port,
			final float componentValue) {
//		System.err.println("Set ValueDown: " + port + ":"+componentValue);
		iniReader.setProperty("Joystick", "Port" + port + "DownValue",
				componentValue);
	}

	/**
	 * Get component value of Joystick direction left.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component value of Joystick direction ledt
	 */
	public final float getComponentValueLeft(final int port) {
		return iniReader.getPropertyFloat("Joystick", "Port" + port
				+ "LeftValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction left.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentValue
	 *            component value of Joystick direction left
	 */
	public final void setComponentValueLeft(final int port,
			final float componentValue) {
//		System.err.println("Set ValueLeft: " + port + ":"+componentValue);
		iniReader.setProperty("Joystick", "Port" + port + "LeftValue",
				componentValue);
	}

	/**
	 * Get component value of Joystick direction right.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component value of Joystick direction right
	 */
	public final float getComponentValueRight(final int port) {
		return iniReader.getPropertyFloat("Joystick", "Port" + port
				+ "RightValue", 1.0f);
	}

	/**
	 * Set component value of Joystick direction right.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentValue
	 *            component value of Joystick direction right
	 */
	public final void setComponentValueRight(final int port,
			final float componentValue) {
//		System.err.println("Set ValueRight: " + port + ":"+componentValue);
		iniReader.setProperty("Joystick", "Port" + port + "RightValue",
				componentValue);
	}

	/**
	 * Get component value of Joystick fire button.
	 * 
	 * @param port
	 *            port 1..2
	 * @return component value of Joystick fire button
	 */
	public final float getComponentValueBtn(final int port) {
		return iniReader.getPropertyFloat("Joystick", "Port" + port
				+ "BtnValue", 1.0f);
	}

	/**
	 * Set component value of Joystick fire button.
	 * 
	 * @param port
	 *            port 1..2
	 * @param componentValue
	 *            component value of Joystick fire button
	 */
	public final void setComponentValueBtn(final int port,
			final float componentValue) {
//		System.err.println("Set ValueBtn: " + port + ":"+componentValue);
		iniReader.setProperty("Joystick", "Port" + port + "BtnValue",
				componentValue);
	}

}
