package ui.assembly64;

import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class ContentEntryRowFactory implements Callback<TableView<ContentEntry>, TableRow<ContentEntry>> {
	@Override
	public TableRow<ContentEntry> call(final TableView<ContentEntry> p) {
		return new TableRow<ContentEntry>() {
			@Override
			public void updateItem(ContentEntry item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(item != null ? new Tooltip(getItem().getName()) : null);
			}
		};
	}
}
