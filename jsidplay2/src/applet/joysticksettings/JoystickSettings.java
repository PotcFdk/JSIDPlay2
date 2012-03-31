package applet.joysticksettings;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import libsidplay.Player;
import libsidplay.components.joystick.IJoystick;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import org.swixml.SwingEngine;
import org.swixml.XDialog;

import sidplay.ini.IniConfig;
import applet.events.IUpdateUI;
import applet.events.UIEvent;
import applet.events.UIEventFactory;
import applet.events.UIEventListener;

public class JoystickSettings extends XDialog implements UIEventListener {

	private SwingEngine swix;

	protected JCheckBox activateJoy1, activateJoy2;
	protected JComboBox device1, device2;
	protected JComboBox up1, down1, left1, right1, fire1;
	protected JTextField up1Value, down1Value, left1Value, right1Value,
			fire1Value;
	protected JComboBox up2, down2, left2, right2, fire2;
	protected JTextField up2Value, down2Value, left2Value, right2Value,
			fire2Value;
	protected JTable testTable1, testTable2;

	protected Player player;
	protected IniConfig config;
	protected UIEventFactory uiEvents = UIEventFactory.getInstance();
	
	protected static ControllerEnvironment fControllerEnv = ControllerEnvironment
			.getDefaultEnvironment();
	protected Controller controller1, controller2;
	protected Component dirUp1, dirDown1, dirLeft1, dirRight1, dirFire1;
	protected Component dirUp2, dirDown2, dirLeft2, dirRight2, dirFire2;
	protected float dirUp1Value, dirDown1Value, dirLeft1Value, dirRight1Value,
			dirFire1Value;
	protected float dirUp2Value, dirDown2Value, dirLeft2Value, dirRight2Value,
			dirFire2Value;

	// Joystick port 1

