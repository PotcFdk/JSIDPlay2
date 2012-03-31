/*
 * DKnob2.java
 * (c) 2005 by Joakim Eriksson
 *
 * DKnob is a component similar to JSlider but with
 * round "user interface", a knob.
 */
package applet.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Arc2D;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

public class DKnob2 extends JComponent {
	private final static float START = 225;
	private final static float LENGTH = 270;

	public static final Color DARK = new Color(0x40, 0x40, 0x40, 0xe0);
	public static final Color DARK_T = new Color(0x40, 0x40, 0x40, 0x80);
	public static final Color DARK_L = new Color(0x80, 0x80, 0x80, 0xa0);
	public static final Color LIGHT_D = new Color(0xa0, 0xa0, 0xa0, 0xa0);
	public static final Color LIGHT = new Color(0xc0, 0xc0, 0xc0, 0xa0);
	public static final Color LIGHT_T = new Color(0xc0, 0xc0, 0xc0, 0x80);

	protected class KnobPanel extends JComponent {
		private final Dimension MIN_SIZE = new Dimension(30, 30);
		private final Dimension PREF_SIZE = new Dimension(40, 40);
		private final Color DEFAULT_FOCUS_COLOR = new Color(0x8080ff);
		private final RenderingHints AALIAS = new RenderingHints(
				RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);

		protected int size;
		protected int middlex, middley;

		protected KnobPanel() {
			setPreferredSize(PREF_SIZE);

			// Let the user control the knob with the mouse
			addMouseMotionListener(new MouseMotionAdapter() {
				@Override
				public void mouseDragged(final MouseEvent me) {
					if (!isEnabled()) {
						return;
					}
					// Measure relative the middle of the button!
					final int xpos = middlex - me.getX();
					final int ypos = middley - me.getY();
					double newAng = Math.atan2(ypos, xpos) / Math.PI * 180;
					if (newAng < -90) {
						newAng += 360;
					}
					newAng += START - 180;
					newAng /= LENGTH;
					setFractionalValue((float) newAng);
				}

				@Override
				public void mouseMoved(final MouseEvent me) {
					if (!isEnabled()) {
						return;
					}
				}
			});

			// Let the user control the knob with the keyboard
			addKeyListener(new KeyListener() {
				public void keyTyped(final KeyEvent e) {
				}

				public void keyReleased(final KeyEvent e) {
				}

				public void keyPressed(final KeyEvent e) {
					if (!isEnabled()) {
						return;
					}
					final int k = e.getKeyCode();
					if (k == KeyEvent.VK_RIGHT) {
						setFractionalValue(val + 1f / (highInt - lowInt));
						e.consume();
					} else if (k == KeyEvent.VK_LEFT) {
						setFractionalValue(val - 1f / (highInt - lowInt));
						e.consume();
					}
				}
			});

			// Handle focus so that the knob gets the correct focus
			// highlighting.
			addFocusListener(new FocusListener() {
				public void focusGained(final FocusEvent e) {
					repaint();
				}

				public void focusLost(final FocusEvent e) {
					repaint();
				}
			});
		}

		@Override
		public Dimension getMinimumSize() {
			return MIN_SIZE;
		}

