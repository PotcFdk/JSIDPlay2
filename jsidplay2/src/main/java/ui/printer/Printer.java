package ui.printer;

import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import libsidplay.components.printer.IPaper;
import libsidplay.components.printer.mps803.MPS803;
import ui.common.C64Tab;

public class Printer extends C64Tab implements IPaper {

	@FXML
	private Canvas paper;

	private boolean[] currentPixelRow = new boolean[MPS803.MAX_WIDTH];
	private int x, y;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		if (getPlayer() == null) {
			// wait for second initialization, where properties have been set!
			return;
		}
		// paper.setScaleX(2);
		// paper.setScaleY(2);
		paper.setWidth(MPS803.MAX_WIDTH);
		getPlayer().getPrinter().setPaper(this);
	}

	@FXML
	private void clearPaper() {
		paper.getGraphicsContext2D().clearRect(0, 0, paper.getWidth(),
				paper.getHeight());
		y = 0;
	}

	@Override
	public void open() {
	}

	@Override
	public void put(Outputs out) {
		switch (out) {
		case OUTPUT_NEWLINE:
			// newline
			final boolean[] toPrint = Arrays.copyOf(currentPixelRow,
					MPS803.MAX_WIDTH);
			Arrays.fill(currentPixelRow, false);
			x = 0;
			final int paperY = y;
			y++;
			if (paperY < paper.getHeight()) {
				Platform.runLater(new Runnable() {

					@Override
					public void run() {
						GraphicsContext g = paper.getGraphicsContext2D();
						for (int paperX = 0; paperX < toPrint.length; paperX++) {
							g.setStroke(toPrint[paperX] ? Color.BLACK
									: Color.WHITE);
							g.strokeLine(paperX, paperY, paperX, paperY);
						}
					}
				});
			}
			break;
		case OUTPUT_PIXEL_BLACK:
			// black pixel
			if (x < MPS803.MAX_WIDTH)
				currentPixelRow[x] = true;
			x++;
			break;
		default:
			// white pixel
			if (x < MPS803.MAX_WIDTH)
				currentPixelRow[x] = false;
			x++;
			break;
		}
	}

	@Override
	public void close() {
	}

}
