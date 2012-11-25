package applet.videoscreen;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import libsidplay.C64;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import applet.JSIDPlay2;

public abstract class VirtualKeyboard {

	public class TranspButton extends JButton {
		float transparency = 0.5f;

		@Override
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D) g;
			Composite alpha = AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER, transparency);
			g2.setComposite(alpha);
			super.paintComponent(g);
		}

		public void setTransparency(int value) {
			transparency = value / 100f;
		}
	}

	protected static class Key {
		private String petscii;
		private KeyTableEntry entry;
		private int width;

		public String getPetscii() {
			return petscii;
		}

		public int getWidth() {
			return width;
		}

		public KeyTableEntry getEntry() {
			return entry;
		}

		public void setEntry(KeyTableEntry entry) {
			this.entry = entry;
		}

		public Key(String thePetscii, KeyTableEntry theEntry, int theWidth) {
			petscii = thePetscii;
			entry = theEntry;
			width = theWidth;
		}
	}

	/**
	 * Custom C64 font resource name.
	 * 
	 * http://style64.org/c64-truetype/petscii-rom-mapping
	 */
	private static final String FONT_NAME = "fonts/C64_Elite_Mono_v1.0-STYLE.ttf";
	/**
	 * Font size.
	 */
	private static final float FONT_SIZE = 10f;
	/**
	 * Upper case letters.
	 */
	protected static final int TRUE_TYPE_FONT_BIG = 0xe000;
	/**
	 * Lower case letters.
	 */
	protected static final int TRUE_TYPE_FONT_SMALL = 0xe100;
	/**
	 * Inverse Upper case letters.
	 */
	protected static final int TRUE_TYPE_FONT_INVERSE_BIG = 0xe200;
	/**
	 * Inverse Lower case letters.
	 */
	protected static final int TRUE_TYPE_FONT_INVERSE_SMALL = 0xe300;

	/**
	 * Current font set.
	 */
	protected int fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;

	private Font cbmFont;
	{
		try {
			// Use custom CBM font
			InputStream fontStream = JSIDPlay2.class
					.getResourceAsStream(FONT_NAME);
			if (fontStream != null) {
				cbmFont = Font.createFont(Font.TRUETYPE_FONT, fontStream)
						.deriveFont(Font.PLAIN, FONT_SIZE);
			}
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public TranspButton createKey(final C64 c64, int posX, int posY,
			final int width, final int height, final String petscii,
			final KeyTableEntry key, final JSlider slider) {
		final TranspButton button = new TranspButton();
		button.setText(print(petscii, getFontSet()));
		button.setRequestFocusEnabled(false);
		button.setOpaque(false);
		button.setBounds(posX, posY, width, height);
		button.setBorder(BorderFactory.createLoweredBevelBorder());
		button.setVisible(false);
		button.setFocusable(false);
		button.setContentAreaFilled(true);
		button.setFont(cbmFont);
		button.setRolloverEnabled(false);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				button.setTransparency(slider.getValue());
			}
		});
		button.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				if (key == KeyTableEntry.RESTORE) {
					c64.getKeyboard().restore();
				} else {
					c64.getEventScheduler().scheduleThreadSafe(
							new Event("Virtual Keyboard Key Pressed") {
								@Override
								public void event() throws InterruptedException {
									c64.getKeyboard().keyPressed(key);
								}
							});
					c64.getEventScheduler().scheduleThreadSafe(
							new Event("Virtual Keyboard Key Released") {
								@Override
								public void event() throws InterruptedException {
									c64.getKeyboard().keyReleased(key);
								}
							});
				}
			}
		});
		return button;
	}

	private String print(final String s, int fontSet) {
		StringBuffer buf = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			buf.append(print(s.charAt(i), fontSet));
		}
		return buf.toString();
	}

	protected String print(final char c, int fontSet) {
		return String.valueOf((char) (c | fontSet));
	}

	/**
	 * @return the fontSet
	 */
	protected int getFontSet() {
		return TRUE_TYPE_FONT_BIG;
	}

	public abstract boolean isVisible();

	public abstract void setVisible(boolean b);

	public abstract Rectangle createUI(Container parent, C64 c64, JSlider slider);

}
