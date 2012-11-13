package applet.emulationsettings;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JPanel;

import resid_builder.resid.FilterModelConfig;
import sidplay.ini.intf.IFilterSection;
import applet.entities.config.Configuration;
import applet.events.IChangeFilter;
import applet.events.UIEvent;
import applet.events.UIEventListener;

public class CurveFit extends JPanel implements UIEventListener {
	protected Configuration config;

	private static final int XMIN = 0;
	private static final int XMAX = 2048;
	private static final int YMIN = 200;
	private static final int YMAX = 20000;

	private final Font fLabelFont = new Font("Courier",
			Font.BOLD | Font.ITALIC, 18);
	protected IFilterSection filter;

	protected final int distance(final int x1, final int y1, final int x2,
			final int y2) {
		final int dx = x1 - x2;
		final int dy = y1 - y2;
		return (int) (Math.sqrt(dx * dx + dy * dy) + 0.5);
	}

	public CurveFit() {
	}

	public final void setConfig(Configuration cfg) {
		this.config = cfg;
	}

	@Override
	protected void paintComponent(final Graphics g) {
		super.paintComponent(g);

		final Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);

		{
			g2.setPaint(new Color(0, 0, 0, 255));
			Point from = viewtransform(XMIN, YMIN);
			Point to = viewtransform(XMAX, YMIN);
			g2.drawLine(from.x, from.y, to.x, to.y);
			from = to;
			to = viewtransform(XMAX, YMAX);
			g2.drawLine(from.x, from.y, to.x, to.y);
			from = to;
			to = viewtransform(XMIN, YMAX);
			g2.drawLine(from.x, from.y, to.x, to.y);
			from = to;
			to = viewtransform(XMIN, YMIN);
			g2.drawLine(from.x, from.y, to.x, to.y);
		}

		for (int x = XMIN - XMIN % 128; x < XMAX; x += 128) {
			final Point from = viewtransform(x, YMIN);
			final Point to = viewtransform(x, YMAX);
			g2.setPaint(x % 512 == 0 ? new Color(32, 32, 32, 255) : new Color(
					192, 192, 192, 255));
			g2.drawLine(from.x, from.y, to.x, to.y);
			if (x % 512 == 0) {
				g2.setPaint(new Color(32, 32, 32, 255));
				g2.drawString("" + x, from.x + 1, from.y - 1);
			}
		}

		for (int y = YMIN; y < YMAX; y += findStep(y)) {
			final Point from = viewtransform(0, y);
			final Point to = viewtransform(XMAX, y);
			g2.setPaint(("" + y).matches("10+") ? new Color(32, 32, 32, 255)
					: new Color(192, 192, 192, 255));
			g2.drawLine(from.x, from.y, to.x, to.y);
			if (("" + y).matches("[1357]0+")) {
				g2.setPaint(new Color(32, 32, 32, 255));
				g2.drawString("" + y, from.x + 1, from.y - 1);
			}
		}

		if (filter != null) {
			g2.setPaint(new Color(128, 0, 128, 255));

			if (filter.getFilter6581CurvePosition() != 0) {
				double dacZero = FilterModelConfig.getDacZero(filter
						.getFilter6581CurvePosition());
				Point l = viewtransform(0,
						FilterModelConfig.estimateFrequency(dacZero, 0));
				for (int i = 1; i < 2048; i += 1) {
					Point n = viewtransform(i,
							FilterModelConfig.estimateFrequency(dacZero, i));
					g2.drawLine(l.x, l.y, n.x, n.y);
					l = n;
				}
			} else {
				Point l = viewtransform(0, 0);
				for (int i = 1; i < 2048; i += 1) {
					Point n = viewtransform(i,
							i * filter.getFilter8580CurvePosition() / 2047);
					g.drawLine(l.x, l.y, n.x, n.y);
					l = n;
				}
			}
		}

		final String xAxis = "FC value";
		final String yAxis = "Center frequency (Hz)";
		g2.setFont(fLabelFont);
		final FontMetrics fontMetrics = g.getFontMetrics();

		g2.setPaint(Color.black.darker());
		g2.drawString(yAxis, 10, 30);
		Rectangle bounds = fontMetrics.getStringBounds(xAxis, g).getBounds();
		g2.drawString(xAxis, (getWidth() - bounds.width - 10),
				(getHeight() - bounds.height));
	}

	private static int findStep(int y) {
		if (y < 100) {
			return 10;
		}
		if (y < 1000) {
			return 100;
		}
		return 1000;
	}

	protected final Point viewtransform(double x, double y) {
		x /= XMAX;
		x *= getWidth();

		y = logscale(y);
		y -= logscale(YMIN);
		y /= (logscale(YMAX) - logscale(YMIN)) / getHeight();

		return new Point((int) x, getHeight() - (int) y);
	}

	protected final double logscale(final double x) {
		if (x < 1) {
			return 0;
		}
		return Math.log10(x);
	}

	@Override
	public void notify(final UIEvent e) {
		if (e.isOfType(IChangeFilter.class)) {
			final IChangeFilter ifObj = (IChangeFilter) e.getUIEventImpl();
			final String filterName = ifObj.getFilterName();
			if ("".equals(filterName)) {
				filter = null;
			} else {
				List<? extends IFilterSection> filters = config.getFilter();
				for (IFilterSection iFilterSection : filters) {
					if (iFilterSection.getName().equals(filterName)) {
						filter = iFilterSection;
					}
				}
			}
			repaint();
		}
	}
}