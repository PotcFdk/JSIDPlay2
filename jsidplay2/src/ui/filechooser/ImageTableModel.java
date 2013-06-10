package ui.filechooser;

import javax.swing.table.DefaultTableModel;

public class ImageTableModel extends DefaultTableModel {
	private String name;

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public String getColumnName(int column) {
		return name != null ? name : "";
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}

	public void setColumnName(int column, String string) {
		name = string;
	}
}
