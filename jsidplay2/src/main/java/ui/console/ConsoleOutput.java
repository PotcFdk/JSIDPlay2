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
import sidplay.ConsolePlayer;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;
import ui.entities.config.Configuration;

public class ConsoleOutput extends VBox implements UIPart {

	private static final String NEWLiNE = System.getProperty("line.separator");

	@FXML
	protected TextArea console;
	@FXML
	private TitledPane titledPane;

	private UIUtil util;

	@FXML
	private void clearConsole() {
		console.clear();
	}

	public ConsoleOutput(C64Window window, ConsolePlayer consolePlayer,
			Player player, Configuration config) {
		util = new UIUtil(window, consolePlayer, player, config, this);
		getChildren().add((Node) util.parse());
	}

	public PrintStream getPrintStream(final OutputStream original) {
		return new PrintStream(new OutputStream() {

			private StringBuffer contents = new StringBuffer();

			public synchronized void write(final byte[] b, final int off,
					final int len) throws IOException {
				original.write(b, off, len);
				final String str = new String(b, off, len);
				contents.append(str);
				if (str.endsWith(NEWLiNE)) {
					flush();
				}
			}

			@Override
			public synchronized void write(int ch) throws IOException {
				contents.append((char) ch);
			}

			@Override
			public synchronized void flush() throws IOException {
				super.flush();
				Platform.runLater(() -> {
					console.appendText(contents.toString());
					contents.setLength(0);
				});
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