package applet.config;

import java.awt.BorderLayout;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

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

	private JTree configTree;
	protected JPanel parent;
	private ConfigModel configModel;

	public ConfigView(Player player, IConfig config) {
		try {
			SwingEngine swix = new SwingEngine(this);
			swix.insert(ConfigView.class.getResource("Config.xml"), this);
			configModel = (ConfigModel) configTree.getModel();
			configModel.setRootUserObject(swix, config);
			configTree.addTreeSelectionListener(new TreeSelectionListener() {

				@Override
				public void valueChanged(TreeSelectionEvent event) {
					final TreePath treePath = event.getNewLeadSelectionPath();
					if (treePath == null) {
						return;
					}
					final Object pathComponent = treePath
							.getLastPathComponent();
					try {
						if (pathComponent instanceof ConfigNode) {
							ConfigNode configNode = (ConfigNode) pathComponent;
							parent.removeAll();
							JTextField textField = new JTextField();
							if (configNode.getUserObject() instanceof Field) {
								Field field = (Field) configNode
										.getUserObject();

								if (configNode.getValue(field) != null) {
									textField.setText(configNode
											.getValue(field).toString());
								}
								parent.add(textField, BorderLayout.NORTH);
							}
						}
					} catch (NoSuchMethodException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IllegalAccessException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvocationTargetException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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