package applet.collection;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

public class TuneInfoTable extends JTable {
	public TuneInfoTable(TableModel dm) {
		super(dm);
		setTableHeader(null);
	}
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		return new TuneInfoTableRenderer();
	}
}
