package applet.config;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.TuneTab;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigField;
import applet.config.editors.CharTextField;
import applet.config.editors.FloatTextField;
import applet.config.editors.IntTextField;
import applet.entities.config.Configuration;
import applet.events.IUpdateUI;
import applet.events.UIEvent;
import applet.filefilter.ConfigFileFilter;

public class ConfigView extends TuneTab {

	private SwingEngine swix;

	private JTree configTree;
	protected Box parent;
	protected JTextField textField;
	protected JCheckBox checkbox;
	protected JComboBox<Enum<?>> combo;
	protected JTextArea description;

	private IConfig config;
	protected File lastDir;
	protected FileFilter configFilter = new ConfigFileFilter();

	private ConfigModel configModel;
	protected ConfigNode configNode;

	private int refreshCounter = 0;

	protected int fileChooserFilter;

	public ConfigView(EntityManager em, Player player, IConfig config) {
		this.config = config;
		try {
			swix = new SwingEngine(this);
			swix.getTaglib().registerTag("inttextfield", IntTextField.class);
			swix.getTaglib()
					.registerTag("floattextfield", FloatTextField.class);
			swix.getTaglib().registerTag("chartextfield", CharTextField.class);
			swix.insert(ConfigView.class.getResource("Config.xml"), this);
			configModel = (ConfigModel) configTree.getModel();
			configModel.setRootUserObject(swix.getLocalizer(), config);
			configTree.addTreeSelectionListener(new TreeSelectionListener() {

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
							removeOldEditor();
							TreeNode parentConfigNode = configNode.getParent();
							if (configNode.getUserObject() instanceof String) {
								String uiTypeName = getUITypeName(configNode
										.getUserObject().getClass());
								createEditor(parentConfigNode, null, uiTypeName);
								textField.setText(configNode.getUserObject()
										.toString());
								textField.setEditable(false);
							} else if (configNode.getUserObject() instanceof Field) {
								Field field = (Field) configNode
										.getUserObject();
								JComponent component = null;
								if (isTextFieldType(field)) {
									ConfigField uiConfig = field
											.getAnnotation(ConfigField.class);
									String uiTypeName;
									if (uiConfig != null
											&& uiConfig.uiClass() != null) {
										fileChooserFilter = uiConfig.filter();
										uiTypeName = getUITypeName(uiConfig
												.uiClass());
									} else {
										uiTypeName = getUITypeName(field
												.getType());
									}
									createEditor(parentConfigNode,
											field.getName(), uiTypeName);
									component = initTextField();
								} else if (isCheckBoxType(field)) {
									String uiTypeName = getUITypeName(field
											.getType());
									createEditor(parentConfigNode,
											field.getName(), uiTypeName);
									component = initCheckBox();
								} else if (isEnumType(field)) {
									String uiTypeName = getUITypeName(field
											.getType());
									createEditor(parentConfigNode,
											field.getName(), uiTypeName);
									component = initEnumComboBox(field);
								}
								ConfigDescription uiDesc = field
										.getAnnotation(ConfigDescription.class);
								if (uiDesc != null) {
									description.setText(swix.getLocalizer()
											.getString(uiDesc.descriptionKey()));
									if (component != null) {
										component.setToolTipText(swix
												.getLocalizer().getString(
														uiDesc.toolTipKey()));
									}
								}
							} else {
								createEditor(parentConfigNode,
										String.valueOf(configNode),
										"NoParameter");
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					parent.repaint();
				}

				private void removeOldEditor() {
					if (parent.getComponentCount() > 1
							&& parent.getComponent(0) != null) {
						JComponent oldEditor = (JComponent) parent
								.getComponent(0);
						parent.remove(oldEditor);
						oldEditor.setToolTipText(swix.getLocalizer().getString(
								"NO_TOOLTIP"));
					}
					description.setText(swix.getLocalizer()
							.getString("NO_DESC"));
				}

				private boolean isEnumType(Field field) {
					return Enum.class.isAssignableFrom(field.getType());
				}

				private boolean isCheckBoxType(Field field) {
					return field.getType() == Boolean.class
							|| field.getType() == boolean.class;
				}

				private boolean isTextFieldType(Field field) {
					return field.getType() == String.class
							|| (field.getType() == Integer.class || field
									.getType() == int.class)
							|| (field.getType() == Float.class || field
									.getType() == float.class)
							|| (field.getType() == Character.class || field
									.getType() == char.class);
				}

				private JComponent initTextField() {
					textField.setText(configNode.getValue() != null ? configNode
							.getValue().toString() : "");
					return textField;
				}

				private JComponent initCheckBox() {
					checkbox.setSelected(configNode.getValue() != null ? Boolean
							.valueOf(configNode.getValue().toString()) : false);
					return checkbox;
				}

				@SuppressWarnings({ "rawtypes", "unchecked" })
				private JComponent initEnumComboBox(Field field) {
					ActionListener[] actionListeners = combo
							.getActionListeners();
					for (ActionListener actionListener : actionListeners) {
						combo.removeActionListener(actionListener);
					}
					combo.addItem(null);
					Class<? extends Enum> en = (Class<? extends Enum>) field
							.getType();
					for (Enum val : en.getEnumConstants()) {
						combo.addItem(val);
					}
					combo.setSelectedItem(configNode.getValue() != null ? (Enum) configNode
							.getValue() : null);
					for (ActionListener actionListener : actionListeners) {
						combo.addActionListener(actionListener);
					}
					return combo;
				}

				private void createEditor(TreeNode parentConfigNode,
						String fieldName, String uiTypeName) throws Exception {
					String category = parentConfigNode != null ? parentConfigNode
							.toString() : "";
					parent.setBorder(new TitledBorder(category));
					JComponent editor = (JComponent) swix
							.render(ConfigView.class.getResource("editors/"
									+ uiTypeName + ".xml"));
					editor.setBorder(new TitledBorder(fieldName));
					parent.add(editor, 0);
				}

				private String getUITypeName(Class<?> fieldType) {
					if (fieldType == String.class) {
						return String.class.getSimpleName();
					} else if (fieldType == Integer.class
							|| fieldType == int.class) {
						return Integer.class.getSimpleName();
					} else if (fieldType == Boolean.class
							|| fieldType == boolean.class) {
						return Boolean.class.getSimpleName();
					} else if (Enum.class.isAssignableFrom(fieldType)) {
						return Enum.class.getSimpleName();
					} else if (fieldType == Float.class
							|| fieldType == float.class) {
						return Float.class.getSimpleName();
					} else if (fieldType == Character.class
							|| fieldType == char.class) {
						return Character.class.getSimpleName();
					} else if (fieldType == File.class) {
						return File.class.getSimpleName();
					} else {
						throw new RuntimeException("unsupported type: "
								+ fieldType.getSimpleName());
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
				Field field = (Field) configNode.getUserObject();
				String value = textField.getText();
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
					final char ch;
					if (value == null || value.length() == 0) {
						ch = (char) 0;
					} else {
						ch = value.charAt(0);
					}
					configNode.setValue(Character.valueOf(ch).charValue());
				}
				update();
			}
		}
	};

	public Action doSetBoolean = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (configNode.getUserObject() instanceof Field) {
				Boolean value = checkbox.isSelected();
				configNode.setValue(Boolean.valueOf(value).booleanValue());
				update();
			}
		}
	};

