package ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import libsidplay.Player;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class ConsoleOutput extends VBox implements UIPart {

	@FXML
	protected TextArea console;
	@FXML
	private TitledPane titledPane;

	private UIUtil util;

	public ConsoleOutput(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		getChildren().add((Node) util.parse());
	}

	@FXML
	private void clearConsole() {
		console.clear();
	}

	public PrintStream getPrintStream(final OutputStream original) {
		return new PrintStream(new OutputStream() {

			public synchronized void write(final byte[] b, final int off,
					final int len) throws IOException {
				original.write(b, off, len);
				print(new String(b, off, len));
			}

			@Override
			public synchronized void write(int ch) throws IOException {
				original.write(ch);
				print(String.valueOf((char) ch));
			}

			private void print(String str) {
				Platform.runLater(() -> console.appendText(str));
			}

		});
	}

	public TitledPane getTitledPane() {
		return titledPane;
	}

	public TextArea getConsole() {
		return console;
	}

}