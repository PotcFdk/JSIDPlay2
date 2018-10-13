package server.netsiddev;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;

public abstract class SIDDeviceStage extends JDialog implements SIDDeviceUIPart {

	private static final long serialVersionUID = 1L;
	private static final KeyStroke escapeStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final String dispatchWindowClosingActionMapKey = "WINDOW_CLOSING";

	protected SIDDeviceUIUtil util;
	private boolean wait;

	public SIDDeviceStage() {
		util = new SIDDeviceUIUtil();
		util.parse(this);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		installEscapeCloseOperation(this);
		setResizable(false);
		setAlwaysOnTop(true);

		setTitle(util.getBundle().getString("TITLE"));

		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(dim.width / 2 - this.getSize().width / 2, dim.height / 2 - this.getSize().height / 2);
	}

	public void open() throws IOException {
		if (wait) {
			setModal(true);
		}
		pack();
		setVisible(true);
	}

	public boolean isWait() {
		return wait;
	}

	public void setWait(boolean wait) {
		this.wait = wait;
	}

	public ResourceBundle getBundle() {
		return util.getBundle();
	}

	private void installEscapeCloseOperation(final JDialog dialog) {
		Action dispatchClosing = new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent event) {
				dialog.dispatchEvent(new WindowEvent(dialog, WindowEvent.WINDOW_CLOSING));
			}
		};
		JRootPane root = dialog.getRootPane();
		root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(escapeStroke, dispatchWindowClosingActionMapKey);
		root.getActionMap().put(dispatchWindowClosingActionMapKey, dispatchClosing);
	}

}
