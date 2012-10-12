package applet.console;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import libsidplay.Player;
import libsidplay.sidtune.SidTune;

import org.swixml.SwingEngine;

import applet.TuneTab;
import applet.events.UIEvent;

public class ConsoleView extends TuneTab {
	public class ConsoleOutput extends OutputStream {
		private final OutputStream original;
		private final boolean stdout;

		public ConsoleOutput(OutputStream original, boolean stdout) {
			this.original = original;
			this.stdout = stdout;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			original.write(b, off, len);
			updateConsole(stdout, new String(b, off, len));
		}

		@Override
		public synchronized void write(int b) throws IOException {
			updateConsole(stdout, String.valueOf((char) b));
		}
	}

	JTextPane console;
	private ConsoleOutput fOut;
	private ConsoleOutput fErr;
	private StyledDocument fDocument;

	private static final String STYLE_STDOUT = "STYLE_STDOUT";
	private static final String STYLE_STDERR = "STYLE_STDERR";

	public ConsoleView() {
		try {
			SwingEngine swix = new SwingEngine(this);
			swix.insert(ConsoleView.class.getResource("ConsoleView.xml"), this);

			fDocument = console.getStyledDocument();

			// BEGIN set style
			Style def = StyleContext.getDefaultStyleContext().getStyle(
					StyleContext.DEFAULT_STYLE);
			Style regular = fDocument.addStyle("regular", def);
			StyleConstants.setFontFamily(regular, "Courier");
			StyleConstants.setFontSize(regular, 12);

			Style s = fDocument.addStyle(STYLE_STDOUT, regular);
			StyleConstants.setForeground(s, Color.blue);

			s = fDocument.addStyle(STYLE_STDERR, regular);
			StyleConstants.setBold(s, true);
			StyleConstants.setForeground(s, Color.red);
			// END set style

			fOut = new ConsoleOutput(System.out, true);
			System.setOut(new PrintStream(fOut));
			fErr = new ConsoleOutput(System.err, false);
			System.setErr(new PrintStream(fErr));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateConsole(boolean isStdOut, String str) throws IOException {
		try {
			fDocument.insertString(fDocument.getLength(), str,
					fDocument.getStyle(isStdOut ? STYLE_STDOUT : STYLE_STDERR));
			console.setCaretPosition(fDocument.getLength());
		} catch (BadLocationException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void setTune(Player m_engine, SidTune m_tune) {
	}

	public void notify(UIEvent evt) {
	}
}
