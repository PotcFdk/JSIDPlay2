package applet.collection;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class TuneInfoTableRenderer extends DefaultTableCellRenderer {
	
	private final Color white, gray;
	
	public TuneInfoTableRenderer() {
		/* make background visible */
		setOpaque(true);
		white = Color.WHITE;
		gray = new Color(0xeeeeee);
	}
	
	@Override
	public Component getTableCellRendererComponent(final JTable table,
			final Object data, final boolean isSelected, final boolean hasFocus, final int row,
			final int column) {

		setBackground((row % 2) == 0 ? white : gray);
		
		setToolTipText(String.valueOf(table.getModel().getValueAt(row, column)));
		return super.getTableCellRendererComponent(table, data, isSelected,
				hasFocus, row, column);
	}
}