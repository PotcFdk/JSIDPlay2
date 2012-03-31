package applet.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.plaf.basic.BasicButtonUI;

@SuppressWarnings("serial")
public class JNiceButton extends JButton {

    private boolean fNonInteractive;

	public JNiceButton() {
        makeItNice();
    }
    
    public JNiceButton(ImageIcon imageIcon) {
		super(imageIcon);
        makeItNice();
	}

    public JNiceButton(ImageIcon imageIcon, boolean noninteractive) {
		super(imageIcon);
		fNonInteractive = noninteractive;
        makeItNice();
	}

	private void makeItNice() {
		//Making nice rollover effect
        //we use the same listener for all buttons
		if (!fNonInteractive) {
	        addMouseListener(buttonMouseListener);
		}
        setBorder(BorderFactory.createEtchedBorder());
        setBorderPainted(false);
        setOpaque(false);
        setFocusable(false);
        setRolloverEnabled(true);
        //Make it transparent
        setContentAreaFilled(false);
        //Make the button looks the same for all Laf's
        setUI(new BasicButtonUI());
        int size = getNiceSize();
		if (size != -1) {
			setPreferredSize(new Dimension(size, size));
		}
	}

	protected int getNiceSize() {
		return 17;
	}

	private final static MouseListener buttonMouseListener = new MouseAdapter() {
        @Override
		public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        @Override
		public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}
