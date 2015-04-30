package ui.oscilloscope;

import java.net.URL;
import java.util.Arrays;

import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import libsidplay.Player;
import libsidplay.common.SIDEmu;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class Gauge extends VBox implements UIPart {
	protected static final Color[] gaugeColors = new Color[256];

	static {
		for (int i = 0; i < gaugeColors.length; i++) {
			int color = Math.round((float) Math.sqrt(i / 255f) * 255f);
			gaugeColors[i] = new Color((color >> 8) / 255.,
					(color & 0xff) / 255., 0, 1.);
		}
	}

	private UIUtil util;

	private String text;
	private int voice;

	public Gauge(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		getChildren().add((Node) util.parse());
	}

	/** data plots normalized between -1 .. 1 */
	private float[] dataMin = new float[256];
	/** data plots normalized between -1 .. 1 */
	private float[] dataMax = new float[256];
	/** Position within data buffer */
	private int dataPos = 0;

	@Override
	public String getBundleName() {
		return Gauge.class.getName();
	}

	@Override
	public URL getFxml() {
		return getClass().getResource(Gauge.class.getSimpleName() + ".fxml");
	}

	public int getVoice() {
		return voice;
	}

	public void setVoice(int voice) {
		this.voice = voice;
	}

	public String getText() {
		return text;
	}

	public void setText(final String text) {
		this.text = text;
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

	public void reset() {
		Arrays.fill(dataMin, (byte) 0);
		Arrays.fill(dataMax, (byte) 0);
		dataPos = 0;
		updateGauge(null);
	}

	public void updateGauge(SIDEmu sidemu) {
		getTitledPane().setText(text);

		GraphicsContext g = getArea().getGraphicsContext2D();
		final int width = (int) getArea().getWidth();
		final int height = (int) getArea().getHeight();

		g.setStroke(Color.BLACK);
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
				g.setStroke(gaugeColors[shade]);
				g.strokeLine(x, intStartPos, x, intStartPos);
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
			g.setStroke(gaugeColors[(int) (shade * firstPixel)]);
			g.strokeLine(x, intStartPos, x, intStartPos);

			g.setStroke(gaugeColors[(int) (shade * lastPixel)]);
			g.strokeLine(x, intEndPos, x, intEndPos);

			intStartPos += 1;
			intEndPos -= 1;

			/* Still space for the middle line? */
			if (intStartPos <= intEndPos) {
				g.setStroke(gaugeColors[shade]);
				g.strokeLine(x, intStartPos, x, intEndPos);
			}
		}
	}

	protected void sample() {

	}

	protected TitledPane getTitledPane() {
		return null;
	}

	protected Canvas getArea() {
		return null;
	}

}
