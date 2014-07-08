package ui.proxysettings;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import libsidplay.Player;
import ui.common.C64Window;

public class ProxySettings extends C64Window {

	@FXML
	protected TextField proxyHost, proxyPort;
	@FXML
	private CheckBox proxyEnable;

	public ProxySettings(Player player) {
		super(player);
	}

	@FXML
	private void initialize() {
		proxyEnable.setSelected(util.getConfig().getSidplay2().isEnableProxy());
		proxyHost.setText(util.getConfig().getSidplay2().getProxyHostname());
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setText(String.valueOf(util.getConfig().getSidplay2()
				.getProxyPort()));
		proxyPort.setEditable(proxyEnable.isSelected());
	}

	@FXML
	private void setEnableProxy() {
		proxyHost.setEditable(proxyEnable.isSelected());
		proxyPort.setEditable(proxyEnable.isSelected());
		util.getConfig().getSidplay2().setEnableProxy(proxyEnable.isSelected());
	}

	@FXML
	private void setProxyHost() {
		util.getConfig().getSidplay2().setProxyHostname(proxyHost.getText());
	}

	@FXML
	private void setProxyPort() {
		util.getConfig()
				.getSidplay2()
				.setProxyPort(
						proxyPort.getText().length() > 0 ? Integer
								.valueOf(proxyPort.getText()) : 80);
	}

}
