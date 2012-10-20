package applet.collection;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JComponent;

public final class Picture extends JComponent {
	private Image composerImage;

	private int scale;

	{
		setOpaque(true);
		setPreferredSize(new Dimension(200, 200));
		addComponentListener(new ComponentListener() {
			@Override
			public void componentShown(ComponentEvent e) {
			}

			@Override
			public void componentResized(ComponentEvent e) {
				calculateNewSize(getWidth(), getHeight());
			}

			@Override
			public void componentMoved(ComponentEvent e) {
			}

			@Override
			public void componentHidden(ComponentEvent e) {
			}
		});
	}

	public final void setComposerImage(Image image) {
		composerImage = image;
		calculateNewSize(getWidth(), getHeight());
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
		scale = 1;
		if (composerImage != null) {
			int xscale = width / composerImage.getWidth(null);
			int yscale = height / composerImage.getHeight(null);
			scale = Math.max(Math.min(xscale, yscale), 1);
			setSize(new Dimension(composerImage.getWidth(null) * scale,
					composerImage.getHeight(null) * scale));
			setPreferredSize(new Dimension(
					composerImage.getWidth(null) * scale,
					composerImage.getHeight(null) * scale));
		}
	}

	@Override
	public void paint(Graphics g) {
		g.setColor(getBackground());
		g.fillRect(0, 0, getWidth(), getHeight());
		if (composerImage != null) {
			((Graphics2D) g).setRenderingHint(RenderingHints.KEY_INTERPOLATION,
					RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g.drawImage(composerImage, 0, 0, getWidth(), getHeight(), null);
		}
	}

	@Override
	public void update(Graphics g) {
		paint(g);
	}
}