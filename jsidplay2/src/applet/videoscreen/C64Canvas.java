/*
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation. For the full
 * license text, see http://www.gnu.org/licenses/gpl.html.
 */
package applet.videoscreen;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DirectColorModel;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import libsidplay.C64;
import libsidplay.components.mos656x.VIC;
import applet.JSIDPlay2;

/**
 * The actual Swing canvas that shows the C64 Screen.
 * 
 * @author Antti Lankila
 */
public class C64Canvas extends JLayeredPane {
	public static class VICDisplay extends JComponent implements
			PropertyChangeListener {
		private final VIC vic;
		private final MemoryImageSource memoryImageSource;
		private final Image screenImage, monitorImage;
		private int scale;
		protected ImageIcon c64Image;
		protected ColorModel cm;
		protected static final int[] RGB_MASKS = { 0xff0000, 0x00ff00, 0x0000ff };

		//
		// Hardcopy fields
		//

		/**
		 * Flag hardcopy function.
		 */
		private Boolean doHardCopy = new Boolean(false);
		/**
		 * Graphics file format.
		 */
		private String format;
		/**
		 * Desired graphics filename.
		 */
		private String outputName;
		/**
		 * Current graphics file counter.
		 */
		private static int outputFileCounter;

		protected VICDisplay(VIC vic) {
			this.vic = vic;
			vic.addPropertyChangeListener(this);

			cm = new DirectColorModel(24, RGB_MASKS[0], RGB_MASKS[1],
					RGB_MASKS[2]);
			memoryImageSource = new MemoryImageSource(vic.getBorderWidth(),
					vic.getBorderHeight(), cm, vic.getPixels(), 0,
					vic.getBorderWidth());
			memoryImageSource.setAnimated(true);
			memoryImageSource.setFullBufferUpdates(true);
			screenImage = createImage(memoryImageSource);

			URL resource = JSIDPlay2.class.getResource("icons/monitor.png");
			if (resource != null) {
				monitorImage = new ImageIcon(resource).getImage();
			} else {
				monitorImage = null;
			}
		}

		/**
		 * Use the largest integer fraction that will just fill this area
		 * 
		 * @param width
		 *            width of container
		 * @param height
		 *            height of container
		 */
		protected void calculateNewSize(int width, int height) {
			int xscale = width / vic.getBorderWidth();
			int yscale = height / vic.getBorderHeight();
			scale = Math.max(Math.min(xscale, yscale), 1);
			setSize(new Dimension(vic.getBorderWidth() * scale,
					vic.getBorderHeight() * scale));
			setPreferredSize(new Dimension(vic.getBorderWidth() * scale,
					vic.getBorderHeight() * scale));
		}

		@Override
		public void paintComponent(final Graphics g) {
			// Sigh. Can't really afford this stuff for memory images.
			// ((Graphics2D)
			// g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			// RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			if (c64Image == null) {
				g.drawImage(screenImage, 35 * scale, 25 * scale, getWidth()
						- 70 * scale, getHeight() - 58 * scale, this);
				g.drawImage(monitorImage, 0, 0, getWidth(), getHeight(), this);
			} else {
				g.drawImage(c64Image.getImage(), 0, 0, getWidth(), getHeight(),
						this);
			}
		}

