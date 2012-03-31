package netsiddev;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

class EscapeDialog extends JDialog {
	public EscapeDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);
	}

	@Override
	protected JRootPane createRootPane() {
		JRootPane rootPane = new JRootPane();
		KeyStroke stroke = KeyStroke.getKeyStroke("ESCAPE");
		Action actionListener = new AbstractAction() {
			public void actionPerformed(ActionEvent actionEvent) {
				setVisible(false);
			}
		};
		InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
		inputMap.put(stroke, "ESCAPE");
		rootPane.getActionMap().put("ESCAPE", actionListener);

		return rootPane;
	}
}

public class AboutBox extends EscapeDialog {
	final String aboutText;
	
	public AboutBox(String title, String aboutText) {
		super(null, title, true);
		this.aboutText = aboutText;
		setLayout(new BorderLayout());
		
        URL url = this.getClass().getResource("jsidplay2.png"); 
        Image image = Toolkit.getDefaultToolkit().getImage(url);
        setIconImage(image);

		add(createCredits());
		add(createOkButton(), BorderLayout.SOUTH);

		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		pack();
		setMinimumSize(getSize());
		
		setLocation(getCenteredLocation());		
	}

	private Point getCenteredLocation() {
		Dimension d = getToolkit().getScreenSize();
		Dimension s = getSize();
		return new Point((d.width - s.width)/2, (d.height - s.height)/2); 
	}
	

	private JComponent createCredits() {
		JTextArea textField = new JTextArea(aboutText);
		textField.setEditable(false);
		textField.setLineWrap(false);
		textField.setHighlighter(null);
		textField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(BorderFactory.createEtchedBorder());
		p.add(textField);
		return new JScrollPane(p);
	}

	private JComponent createOkButton() {
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
			}
		});
		
		JPanel p = new JPanel();
		p.add(okButton);
		return p;
	}
}