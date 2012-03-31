package applet.gamebase;

import java.awt.event.KeyEvent;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

public class GameBaseTable extends JTable {
	public GameBaseTable(TableModel dm) {
		final GameBaseColumnModel columnModel = new GameBaseColumnModel();
		setColumnModel(columnModel);
		setModel(dm);
		for (int j = 1; j <= GameBaseTableModel.COLUMN_COUNT - 1; j++) {
			TableColumn column = columnModel.getColumnByModelIndex(j);
			columnModel.setColumnVisible(column, false);
		}
		TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(
				dataModel);
		setRowSorter(rowSorter);
		getSelectionModel().setSelectionMode(
				ListSelectionModel.SINGLE_SELECTION);
		// disable enter key behavior
		getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
				KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
	}
}
