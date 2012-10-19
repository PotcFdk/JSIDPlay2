package applet.config;

import javax.swing.JTree;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import sidplay.ini.intf.IConfig;
import applet.TuneTab;
import applet.events.IUpdateUI;
import applet.events.UIEvent;

public class ConfigView extends TuneTab {

	private JTree configTree;
	private ConfigModel configModel;

	public ConfigView(Player player, IConfig config) {
		try {
			SwingEngine swix = new SwingEngine(this);
			swix.insert(ConfigView.class.getResource("Config.xml"), this);
			configModel = (ConfigModel) configTree.getModel();
			configModel.setRootUserObject(config);
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