package applet.siddump;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

@SuppressWarnings("serial")
public class SIDDumpTableCellRenderer extends DefaultTableCellRenderer {

	private static final Color COLOR_TABLE[] = new Color[] { Color.LIGHT_GRAY,
			Color.GREEN, Color.LIGHT_GRAY, };

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (value.toString().startsWith("-")) {
			setBackground(Color.YELLOW);
		} else if (value.toString().startsWith("=")) {
			setBackground(Color.BLUE);
		} else {
			if (column == 0) {
				setBackground(Color.WHITE);
			} else if (column > 15) {
				setBackground(Color.WHITE);
			} else {
				int col = (column - 1) / 5;
				setBackground(COLOR_TABLE[col]);
			}
		}

		return super.getTableCellRendererComponent(table, value, isSelected,
				hasFocus, row, column);
	}

}
