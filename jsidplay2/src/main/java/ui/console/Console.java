package ui.console;

import javafx.fxml.FXML;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;

public class Console extends C64VBox implements UIPart {
	public static final String ID = "CONSOLE";
	private static final String STYLE_ERROR_CONSOLE = "errorConsole";
	private static final String STYLE_OUTPUT_CONSOLE = "outputConsole";

	@FXML
	private ConsoleOutput out, err;

	public Console() {
		super();
	}

	public Console(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		out.getTitledPane().setText(util.getBundle().getString("OUT"));
		out.getConsole().getStyleClass().add(STYLE_OUTPUT_CONSOLE);
		err.getTitledPane().setText(util.getBundle().getString("ERR"));
		err.getConsole().getStyleClass().add(STYLE_ERROR_CONSOLE);
		System.setOut(out.getPrintStream(System.out));
		System.setErr(err.getPrintStream(System.err));
	}

}
