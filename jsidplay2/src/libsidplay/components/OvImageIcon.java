package libsidplay.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.ImageIcon;

public class OvImageIcon extends ImageIcon {

	private String imageName;
	private final Font overlayFont = new Font("Courier", Font.PLAIN, 12);

	public OvImageIcon(final URL resource) {
		super(resource);
	}

	public synchronized String getImageName() {
		return imageName;
	}

	public synchronized void setImageName(final String name) {
		imageName = name;
	}

	@Override
	public synchronized void paintIcon(final Component c, final Graphics g,
			final int x, final int y) {
		super.paintIcon(c, g, x, y);
		// Write text over image
		if (imageName != null) {
			// what image is attached?
			g.setColor(Color.BLUE);
			g.setFont(overlayFont);
			g.drawString(imageName, 6, 24);
		}
	}

}