		// Paint the DKnob
		@Override
		public void paintComponent(final Graphics g) {
			final Graphics2D g2d = (Graphics2D) g;

			final int width = getWidth();
			final int height = getHeight();
			middlex = width / 2;
			middley = height / 2;
			size = Math.min(width, height);

			g2d.setBackground(getParent().getBackground());
			g2d.addRenderingHints(AALIAS);

			g2d.translate(middlex, middley);
			g2d.scale(size / 30.0, size / 30.0);
			g2d.translate(-15, -15);

			if (hasFocus()) {
				g2d.setColor(DEFAULT_FOCUS_COLOR);
				g2d.fill(new Arc2D.Double(0.0, 0.0, 27.0, 27.0, 90.0, 360.0,
						Arc2D.OPEN));
			}
			if (isEnabled()) {
				g2d.setColor(DARK);
			} else {
				g2d.setColor(LIGHT);
			}

			/* Knob base */
			g2d.fill(new Arc2D.Double(1.0, 1.0, 25.0, 25.0, 90.0, 360.0,
					Arc2D.OPEN));
			g2d.setColor(DARK_L);
			g2d.fill(new Arc2D.Double(2.0, 2.0, 22.0, 22.0, 90.0, 360.0,
					Arc2D.OPEN));
			g2d.setColor(LIGHT_D);
			g2d.draw(new Arc2D.Double(3.0, 3.0, 21.0, 21.0, 75.0, 120.0,
					Arc2D.OPEN));
			g2d.setColor(LIGHT);
			g2d.draw(new Arc2D.Double(4.0, 4.0, 20.0, 20.0, 122.5, 25.0,
					Arc2D.OPEN));

			/* Knob edge */
			g2d.setColor(DARK);
			g2d.fill(new Arc2D.Double(5.0, 5.0, 18.0, 18.0, 90.0, 360.0,
					Arc2D.OPEN));
			/* knob shadow */
			g2d.setColor(DARK_T);
			g2d.fill(new Arc2D.Double(9.0, 9.0, 18.0, 18.0, 90.0, 360.0,
					Arc2D.OPEN));
			/* knob high/lowlights */
			g2d.setColor(DARK_L);
			g2d.fill(new Arc2D.Double(6.0, 6.0, 16.0, 16.0, 90.0, 360.0,
					Arc2D.OPEN));
			g2d.setColor(DARK_T);
			g2d.draw(new Arc2D.Double(7.0, 7.0, 14.0, 14.0, 270.0, 90.0,
					Arc2D.OPEN));
			g2d.setColor(LIGHT_D);
			g2d.draw(new Arc2D.Double(7.0, 7.0, 15.0, 15.0, 90.0, 90.0,
					Arc2D.OPEN));

			/* Value indicator */
			double ang = START - LENGTH * val;
			g2d.setColor(LIGHT_D);
			g2d.fill(new Arc2D.Double(7.0, 7.0, 14.0, 14.0, ang - 60, 120.0,
					Arc2D.OPEN));
			g2d.setColor(DARK);
			g2d.draw(new Arc2D.Double(6.0, 6.0, 16.0, 16.0, ang - 1, 2.0,
					Arc2D.PIE));
			g2d.setColor(DARK);
			g2d.draw(new Arc2D.Double(5.0, 5.0, 18.0, 18.0, ang - 8, 16.0,
					Arc2D.OPEN));
			g2d.draw(new Arc2D.Double(4.0, 4.0, 20.0, 20.0, ang - 4, 8.0,
					Arc2D.OPEN));
		}
	}

	private String type;
	private final JLabel legend;

	private ChangeEvent changeEvent = null;
	private final EventListenerList listenerList = new EventListenerList();

	protected float val;

	protected int lowInt = 0;
	protected int highInt = 100;
	private float divisor = 0;
	private JLabel nameLabel;
	protected KnobPanel knobPanel;

	public DKnob2() {
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;

		nameLabel = new JLabel();
		add(nameLabel, c);
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		knobPanel = new KnobPanel();
		add(knobPanel, c);
		c.fill = GridBagConstraints.NONE;
		c.weighty = 0;
		add(legend = new JLabel(), c);
	}

	public void setLabel(final String name) {
		nameLabel.setText(name);
	}

	public void setType(final String typeName) {
		type = typeName;
	}

	public int getIntValue() {
		return lowInt + Math.round(val * (highInt - lowInt));
	}

	public void setIntValue(final int val) {
		setFractionalValue((float) (val - lowInt) / (highInt - lowInt));
	}

	public float getValue() {
		return getIntValue() / getDivisor();
	}

	public void setValue(final float val) {
		setIntValue(Math.round(val * getDivisor()));
	}

	protected void setFractionalValue(float val) {
		if (val < 0) {
			val = 0;
		}
		if (val > 1) {
			val = 1;
		}
		this.val = val;
		legend.setText(getIntValue() / getDivisor()
				+ (type != null ? type : ""));

		repaint();
		fireChangeEvent();
	}

	public void addChangeListener(final ChangeListener cl) {
		listenerList.add(ChangeListener.class, cl);
	}

	public void removeChangeListener(final ChangeListener cl) {
		listenerList.remove(ChangeListener.class, cl);
	}

	protected void fireChangeEvent() {
		// Guaranteed to return a non-null array
		final Object[] listeners = listenerList.getListenerList();
		// Process the listeners last to first, notifying
		// those that are interested in this event
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ChangeListener.class) {
				// Lazily create the event:
				if (changeEvent == null) {
					changeEvent = new ChangeEvent(this);
				}
				((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
			}
		}
	}

	public void setIntervalLow(final int low) {
		this.lowInt = low;
	}

	public void setIntervalHigh(final int high) {
		this.highInt = high;
	}

	// Just used for printing the label...
	public void setDivisor(final float divisor) {
		this.divisor = divisor;
	}

	public float getDivisor() {
		return divisor;
	}

	public void setFocusableDKnob(boolean focusable) {
		if (focusable) {
			knobPanel.setFocusable(true);

			knobPanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mousePressed(final MouseEvent me) {
					knobPanel.requestFocus();
				}
			});
		}

	}

}
