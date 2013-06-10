package ui.console;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import ui.common.C64Tab;
import ui.events.UIEvent;

public class Console extends C64Tab {
	public class ConsoleOutput extends OutputStream {
		private final OutputStream original;

		public ConsoleOutput(OutputStream original) {
			this.original = original;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len)
				throws IOException {
			original.write(b, off, len);
			String str = new String(b, off, len);
			updateConsole(str);
		}

		@Override
		public synchronized void write(int b) throws IOException {
		}
	}

	@FXML
	private TextArea console;
	private ConsoleOutput fOut;
	private ConsoleOutput fErr;

	@Override
	public String getBundleName() {
		return getClass().getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(getClass().getSimpleName() + ".fxml");
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		fOut = new ConsoleOutput(System.out);
		System.setOut(new PrintStream(fOut, false));
		fErr = new ConsoleOutput(System.err);
		System.setErr(new PrintStream(fErr, false));
	}

	public void updateConsole(final String str) throws IOException {
		Platform.runLater(new Runnable() {

			@Override
			public void run() {
				console.setText(console.getText() + str);
			}
		});
	}

	public void notify(UIEvent evt) {
	}

}
