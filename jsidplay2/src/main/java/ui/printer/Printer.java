package ui.printer;

import java.util.Arrays;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import libsidplay.Player;
import libsidplay.components.printer.IPaper;
import libsidplay.components.printer.mps803.MPS803;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class Printer extends Tab implements UIPart, IPaper {

	public static final String ID = "PRINTER";

	@FXML
	protected Canvas paper;

	private UIUtil util;

	private boolean[] currentPixelRow = new boolean[MPS803.MAX_WIDTH];
	private int x, y;

	public Printer(C64Window window, Player player) {
		util = new UIUtil(window, player, this);
		setContent((Node) util.parse());
		setId(ID);
		setText(util.getBundle().getString(getId()));
	}

	@FXML
	private void initialize() {
		// paper.setScaleX(2);
		// paper.setScaleY(2);
		paper.setWidth(MPS803.MAX_WIDTH);
		util.getPlayer().getPrinter().setPaper(this);
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
				Platform.runLater(() -> {
					GraphicsContext g = paper.getGraphicsContext2D();
					for (int paperX = 0; paperX < toPrint.length; paperX++) {
						g.setStroke(toPrint[paperX] ? Color.BLACK : Color.WHITE);
						g.strokeLine(paperX, paperY, paperX, paperY);
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
