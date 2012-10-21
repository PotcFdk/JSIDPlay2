package applet.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.TreePath;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.TuneTab;
import applet.events.IUpdateUI;
import applet.events.UIEvent;

public class ConfigView extends TuneTab {

	private SwingEngine swix;
	private JTree configTree;
	protected JPanel parent;
	protected JTextField textField;
	protected JCheckBox checkbox;
	protected JComboBox<Enum<?>> combo;

	private ConfigModel configModel;
	protected ConfigNode configNode;

	public ConfigView(Player player, IConfig config) {
		try {
			swix = new SwingEngine(this);
			swix.insert(ConfigView.class.getResource("Config.xml"), this);
			configModel = (ConfigModel) configTree.getModel();
			configModel.setRootUserObject(swix, config);
			configTree.addTreeSelectionListener(new TreeSelectionListener() {

				@SuppressWarnings({ "rawtypes", "unchecked" })
				@Override
				public void valueChanged(TreeSelectionEvent event) {
					final TreePath treePath = event.getNewLeadSelectionPath();
					if (treePath == null) {
						return;
					}
					final Object pathComponent = treePath
							.getLastPathComponent();
					if (pathComponent instanceof ConfigNode) {
						configNode = (ConfigNode) pathComponent;
						try {
							if (configNode.getUserObject() instanceof Field) {
								parent.removeAll();
								Field field = (Field) configNode
										.getUserObject();
								if (field.getType() == String.class
										|| (field.getType() == Integer.class || field
												.getType() == int.class)
										|| (field.getType() == Float.class || field
												.getType() == float.class)
										|| (field.getType() == Character.class || field
												.getType() == char.class)) {
									parent.add(swix.render(ConfigView.class
											.getResource(getUITypeName(field)
													+ ".xml")),
											BorderLayout.NORTH);
									String value;
									if (configNode.getValue() != null) {
										value = configNode.getValue()
												.toString();
									} else {
										value = "";
									}
									textField.setText(value);
								} else if (field.getType() == Boolean.class
										|| field.getType() == boolean.class) {
									parent.add(swix.render(ConfigView.class
											.getResource(getUITypeName(field)
													+ ".xml")),
											BorderLayout.NORTH);
									String value;
									if (configNode.getValue() != null) {
										value = configNode.getValue()
												.toString();
									} else {
										value = "false";
									}
									checkbox.setSelected(Boolean.valueOf(value));
								} else if (Enum.class.isAssignableFrom(field
										.getType())) {
									parent.add(swix.render(ConfigView.class
											.getResource(getUITypeName(field)
													+ ".xml")),
											BorderLayout.NORTH);
									Class<? extends Enum> en = (Class<? extends Enum>) field
											.getType();
									ActionListener[] actionListeners = combo
											.getActionListeners();
									for (ActionListener actionListener : actionListeners) {
										combo.removeActionListener(actionListener);
									}
									combo.addItem(null);
									for (Enum val : en.getEnumConstants()) {
										combo.addItem(val);
									}
									Enum value;
									if (configNode.getValue() != null) {
										value = (Enum) configNode.getValue();
									} else {
										value = null;
									}
									combo.setSelectedItem(value);
									for (ActionListener actionListener : actionListeners) {
										combo.addActionListener(actionListener);
									}
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					parent.repaint();
				}

				private String getUITypeName(Field field) {
					if (field.getType() == String.class) {
						return String.class.getSimpleName();
					} else if (field.getType() == Integer.class
							|| field.getType() == int.class) {
						return Integer.class.getSimpleName();
					} else if (field.getType() == Boolean.class
							|| field.getType() == boolean.class) {
						return Boolean.class.getSimpleName();
					} else if (Enum.class.isAssignableFrom(field.getType())) {
						return Enum.class.getSimpleName();
					} else if (field.getType() == Float.class
							|| field.getType() == float.class) {
						return Float.class.getSimpleName();
					} else if (field.getType() == Character.class
							|| field.getType() == char.class) {
						return Character.class.getSimpleName();
					} else {
						return null;
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Action doSetValue = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (configNode.getUserObject() instanceof Field) {
				String value;
				if (textField.getText().equals("")) {
					value = null;
				} else {
					value = textField.getText();
				}
				Field field = (Field) configNode.getUserObject();
				if (field.getType() == String.class) {
					configNode.setValue(value);
				} else if (field.getType() == Integer.class
						|| field.getType() == int.class) {
					configNode.setValue(Integer.valueOf(value).intValue());
				} else if (field.getType() == Float.class
						|| field.getType() == float.class) {
					configNode.setValue(Float.valueOf(value).floatValue());
				} else if (field.getType() == Character.class
						|| field.getType() == char.class) {
					char ch;
					if (value == null) {
						ch = (char) 0;
					} else {
						ch = value.charAt(0);
					}
					configNode.setValue(Character.valueOf(ch).charValue());
				}
			}
			configModel.reload();
		}
	};

	public Action doSetBoolean = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (configNode.getUserObject() instanceof Field) {
				Boolean value;
				if (checkbox.isSelected()) {
					value = true;
				} else {
					value = false;
				}
				Field field = (Field) configNode.getUserObject();
				if (field.getType() == Boolean.class
						|| field.getType() == boolean.class) {
					configNode.setValue(Boolean.valueOf(value).booleanValue());
				}
			}
			configModel.reload();
		}
	};

	public Action doSetEnum = new AbstractAction() {

		@SuppressWarnings("rawtypes")
		@Override
		public void actionPerformed(ActionEvent e) {
			if (configNode.getUserObject() instanceof Field) {
				Enum value;
				if (combo.getSelectedItem() != null) {
					value = (Enum) combo.getSelectedItem();
				} else {
					value = null;
				}
				Field field = (Field) configNode.getUserObject();
				if (Enum.class.isAssignableFrom(field.getType())) {
					configNode.setValue(value);
				}
			}
			configModel.reload();
		}
	};

	@Override
	public void notify(UIEvent evt) {
		if (!evt.isOfType(IUpdateUI.class)) {
			if (configModel != null) {
				configModel.reload();
			}
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
		// TODO Auto-generated method stub

	}

}