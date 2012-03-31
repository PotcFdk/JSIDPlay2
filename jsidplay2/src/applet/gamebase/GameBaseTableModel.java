package applet.gamebase;

import javax.swing.table.DefaultTableModel;

public class GameBaseTableModel extends DefaultTableModel {
	public static final int COLUMN_COUNT = 11;

	public GameBaseTableModel() {
		super(1, COLUMN_COUNT);
	}
	
	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	@Override
	public String getColumnName(int column) {
		return "Games";
	}
}
