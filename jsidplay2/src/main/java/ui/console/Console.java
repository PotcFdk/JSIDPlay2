package ui.console;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import libsidplay.Player;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class Console extends Tab implements UIPart {
	public static final String ID = "CONSOLE";
	private static final String STYLE_ERROR_CONSOLE = "errorConsole";
	private static final String STYLE_OUTPUT_CONSOLE = "outputConsole";

	@FXML
	private ConsoleOutput out, err;

	private UIUtil util;

	public Console(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
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