	public Action doSetEnum = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (configNode.getUserObject() instanceof Field) {
				Enum<?> value = (Enum<?>) combo.getSelectedItem();
				configNode.setValue(value);
				update();
			}
		}
	};

	public Action doImport = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileDialog = new JFileChooser(lastDir);
			fileDialog.setFileFilter(configFilter);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(ConfigView.this);
			int rc = fileDialog.showOpenDialog(containerFrame);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				lastDir = fileDialog.getSelectedFile();
				File file = fileDialog.getSelectedFile();
				JOptionPane.showMessageDialog(ConfigView.this, swix
						.getLocalizer().getString("PLEASE_RESTART"));
				((Configuration) config).setReconfigFilename(file
						.getAbsolutePath());
			}
		}
	};

	public Action doExport = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileDialog = new JFileChooser(lastDir);
			fileDialog.setFileFilter(configFilter);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(ConfigView.this);
			int rc = fileDialog.showSaveDialog(containerFrame);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				lastDir = fileDialog.getSelectedFile();
				try {
					File file = fileDialog.getSelectedFile();
					JAXBContext jaxbContext = JAXBContext
							.newInstance(Configuration.class);
					Marshaller marshaller = jaxbContext.createMarshaller();
					marshaller.marshal(config, file);
				} catch (JAXBException e1) {
					e1.printStackTrace();
				}
			}
		}
	};

	public Action doBrowse = new AbstractAction() {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fileDialog = new JFileChooser(lastDir);
			fileDialog.setFileSelectionMode(fileChooserFilter);
			final Frame containerFrame = JOptionPane
					.getFrameForComponent(ConfigView.this);
			int rc = fileDialog.showOpenDialog(containerFrame);
			if (rc == JFileChooser.APPROVE_OPTION
					&& fileDialog.getSelectedFile() != null) {
				lastDir = fileDialog.getSelectedFile();
				File file = fileDialog.getSelectedFile();
				textField.setText(file.getAbsolutePath());
			}
		}
	};

	private void update() {
		String expansionState = TreeUtil.getExpansionState(configTree, 0);
		configModel.nodeStructureChanged((TreeNode) configModel.getRoot());
		TreeUtil.restoreExpanstionState(configTree, 0, expansionState);
	}

	@Override
	public void notify(UIEvent evt) {
		if (!evt.isOfType(IUpdateUI.class)) {
			if (refreshCounter++ % 10 == 0) {
				if (configModel != null) {
					update();
				}
			}
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
		// TODO Auto-generated method stub

	}
}