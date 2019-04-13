package ui.assembly64;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ZeroIgnoringCellFactory
		implements Callback<TableColumn<SearchResult, Integer>, TableCell<SearchResult, Integer>> {

	@Override
	public TableCell<SearchResult, Integer> call(TableColumn<SearchResult, Integer> arg0) {
		return new TableCell<SearchResult, Integer>() {

			@Override
			protected void updateItem(Integer item, boolean empty) {
				super.updateItem(item, empty);

				if (empty || item == null) {
					item = Integer.valueOf(0);
				}
				setText(item.equals(0) ? "" : String.valueOf(item));
			}
		};
	}

}
