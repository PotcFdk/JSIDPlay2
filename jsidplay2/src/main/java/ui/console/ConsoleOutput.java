package ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class ConsoleOutput extends VBox implements UIPart {
	
	private StringBuilder output = new StringBuilder();
	
	@FXML
	protected TextArea console;
	@FXML
	private TitledPane titledPane;

	private UIUtil util;

	public ConsoleOutput(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		util.parse(this);
	}

	@FXML
	private void clearConsole() {
		console.clear();
		output.setLength(0);
	}

	public PrintStream getPrintStream(final OutputStream original) {
		return new PrintStream(new OutputStream() {

			public synchronized void write(final byte[] b, final int off, final int len) throws IOException {
				original.write(b, off, len);
				print(new String(b, off, len));
			}

			@Override
			public synchronized void write(int ch) throws IOException {
				original.write(ch);
				print(String.valueOf((char) ch));
			}

			private void print(String str) {
				output.append(str);
				if (str.indexOf('\n') != -1) {
					Platform.runLater(() -> {
						console.setText(output.toString());
						console.setScrollTop(Double.MAX_VALUE);
					});
				}
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