	public Action doActivateJoy1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (activateJoy1.isSelected()) {
				player.getC64().setJoystick(0, getJoystickReader1());
			} else {
				player.getC64().setJoystick(0, null);
			}
		}
	};

	public Action chooseDevice1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = device1.getSelectedIndex();
			if (index >= 1) {
				setController1(fControllerEnv.getControllers()[index - 1]);

				config.joystick().setDeviceName(1, controller1.getName());
				((DefaultTableModel) testTable1.getModel())
						.fireTableStructureChanged();
			}
		}
	};

	public Action chooseUp1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = up1.getSelectedIndex();
			if (index >= 0) {
				dirUp1 = controller1.getComponents()[index];
				config.joystick().setComponentNameUp(1, dirUp1.getName());
			}
		}
	};

	public Action chooseUp1Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(up1Value.getText());
			dirUp1Value = value;
			config.joystick().setComponentValueUp(1, value);
		}
	};

	public Action chooseDown1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = down1.getSelectedIndex();
			if (index >= 0) {
				dirDown1 = controller1.getComponents()[index];
				config.joystick().setComponentNameDown(1, dirDown1.getName());
			}
		}
	};

	public Action chooseDown1Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(down1Value.getText());
			dirDown1Value = value;
			config.joystick().setComponentValueDown(1, value);
		}
	};

	public Action chooseLeft1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = left1.getSelectedIndex();
			if (index >= 0) {
				dirLeft1 = controller1.getComponents()[index];
				config.joystick().setComponentNameLeft(1, dirLeft1.getName());
			}
		}
	};

	public Action chooseLeft1Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(left1Value.getText());
			dirLeft1Value = value;
			config.joystick().setComponentValueLeft(1, value);
		}
	};

	public Action chooseRight1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = right1.getSelectedIndex();
			if (index >= 0) {
				dirRight1 = controller1.getComponents()[index];
				config.joystick().setComponentNameRight(1, dirRight1.getName());
			}
		}
	};

	public Action chooseRight1Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(right1Value.getText());
			dirRight1Value = value;
			config.joystick().setComponentValueRight(1, value);
		}
	};

	public Action chooseFire1 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = fire1.getSelectedIndex();
			if (index >= 0) {
				dirFire1 = controller1.getComponents()[index];
				config.joystick().setComponentNameBtn(1, dirFire1.getName());
			}
		}
	};

	public Action chooseFire1Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(fire1Value.getText());
			dirFire1Value = value;
			config.joystick().setComponentValueBtn(1, value);
		}
	};

	// Joystick port 2

	public Action doActivateJoy2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (activateJoy2.isSelected()) {
				player.getC64().setJoystick(1, getJoystickReader2());
			} else {
				player.getC64().setJoystick(1, null);
			}
		}
	};

	public Action chooseDevice2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = device2.getSelectedIndex();
			if (index >= 1) {
				setController2(fControllerEnv.getControllers()[index - 1]);

				config.joystick().setDeviceName(2, controller2.getName());
				((DefaultTableModel) testTable2.getModel())
						.fireTableStructureChanged();
			}
		}
	};

	public Action chooseUp2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = up2.getSelectedIndex();
			if (index >= 0) {
				dirUp2 = controller2.getComponents()[index];
				config.joystick().setComponentNameUp(2, dirUp2.getName());
			}
		}
	};

	public Action chooseUp2Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(up2Value.getText());
			dirUp2Value = value;
			config.joystick().setComponentValueUp(2, value);
		}
	};

	public Action chooseDown2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = down2.getSelectedIndex();
			if (index >= 0) {
				dirDown2 = controller2.getComponents()[index];
				config.joystick().setComponentNameDown(2, dirDown2.getName());
			}
		}
	};

	public Action chooseDown2Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(down2Value.getText());
			dirDown2Value = value;
			config.joystick().setComponentValueDown(2, value);
		}
	};

	public Action chooseLeft2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = left2.getSelectedIndex();
			if (index >= 0) {
				dirLeft2 = controller2.getComponents()[index];
				config.joystick().setComponentNameLeft(2, dirLeft2.getName());
			}
		}
	};

	public Action chooseLeft2Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(left2Value.getText());
			dirLeft2Value = value;
			config.joystick().setComponentValueLeft(2, value);
		}
	};

	public Action chooseRight2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = right2.getSelectedIndex();
			if (index >= 0) {
				dirRight2 = controller2.getComponents()[index];
				config.joystick().setComponentNameRight(2, dirRight2.getName());
			}
		}
	};

	public Action chooseRight2Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(right2Value.getText());
			dirRight2Value = value;
			config.joystick().setComponentValueRight(2, value);
		}
	};

	public Action chooseFire2 = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final int index = fire2.getSelectedIndex();
			if (index >= 0) {
				dirFire2 = controller2.getComponents()[index];
				config.joystick().setComponentNameBtn(2, dirFire2.getName());
			}
		}
	};

	public Action chooseFire2Value = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			final float value = Float.valueOf(fire2Value.getText());
			dirFire2Value = value;
			config.joystick().setComponentValueBtn(2, value);
		}
	};

	public JoystickSettings(Player pl, IniConfig cfg) {
		this.player = pl;
		this.config = cfg;
		uiEvents.addListener(this);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				uiEvents.removeListener(JoystickSettings.this);
			}
		});
		try {
			swix = new SwingEngine(this);
			swix.insert(
					JoystickSettings.class.getResource("JoystickSettings.xml"),
					this);

			fillComboBoxes();
			setDefaultsAndActions();

			Dimension d = getToolkit().getScreenSize();
			Dimension s = getSize();
			setLocation(new Point((d.width - s.width) / 2,
					(d.height - s.height) / 2));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void setDefaultsAndActions() {
		((JoystickTestModel) testTable1.getModel()).setLocalizer(swix
				.getLocalizer());
		((JoystickTestModel) testTable2.getModel()).setLocalizer(swix
				.getLocalizer());

		// Joystick Port 1

		activateJoy1.setSelected(player.getC64().isJoystickConnected(0));
		{
			device1.removeActionListener(chooseDevice1);
			up1.removeActionListener(chooseUp1);
			down1.removeActionListener(chooseDown1);
			left1.removeActionListener(chooseLeft1);
			right1.removeActionListener(chooseRight1);
			fire1.removeActionListener(chooseFire1);
			String deviceName = config.joystick().getDeviceName(1);
			if (deviceName != null) {
				device1.setSelectedItem(deviceName);
				for (int i = 0; i < fControllerEnv.getControllers().length; i++) {
					if (fControllerEnv.getControllers()[i].getName().equals(
							deviceName)) {
						setController1(fControllerEnv.getControllers()[i]);
						break;
					}
				}
			}
			device1.addActionListener(chooseDevice1);
			up1.addActionListener(chooseUp1);
			down1.addActionListener(chooseDown1);
			left1.addActionListener(chooseLeft1);
			right1.addActionListener(chooseRight1);
			fire1.addActionListener(chooseFire1);
		}
		{
			String componentName = config.joystick().getComponentNameUp(1);
			if (componentName != null) {
				up1.setSelectedItem(componentName);
				for (int i = 0; controller1 != null
						&& i < controller1.getComponents().length; i++) {
					if (controller1.getComponents()[i].getName().equals(
							componentName)) {
						dirUp1 = controller1.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameDown(1);
			if (componentName != null) {
				down1.setSelectedItem(componentName);
				for (int i = 0; controller1 != null
						&& i < controller1.getComponents().length; i++) {
					if (controller1.getComponents()[i].getName().equals(
							componentName)) {
						dirDown1 = controller1.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameLeft(1);
			if (componentName != null) {
				left1.setSelectedItem(componentName);
				for (int i = 0; controller1 != null
						&& i < controller1.getComponents().length; i++) {
					if (controller1.getComponents()[i].getName().equals(
							componentName)) {
						dirLeft1 = controller1.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameRight(1);
			if (componentName != null) {
				right1.setSelectedItem(componentName);
				for (int i = 0; controller1 != null
						&& i < controller1.getComponents().length; i++) {
					if (controller1.getComponents()[i].getName().equals(
							componentName)) {
						dirRight1 = controller1.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameBtn(1);
			if (componentName != null) {
				fire1.setSelectedItem(componentName);
				for (int i = 0; controller1 != null
						&& i < controller1.getComponents().length; i++) {
					if (controller1.getComponents()[i].getName().equals(
							componentName)) {
						dirFire1 = controller1.getComponents()[i];
						break;
					}
				}
			}
		}

		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueUp(1));
			dirUp1Value = value;
			up1Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueDown(1));
			dirDown1Value = value;
			down1Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueLeft(1));
			dirLeft1Value = value;
			left1Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueRight(1));
			dirRight1Value = value;
			right1Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueBtn(1));
			dirFire1Value = value;
			fire1Value.setText(String.valueOf(value));
		}

		// Joystick Port 2

		{
			activateJoy2.setSelected(player.getC64().isJoystickConnected(1));
		}
		{
			device2.removeActionListener(chooseDevice2);
			up2.removeActionListener(chooseUp2);
			down2.removeActionListener(chooseDown2);
			left2.removeActionListener(chooseLeft2);
			right2.removeActionListener(chooseRight2);
			fire2.removeActionListener(chooseFire2);
			String deviceName = config.joystick().getDeviceName(2);
			if (deviceName != null) {
				device2.setSelectedItem(deviceName);
				for (int i = 0; i < fControllerEnv.getControllers().length; i++) {
					if (fControllerEnv.getControllers()[i].getName().equals(
							deviceName)) {
						setController2(fControllerEnv.getControllers()[i]);
						break;
					}
				}
			}
			device2.addActionListener(chooseDevice2);
			up2.addActionListener(chooseUp2);
			down2.addActionListener(chooseDown2);
			left2.addActionListener(chooseLeft2);
			right2.addActionListener(chooseRight2);
			fire2.addActionListener(chooseFire2);
		}
		{
			String componentName = config.joystick().getComponentNameUp(2);
			if (componentName != null) {
				up2.setSelectedItem(componentName);
				for (int i = 0; controller2 != null
						&& i < controller2.getComponents().length; i++) {
					if (controller2.getComponents()[i].getName().equals(
							componentName)) {
						dirUp2 = controller2.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameDown(2);
			if (componentName != null) {
				down2.setSelectedItem(componentName);
				for (int i = 0; controller2 != null
						&& i < controller2.getComponents().length; i++) {
					if (controller2.getComponents()[i].getName().equals(
							componentName)) {
						dirDown2 = controller2.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameLeft(2);
			if (componentName != null) {
				left2.setSelectedItem(componentName);
				for (int i = 0; controller2 != null
						&& i < controller2.getComponents().length; i++) {
					if (controller2.getComponents()[i].getName().equals(
							componentName)) {
						dirLeft2 = controller2.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameRight(2);
			if (componentName != null) {
				right2.setSelectedItem(componentName);
				for (int i = 0; controller2 != null
						&& i < controller2.getComponents().length; i++) {
					if (controller2.getComponents()[i].getName().equals(
							componentName)) {
						dirRight2 = controller2.getComponents()[i];
						break;
					}
				}
			}
		}
		{
			String componentName = config.joystick().getComponentNameBtn(2);
			if (componentName != null) {
				fire2.setSelectedItem(componentName);
				for (int i = 0; controller2 != null
						&& i < controller2.getComponents().length; i++) {
					if (controller2.getComponents()[i].getName().equals(
							componentName)) {
						dirFire2 = controller2.getComponents()[i];
						break;
					}
				}
			}
		}

		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueUp(2));
			dirUp2Value = value;
			up2Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueDown(2));
			dirDown2Value = value;
			down2Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueLeft(2));
			dirLeft2Value = value;
			left2Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueRight(2));
			dirRight2Value = value;
			right2Value.setText(String.valueOf(value));
		}
		{
			final float value = Float.valueOf(config.joystick()
					.getComponentValueBtn(2));
			dirFire2Value = value;
			fire2Value.setText(String.valueOf(value));
		}
	}

	protected void setController1(Controller controller) {
		controller1 = controller;
		up1.removeAllItems();
		down1.removeAllItems();
		left1.removeAllItems();
		right1.removeAllItems();
		fire1.removeAllItems();
		for (final Component c : controller1.getComponents()) {
			final String componentName = c.getName();
			up1.addItem(componentName);
			down1.addItem(componentName);
			left1.addItem(componentName);
			right1.addItem(componentName);
			fire1.addItem(componentName);
		}
		((JoystickTestModel) testTable1.getModel()).setInput(controller1);
		testTable1.repaint();

	}

	protected void setController2(Controller controller) {
		controller2 = controller;
		up2.removeAllItems();
		down2.removeAllItems();
		left2.removeAllItems();
		right2.removeAllItems();
		fire2.removeAllItems();
		for (final Component c : controller2.getComponents()) {
			final String componentName = c.getName();
			up2.addItem(componentName);
			down2.addItem(componentName);
			left2.addItem(componentName);
			right2.addItem(componentName);
			fire2.addItem(componentName);
		}
		((JoystickTestModel) testTable2.getModel()).setInput(controller2);
		testTable2.repaint();
	}

	private void fillComboBoxes() {
		if (fControllerEnv.isSupported()) {
			device1.addItem("");
			device2.addItem("");
			for (final Controller c : fControllerEnv.getControllers()) {
				device1.addItem(c.getName());
				device2.addItem(c.getName());
			}
		}
	}

	long oldRenderTime;

	public void notify(final UIEvent evt) {
		if (evt.isOfType(IUpdateUI.class)) {
			final long currentTime = System.currentTimeMillis();
			if (currentTime - oldRenderTime > 500) {
				oldRenderTime = currentTime;
				testTable1.repaint();
				testTable2.repaint();
			}
		}
	}
	
	/**
	 * Implementation of Joystick in port 1. Connects selected controller values
	 * with the emulation core.
	 * 
	 * @return joystick reader port 1
	 */
	protected IJoystick getJoystickReader1() {
		return new IJoystick() {
			private long lastPollingTime;
			private byte bits;

			public byte getValue() {
				if (controller1 == null) {
					return (byte) 0xff;
				}
				/* throttle polling to max. once every 5 ms */
				final long currentTime = System.currentTimeMillis();
				if (currentTime > lastPollingTime + 5) {
					controller1.poll();
					lastPollingTime = currentTime;

					bits = (byte) 0xff;
					if (dirUp1 != null
							&& Math.abs(dirUp1.getPollData() - dirUp1Value) < 0.1) {
						bits ^= 1;
					}
					if (dirDown1 != null
							&& Math.abs(dirDown1.getPollData() - dirDown1Value) < 0.1) {
						bits ^= 2;
					}
					if (dirLeft1 != null
							&& Math.abs(dirLeft1.getPollData() - dirLeft1Value) < 0.1) {
						bits ^= 4;
					}
					if (dirRight1 != null
							&& Math.abs(dirRight1.getPollData()
									- dirRight1Value) < 0.1) {
						bits ^= 8;
					}
					if (dirFire1 != null
							&& Math.abs(dirFire1.getPollData() - dirFire1Value) < 0.1) {
						bits ^= 16;
					}
				}

				return bits;
			}
		};
	}

	/**
	 * Implementation of Joystick in port 2.Connects selected controller values
	 * with the emulation core.
	 * 
	 * @return joystick reader port 2
	 */
	protected IJoystick getJoystickReader2() {
		return new IJoystick() {
			private long lastPollingTime;
			private byte bits;

			public byte getValue() {
				if (controller2 == null) {
					return (byte) 0xff;
				}
				/* throttle polling to max. once every 5 ms */
				final long currentTime = System.currentTimeMillis();
				if (currentTime > lastPollingTime + 5) {
					controller2.poll();
					lastPollingTime = currentTime;

					bits = (byte) 0xff;
					if (dirUp2 != null
							&& Math.abs(dirUp2.getPollData() - dirUp2Value) < 0.1) {
						bits ^= 1;
					}
					if (dirDown2 != null
							&& Math.abs(dirDown2.getPollData() - dirDown2Value) < 0.1) {
						bits ^= 2;
					}
					if (dirLeft2 != null
							&& Math.abs(dirLeft2.getPollData() - dirLeft2Value) < 0.1) {
						bits ^= 4;
					}
					if (dirRight2 != null
							&& Math.abs(dirRight2.getPollData()
									- dirRight2Value) < 0.1) {
						bits ^= 8;
					}
					if (dirFire2 != null
							&& Math.abs(dirFire2.getPollData() - dirFire2Value) < 0.1) {
						bits ^= 16;
					}
				}

				return bits;
			}
		};
	}

}
