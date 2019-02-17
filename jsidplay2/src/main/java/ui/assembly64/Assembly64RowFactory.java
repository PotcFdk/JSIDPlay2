package ui.assembly64;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class Assembly64RowFactory implements Callback<TableView<SearchResult>, TableRow<SearchResult>> {
	@Override
	public TableRow<SearchResult> call(final TableView<SearchResult> p) {
		return new TableRow<SearchResult>() {
			@Override
			public void updateItem(SearchResult item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(item != null ? new Tooltip(getItem().getName()) : null);
			}
		};
	}
}
