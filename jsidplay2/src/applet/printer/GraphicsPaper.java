package applet.printer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import libsidplay.components.printer.IPaper;

/**
 * Prototype of a graphical printer output.
 * 
 * @author Ken
 *
 */
public class GraphicsPaper extends JPanel implements IPaper {

	public class PixelRow {
		boolean[] rowData = new boolean[480];
	}

	ArrayList<PixelRow> dots = new ArrayList<PixelRow>();
	private int x;
	private DrawingPane drawingPane;

	public GraphicsPaper() {
		super(new BorderLayout());

		// Set up the drawing area.
		drawingPane = new DrawingPane();
		drawingPane.setBackground(Color.WHITE);

		// Put the drawing area in a scroll pane.
		JScrollPane scroller = new JScrollPane(drawingPane);
		scroller.setPreferredSize(new Dimension(960, 480));

		add(scroller, BorderLayout.CENTER);

		dots.add(new PixelRow());
		x = 0;

		setOpaque(true);
	}

	/** The component inside the scroll pane. */
	public class DrawingPane extends JPanel {
		public DrawingPane() {
			setIgnoreRepaint(true);
			setDoubleBuffered(false);
		}
		@Override
		public void paintComponent(final Graphics g) {
			super.paintComponent(g);
			int y = 0;
			synchronized (dots) {
				for (PixelRow row : dots) {
					for (int i = 0; i < row.rowData.length; i++) {
						int x1 = i << 1;
						int y1 = y << 1;
						if (row.rowData[i]) {
							g.setColor(Color.BLACK);
							g.drawLine(x1, y1, x1 + 1, y1);
							g.drawLine(x1, y1 + 1, x1 + 1, y1 + 1);
						} else {
							g.setColor(Color.WHITE);
							g.drawLine(x1, y1, x1 + 1, y1);
							g.drawLine(x1, y1 + 1, x1 + 1, y1 + 1);
						}
					}
					y++;
				}
			}
		}

	}

	@Override
	public void open() {
	}

	@Override
	public void put(Outputs out) {
		synchronized (dots) {
			switch (out) {
			case OUTPUT_NEWLINE:
				// newline
				dots.add(new PixelRow());
				x = 0;
				break;
			case OUTPUT_PIXEL_BLACK:
				// black pixel
				if (x < 480)
					dots.get(dots.size() - 1).rowData[x] = true;
				x++;
				break;
			default:
				// white pixel
				if (x < 480)
					dots.get(dots.size() - 1).rowData[x] = false;
				x++;
				break;
			}
		}
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				// Update client's preferred size because
				// the area taken up by the graphics has
				// gotten larger or smaller (if cleared).
				getDrawingPane().setPreferredSize(new Dimension(960,
						dots.size() << 1));

				// Let the scroll pane know to update itself
				// and its scrollbars.
				getDrawingPane().revalidate();
				getDrawingPane().repaint();
			}
		});
	}

	@Override
	public void close() {
	}

	public DrawingPane getDrawingPane() {
		return drawingPane;
	}
}
