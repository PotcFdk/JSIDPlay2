package server.netsiddev;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTextArea;

public class About extends SIDDeviceStage {
	private static final long serialVersionUID = 1L;

	/** Build date calculated from our own modify time */
	public static Calendar LAST_MODIFIED;

	static {
		try {
			URL us = About.class.getProtectionDomain().getCodeSource().getLocation();
			LAST_MODIFIED = Calendar.getInstance();
			LAST_MODIFIED.setTime(new Date(us.openConnection().getLastModified()));
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private JTextArea credits;
	private JButton ok;

	public About() {
		credits = new JTextArea();
		getContentPane().add(credits, BorderLayout.CENTER);

		ok = new JButton(util.getBundle().getString("OK"));
		ok.addActionListener(event -> okPressed(event));
		getContentPane().add(ok, BorderLayout.PAGE_END);

		getRootPane().setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4, Color.DARK_GRAY));
		setBackground(Color.WHITE);
		
		initialize();
	}

	private void okPressed(ActionEvent event) {
		dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
	}

	private void initialize() {
		credits.setText(String.format(getBundle().getString("CREDITS"), LAST_MODIFIED.get(Calendar.YEAR)));
		setWait(true);
	}
}
