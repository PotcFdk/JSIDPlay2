package applet.config;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.TitledBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import applet.TuneTab;
import applet.config.annotations.ConfigDescription;
import applet.config.annotations.ConfigFieldType;
import applet.editors.EditorUtils;
import applet.entities.config.Configuration;
import applet.entities.config.service.ConfigService;
import applet.events.UIEvent;
import applet.events.favorites.IAddFavoritesTab;
import applet.events.favorites.IRemoveFavoritesTab;
import applet.filefilter.ConfigFileFilter;

public class ConfigView extends TuneTab {

	private SwingEngine swix;

	private JTree configTree;
	protected JPanel parent;

	private EditorUtils editorUtils;

	protected JTextField textField;
	protected JCheckBox checkbox;
	protected JComboBox<Enum<?>> combo;
	protected JTextArea description;

	private JComponent editor;

	private Configuration config;
	protected File lastDir;
	protected FileFilter configFilter = new ConfigFileFilter();

	private ConfigModel configModel;
	protected ConfigNode configNode;

	protected int fileChooserFilter;

	private ConfigService configService;

	public ConfigView(EntityManager em, Player player, Configuration config) {
		this.configService = new ConfigService(em);
		this.config = config;
		editorUtils = new EditorUtils(this);
		try {
			swix = new SwingEngine(this);
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
							parent.removeAll();
							if (editor != null) {
								editor.setToolTipText(swix.getLocalizer()
										.getString("NO_TOOLTIP"));
							}
							description.setText(swix.getLocalizer().getString(
									"NO_DESC"));
							TreeNode parentConfigNode = configNode.getParent();
							if (configNode.getUserObject() instanceof String) {
								createEditor(parentConfigNode, configNode
										.getUserObject().getClass(), null);
								textField.setText(configNode.getUserObject()
										.toString());
								textField.setEditable(false);
							} else if (configNode.getUserObject() instanceof Field) {
								Field field = (Field) configNode
										.getUserObject();
								JComponent component = null;
								if (isTextFieldType(field)) {
									ConfigFieldType uiConfig = field
											.getAnnotation(ConfigFieldType.class);
									if (uiConfig != null
											&& uiConfig.uiClass() != null) {
										fileChooserFilter = uiConfig.filter();
										createEditor(parentConfigNode,
												uiConfig.uiClass(),
												field.getName());
									} else {
										createEditor(parentConfigNode,
												field.getType(),
												field.getName());
									}
									component = initTextField();
								} else if (isCheckBoxType(field)) {
									createEditor(parentConfigNode,
											field.getType(), field.getName());
									component = initCheckBox();
								} else if (isEnumType(field)) {
									createEditor(parentConfigNode,
											field.getType(), field.getName());
									component = initEnumComboBox(field);
								}
								ConfigDescription uiDesc = field
										.getAnnotation(ConfigDescription.class);
								if (uiDesc != null) {
									description.setText(swix.getLocalizer()
											.getString(uiDesc.bundleKey()));
									if (component != null) {
										component.setToolTipText(swix
												.getLocalizer()
												.getString(
														uiDesc.toolTipBundleKey()));
									}
								}
							} else {
								createEditor(parentConfigNode, null,
										String.valueOf(configNode));
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					parent.repaint();
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
							|| (field.getType() == Long.class || field
									.getType() == long.class)
							|| (field.getType() == Integer.class || field
									.getType() == int.class)
							|| (field.getType() == Short.class || field
									.getType() == short.class)
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

				@SuppressWarnings({ "rawtypes" })
				private JComponent initEnumComboBox(Field field) {
					combo.setSelectedItem(configNode.getValue() != null ? (Enum) configNode
							.getValue() : null);
					return combo;
				}

				private void createEditor(TreeNode parentConfigNode,
						Class<?> type, String fieldName) throws Exception {
					editor = editorUtils.render(type);
					editor.setBorder(new TitledBorder(fieldName));
					String category = parentConfigNode != null ? parentConfigNode
							.toString() : "";
					parent.setBorder(new TitledBorder(category));
					parent.setLayout(new GridBagLayout());
					GridBagConstraints gbc = new GridBagConstraints();
					gbc.weightx = 1;
					gbc.weighty = 1;
					gbc.fill = GridBagConstraints.BOTH;
					parent.add(editor, gbc);
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
				if (field.getType() == String.class) {
					configNode.setValue(textField.getText());
				} else if (Enum.class.isAssignableFrom(field.getType())) {
					configNode.setValue((Enum<?>) combo.getSelectedItem());
				} else if (field.getType() == Boolean.class
						|| field.getType() == boolean.class) {
					configNode.setValue(checkbox.isSelected());
				} else if (field.getType() == Short.class
						|| field.getType() == short.class) {
					configNode.setValue(Short.valueOf(textField.getText())
							.shortValue());
				} else if (field.getType() == Integer.class
						|| field.getType() == int.class) {
					configNode.setValue(Integer.valueOf(textField.getText())
							.intValue());
				} else if (field.getType() == Long.class
						|| field.getType() == long.class) {
					configNode.setValue(Long.valueOf(textField.getText())
							.longValue());
				} else if (field.getType() == Float.class
						|| field.getType() == float.class) {
					configNode.setValue(Float.valueOf(textField.getText())
							.floatValue());
				} else if (field.getType() == Character.class
						|| field.getType() == char.class) {
					final char ch;
					if (textField.getText() == null
							|| textField.getText().length() == 0) {
						ch = (char) 0;
					} else {
						ch = textField.getText().charAt(0);
					}
					configNode.setValue(Character.valueOf(ch).charValue());
				}
				repaint();
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
				config.setReconfigFilename(file.getAbsolutePath());
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
				final File file;
				if (!fileDialog.getSelectedFile().getName()
						.endsWith(ConfigFileFilter.EXT_CONFIGURATION)) {
					file = new File(fileDialog.getSelectedFile()
							.getAbsolutePath()
							+ ConfigFileFilter.EXT_CONFIGURATION);
				} else {
					file = fileDialog.getSelectedFile();
				}
				configService.backup(config, file);
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
		if (evt.isOfType(IAddFavoritesTab.class)
				|| evt.isOfType(IRemoveFavoritesTab.class)) {
			update();
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
		// TODO Auto-generated method stub

	}
}