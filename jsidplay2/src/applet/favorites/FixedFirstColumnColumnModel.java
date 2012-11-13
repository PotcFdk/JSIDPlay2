package applet.favorites;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableColumnModel;

public class FixedFirstColumnColumnModel extends DefaultTableColumnModel
		implements TableColumnModel {

	public FixedFirstColumnColumnModel(final FavoritesModel favoritesModel) {
		addColumnModelListener(new TableColumnModelListener() {

			@Override
			public void columnSelectionChanged(ListSelectionEvent arg0) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent arg0) {
			}

			@Override
			public void columnMoved(TableColumnModelEvent event) {
				favoritesModel.moveColumn(event.getFromIndex(),
						event.getToIndex());
			}

			@Override
			public void columnMarginChanged(ChangeEvent arg0) {
			}

			@Override
			public void columnAdded(TableColumnModelEvent arg0) {
			}
		});

	}

	@Override
	public void moveColumn(int columnIndex, int newIndex) {
		if (columnIndex != 0 && newIndex != 0) {
			super.moveColumn(columnIndex, newIndex);
		}
	}

}
