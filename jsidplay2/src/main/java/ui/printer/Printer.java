package ui.printer;

import java.util.Arrays;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.paint.Color;
import libsidplay.components.printer.IPaper;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.components.printer.paper.ConsolePaper;
import sidplay.Player;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.UIUtil;

public class Printer extends Tab implements UIPart, IPaper {

	public static final String ID = "PRINTER";

	@FXML
	private ScrollPane scroll;
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
		paper.setWidth(MPS803.MAX_WIDTH);
		util.getPlayer().getPrinter().setPaper(this);
	}

	@Override
	public void doClose() {
		// set default paper
		util.getPlayer().getPrinter().setPaper(new ConsolePaper());
	}
	
	@FXML
	private void clearPaper() {
		paper.getGraphicsContext2D().clearRect(0, 0, paper.getWidth(), paper.getHeight());
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
			final boolean[] toPrint = Arrays.copyOf(currentPixelRow, MPS803.MAX_WIDTH);
			Arrays.fill(currentPixelRow, false);
			x = 0;
			int paperY = y;
			y++;
			Platform.runLater(() -> {
				GraphicsContext g = paper.getGraphicsContext2D();
				if (paperY >= paper.getHeight()) {
					// endless paper scroll
					paper.setTranslateY(paperY-paper.getHeight());
					paper.setHeight(paperY);
					scroll.setVvalue(1.0); 
				}
				for (int paperX = 0; paperX < toPrint.length; paperX++) {
					g.setStroke(toPrint[paperX] ? Color.BLACK : Color.WHITE);
					g.strokeLine(paperX, paperY, paperX, paperY);
				}
			});
			break;
		case OUTPUT_PIXEL_BLACK:
		case OUTPUT_PIXEL_WHITE:
		default:
			// black/white pixel
			if (x < MPS803.MAX_WIDTH)
				currentPixelRow[x] = out == Outputs.OUTPUT_PIXEL_BLACK;
			x++;
			break;
		}
	}

	@Override
	public void close() {
	}

}
