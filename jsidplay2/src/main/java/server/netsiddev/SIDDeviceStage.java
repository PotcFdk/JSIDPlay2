package server.netsiddev;

import static java.awt.Dialog.ModalityType.APPLICATION_MODAL;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

public abstract class SIDDeviceStage extends JDialog implements SIDDeviceUIPart {
	private static final long serialVersionUID = 1L;
	
	private static final KeyStroke ESCAPE_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
	private static final String DISPATCH_WINDOW_CLOSING_ACTION_MAP_KEY = "WINDOW_CLOSING";
	private static final Dimension SCREEN_SIZE = Toolkit.getDefaultToolkit().getScreenSize();

	private final Action dispatchClosing = new AbstractAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent event) {
			SIDDeviceStage.this.dispatchEvent(new WindowEvent(SIDDeviceStage.this, WindowEvent.WINDOW_CLOSING));
		}
	};

	protected SIDDeviceUIUtil util;

	public SIDDeviceStage() {
		util = new SIDDeviceUIUtil();
		util.parse(this);

		setTitle(util.getBundle().getString("TITLE"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		getRootPane().getInputMap(WHEN_IN_FOCUSED_WINDOW).put(ESCAPE_STROKE, DISPATCH_WINDOW_CLOSING_ACTION_MAP_KEY);
		getRootPane().getActionMap().put(DISPATCH_WINDOW_CLOSING_ACTION_MAP_KEY, dispatchClosing);
		setResizable(false);
	}

	public void open() throws IOException {
		setModalityType(APPLICATION_MODAL);
		pack();
		setLocation((SCREEN_SIZE.width >> 1) - (getSize().width >> 1),
				(SCREEN_SIZE.height >> 1) - (getSize().height >> 1));
		setVisible(true);
	}

	public ResourceBundle getBundle() {
		return util.getBundle();
	}

}
