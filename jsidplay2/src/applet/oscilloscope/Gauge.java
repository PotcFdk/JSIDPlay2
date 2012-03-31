package applet.oscilloscope;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

abstract class Gauge extends JPanel {
	protected static final Color[] gaugeColors = new Color[256];

	static {
		for (int i = 0; i < gaugeColors.length; i++) {
			int color = Math.round((float) Math.sqrt(i / 255f) * 255f);
			gaugeColors[i] = new Color(color << 8);
		}
	}

	/** data plots normalized between -1 .. 1 */
	protected float[] dataMin = new float[256];
	/** data plots normalized between -1 .. 1 */
	protected float[] dataMax = new float[256];
	/** Position within data buffer */
	protected int dataPos = 0;
	/** Our label */
	private final TitledBorder border;

	private final RenderArea area;

	private String oldLabel;

	protected class RenderArea extends JComponent {
		protected RenderArea() {
			setBackground(Color.BLACK);
			setOpaque(true);
			setIgnoreRepaint(true);
		}

		@Override
		public void paintComponent(final Graphics g) {
			final int width = getWidth();
			final int height = getHeight();

			g.setColor(Color.BLACK);
			g.fillRect(0, 0, width, height);

			int shade = 255;
			for (int x = 0; x < width; x++) {
				final int readPos = dataPos - width + x & dataMin.length - 1;

				float startPos = (1 - dataMax[readPos]) * (height - 1);
				if (startPos < 0) {
					startPos = 0;
				}

				float endPos = (1 - dataMin[readPos]) * (height - 1);
				if (endPos > height - 1) {
					endPos = height - 1;
				}

				int intStartPos = (int) Math.floor(startPos);
				int intEndPos = (int) Math.ceil(endPos);

				/* one pixel line? */
				if (intStartPos == intEndPos) {
					g.setColor(gaugeColors[shade]);
					g.drawLine(x, intStartPos, x, intStartPos);
					continue;
				}

				/* At least 1 pixel separating, calculate start and end colors */
				float firstPixel = startPos - intStartPos;
				firstPixel = 1 - firstPixel;
				if (firstPixel < 0) {
					firstPixel = 0;
				}
				if (firstPixel > 1) {
					firstPixel = 1;
				}

				float lastPixel = intEndPos - endPos;
				lastPixel = 1 - lastPixel;
				if (lastPixel < 0) {
					lastPixel = 0;
				}
				if (lastPixel > 1) {
					lastPixel = 1;
				}

				/* Draw end points */
				g.setColor(gaugeColors[(int) (shade * firstPixel)]);
				g.drawLine(x, intStartPos, x, intStartPos);

				g.setColor(gaugeColors[(int) (shade * lastPixel)]);
				g.drawLine(x, intEndPos, x, intEndPos);

				intStartPos += 1;
				intEndPos -= 1;

				/* Still space for the middle line? */
				if (intStartPos <= intEndPos) {
					g.setColor(gaugeColors[shade]);
					g.drawLine(x, intStartPos, x, intEndPos);
				}

			}
		}
	}

	public Gauge() {
		border = new TitledBorder("");
		oldLabel = null;
		setLayout(new GridLayout());
		setBorder(border);
		area = new RenderArea();
		add(area);
	}

	public void reset() {
		Arrays.fill(dataMin, (byte) 0);
		Arrays.fill(dataMax, (byte) 0);
		dataPos = 0;
	}

	protected void accumulate(float value) {
		if (value < dataMin[dataPos]) {
			dataMin[dataPos] = value;
		}
		if (value > dataMax[dataPos]) {
			dataMax[dataPos] = value;
		}
	}

	protected void advance() {
		final float min = dataMin[dataPos];
		final float max = dataMax[dataPos];
		dataPos = dataPos + 1 & dataMin.length - 1;
		dataMin[dataPos] = max;
		dataMax[dataPos] = min;
	}

	public void setLabel(final String label) {
		if (label.equals(oldLabel)) {
			return;
		}
		oldLabel = label;
		border.setTitle(label);
		repaint();
	}

	public void updateGauge() {
		area.repaint();
	}

	public abstract void sample();
}
