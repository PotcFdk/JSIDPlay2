package ui.console;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import libsidplay.Player;
import sidplay.ConsolePlayer;
import ui.common.C64Stage;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.Configuration;

public class Console extends Tab implements UIPart {
	private static final String STYLE_ERROR_CONSOLE = "errorConsole";
	private static final String STYLE_OUTPUT_CONSOLE = "outputConsole";

	@FXML
	private ConsoleOutput out, err;

	private UIUtil util;

	public Console(C64Stage c64Stage, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		util = new UIUtil(c64Stage, consolePlayer, player, config, this);
		setContent((Node) util.parse());
	}

	@FXML
	private void initialize() {
		out.getTitledPane().setText(util.getBundle().getString("OUT"));
		out.getConsole().getStyleClass().add(STYLE_OUTPUT_CONSOLE);
		err.getTitledPane().setText(util.getBundle().getString("ERR"));
		err.getConsole().getStyleClass().add(STYLE_ERROR_CONSOLE);
		System.setOut(out.getPrintStream(System.out));
		System.setErr(err.getPrintStream(System.err));
	}

}
