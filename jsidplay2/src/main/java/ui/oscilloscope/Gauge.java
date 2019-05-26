package ui.oscilloscope;

import java.net.URL;
import java.util.Arrays;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.TitledPane;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.common.UIPart;

public class Gauge extends C64VBox implements UIPart {
	protected static final Color[] gaugeColors = new Color[256];

	static {
		for (int i = 0; i < gaugeColors.length; i++) {
			int color = Math.round((float) Math.sqrt(i / 255f) * 255f);
			gaugeColors[i] = new Color((color >> 8) / 255., (color & 0xff) / 255., 0, 1.);
		}
	}

	private int width;
	private int height;

	private String text;
	private int voice;

	public Gauge() {
	}

	public Gauge(C64Window window, Player player) {
		super(window, player);
	}

	@Override
	protected void initialize() {
		width = (int) getArea().getWidth();
		height = (int) getArea().getHeight();
	}

	/** data plots normalized between -1 .. 1 */
	private float[] dataMin = new float[256];
	/** data plots normalized between -1 .. 1 */
	private float[] dataMax = new float[256];
	/** Position within data buffer */
	private int dataPos = 0;

	protected ImageQueue imageQueue;

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
		imageQueue = new ImageQueue();
		addImage(null);
		updateGauge(null);
	}

	public void updateGauge(SIDEmu sidemu) {
		Image image = imageQueue.poll();
		if (getTitledPane() == null) {
			return;
		}
		getTitledPane().setText(text);

		GraphicsContext g = getArea().getGraphicsContext2D();

		if (image != null) {
			g.clearRect(0, 0, width, height);
			g.drawImage(image, 0, 0);
		}
	}

	public void addImage(SIDEmu sidemu) {
		WritableImage image = new WritableImage(width, height);
		PixelWriter pixelWriter = image.getPixelWriter();
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
				drawLine(pixelWriter, x, intStartPos, x, intStartPos, gaugeColors[shade]);
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
			drawLine(pixelWriter, x, intStartPos, x, intStartPos, gaugeColors[(int) (shade * firstPixel)]);

			drawLine(pixelWriter, x, intEndPos, x, intEndPos, gaugeColors[(int) (shade * lastPixel)]);

			intStartPos += 1;
			intEndPos -= 1;

			/* Still space for the middle line? */
			if (intStartPos <= intEndPos) {
				drawLine(pixelWriter, x, intStartPos, x, intEndPos, gaugeColors[shade]);
			}
		}
		imageQueue.add(image);
	}

	private void drawLine(PixelWriter pixelWriter, int x1, int y1, int x2, int y2, Color c) {
		x1 = (int) Math.min(Math.max(0, x1), width - 1);
		x2 = (int) Math.min(Math.max(0, x2), width - 1);
		y1 = (int) Math.min(Math.max(0, y1), height - 1);
		y2 = (int) Math.min(Math.max(0, y2), height - 1);
		// delta of exact value and rounded value of the dependent variable
		int d = 0;

		int dx = Math.abs(x2 - x1);
		int dy = Math.abs(y2 - y1);

		int dx2 = 2 * dx; // slope scaling factors to
		int dy2 = 2 * dy; // avoid floating point

		int ix = x1 < x2 ? 1 : -1; // increment direction
		int iy = y1 < y2 ? 1 : -1;

		int x = x1;
		int y = y1;

		if (dx >= dy) {
			while (true) {
				pixelWriter.setColor(x, y, c);
				if (x == x2)
					break;
				x += ix;
				d += dy2;
				if (d > dx) {
					y += iy;
					d -= dx2;
				}
			}
		} else {
			while (true) {
				pixelWriter.setColor(x, y, c);
				if (y == y2)
					break;
				y += iy;
				d += dx2;
				if (d > dy) {
					x += ix;
					d -= dy2;
				}
			}
		}
	}

	protected TitledPane getTitledPane() {
		return null;
	}

	protected Canvas getArea() {
		return null;
	}

	protected ImageQueue getImageQueue() {
		return imageQueue;
	}

	@Override
	public void doClose() {
		// just in case to not waste ram with frames!
		imageQueue.dispose();
	}
}
