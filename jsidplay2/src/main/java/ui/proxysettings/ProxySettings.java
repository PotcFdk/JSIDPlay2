package ui.proxysettings;

import static javafx.beans.binding.Bindings.bindBidirectional;
import static ui.entities.config.SidPlay2Section.DEFAULT_PROXY_ENABLE;
import static ui.entities.config.SidPlay2Section.DEFAULT_PROXY_HOSTNAME;
import static ui.entities.config.SidPlay2Section.DEFAULT_PROXY_PORT;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.util.converter.IntegerStringConverter;
import sidplay.Player;
import ui.common.C64Window;
import ui.entities.config.SidPlay2Section;

public class ProxySettings extends C64Window {

	@FXML
	private CheckBox proxyEnable;

	@FXML
	private TextField proxyHostname, proxyPort;

	public ProxySettings() {
		super();
	}

	public ProxySettings(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		final SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		proxyEnable.selectedProperty().bindBidirectional(sidplay2Section.proxyEnableProperty());
		proxyHostname.textProperty().bindBidirectional(sidplay2Section.proxyHostnameProperty());
		bindBidirectional(proxyPort.textProperty(), sidplay2Section.proxyPortProperty(), new IntegerStringConverter());
	}

	@FXML
	private void restoreDefaults() {
		final SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();

		sidplay2Section.setProxyEnable(DEFAULT_PROXY_ENABLE);
		sidplay2Section.setProxyHostname(DEFAULT_PROXY_HOSTNAME);
		sidplay2Section.setProxyPort(DEFAULT_PROXY_PORT);
	}

}