		@Override
		public void propertyChange(final PropertyChangeEvent evt) {
			if ("pixels".equals(evt.getPropertyName())) {
				memoryImageSource.newPixels();
				// Create a hardcopy of the Video screen, eventually
				synchronized (doHardCopy) {
					if (doHardCopy.booleanValue()) {
						doHardCopy = new Boolean(false);
						BufferedImage bufferedImage = new BufferedImage(
								vic.getBorderWidth(), vic.getBorderHeight(),
								BufferedImage.TYPE_INT_RGB);
						// Paint the image onto the buffered image
						Graphics g = bufferedImage.createGraphics();
						g.drawImage(screenImage, 0, 0, null);
						g.dispose();
						try {
							ImageIO.write(bufferedImage, format, new File(
									outputName));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		public Image getScreenImage() {
			return screenImage;
		}

		public void setC64Image(ImageIcon c64Image) {
			this.c64Image = c64Image;
		}

		/**
		 * Save a hardcopy of the Video screen.
		 * 
		 * @param format
		 *            graphics format (e.g. "bmp", "gif", "jpg", "jpeg", "png"
		 *            or "wbmp")
		 * @param output
		 *            output filename of the graphics file
		 * @throws IOException
		 *             error writing image
		 */
		public void hardCopy(final String format, String outputName)
				throws IOException {
			synchronized (doHardCopy) {
				this.doHardCopy = new Boolean(true);
				this.format = format;
				// Add default file extension, if missing
				outputName += "." + format;
				int i = outputName.lastIndexOf('.');
				// Filename: name + <counter> + .extension
				this.outputName = outputName.substring(0, i)
						+ (++outputFileCounter) + outputName.substring(i);
			}
		}
	}

	protected VICDisplay screenCanvas;
	private VirtualKeyboard normalKeyboard, shiftedKeyboard, commodoreKeyboard,
			currentKeyboard;

	protected int opacity = 50;

	public C64Canvas() {
		setLayout(null);
		addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentResized(ComponentEvent e) {
				if (screenCanvas != null) {
					screenCanvas.calculateNewSize(getWidth(), getHeight());
				}
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
	}

	public VICDisplay getScreenCanvas() {
		return screenCanvas;
	}

	public void setupVideoScreen(final C64 c64) {
		removeAll();
		screenCanvas = new VICDisplay(c64.getVIC());
		screenCanvas.calculateNewSize(getWidth(), getHeight());
		add(screenCanvas);

		keyboardFrame = new JInternalFrame(
				"Virtual Keyboard (Normal-Key Layout)");
		keyboardFrame.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentHidden(ComponentEvent e) {
				// otherwise keyboard does not work anymore!
				C64Canvas.this.requestFocus();
			}
		});
		URL url = C64Canvas.class
				.getResource("/applet/icons/commodore_logo.png");
		if (url != null) {
			keyboardFrame.setFrameIcon(new ImageIcon(url));
		}
		keyboardFrame.setOpaque(false);
		keyboardFrame.setLayout(null);
		keyboardFrame.setVisible(false);
		keyboardFrame.setClosable(true);
		final JSlider slider = new JSlider(JSlider.HORIZONTAL, 0, 100, opacity);
		slider.setOpaque(false);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				keyboardFrame.setBackground(new Color(0, 0, 0, slider
						.getValue()));
				opacity = slider.getValue();
			}
		});
		keyboardFrame.add(slider);

		keyboardFrame.setBackground(new Color(0, 0, 0, slider.getValue()));
		keyboardFrame.setBorder(BorderFactory.createLoweredBevelBorder());
		add(keyboardFrame, JLayeredPane.POPUP_LAYER);

		currentKeyboard = normalKeyboard = new NormalKeyBoard();
		normalKeyboard.createUI(keyboardFrame, c64, slider);
		shiftedKeyboard = new ShiftedKeyBoard();
		Rectangle bounds = shiftedKeyboard.createUI(keyboardFrame, c64, slider);
		slider.setBounds(0, 0, bounds.width - 20,
				slider.getMinimumSize().height);
		commodoreKeyboard = new CommodoreKeyBoard();
		commodoreKeyboard.createUI(keyboardFrame, c64, slider);
	}

	private int mode;
	protected JInternalFrame keyboardFrame;

	public void switchKeyboard() {
		if (!keyboardFrame.isVisible()) {
			mode = 3;
		}
		keyboardFrame.setVisible(false);
		currentKeyboard.setVisible(false);
		mode = (mode + 1) % 4;
		switch (mode) {
		case 0:
			currentKeyboard = normalKeyboard;
			keyboardFrame.setVisible(true);
			currentKeyboard.setVisible(true);
			keyboardFrame.setTitle("Virtual Keyboard (Normal-Key Layout)");
			break;
		case 1:
			currentKeyboard = shiftedKeyboard;
			keyboardFrame.setVisible(true);
			currentKeyboard.setVisible(true);
			keyboardFrame.setTitle("Virtual Keyboard (Shifted-Key Layout)");
			break;
		case 2:
			currentKeyboard = commodoreKeyboard;
			keyboardFrame.setVisible(true);
			currentKeyboard.setVisible(true);
			keyboardFrame.setTitle("Virtual Keyboard (Commodore-Key Layout)");
			break;
		default:
			mode = 0;
			keyboardFrame.setVisible(false);
			currentKeyboard.setVisible(false);
			currentKeyboard = normalKeyboard;
			keyboardFrame.setTitle("Virtual Keyboard (Normal-Key Layout)");
			break;
		}
	}

}
