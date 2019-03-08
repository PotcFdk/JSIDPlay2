package ui.assembly64;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class Assembly64RowFactory implements Callback<TableView<SearchResult>, TableRow<SearchResult>> {
	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private ObjectProperty<SearchResult> currentlyPlayedRowProperty;

	public ObjectProperty<SearchResult> getCurrentlyPlayedRowProperty() {
		return currentlyPlayedRowProperty;
	}
	
	public void setCurrentlyPlayedRowProperty(ObjectProperty<SearchResult> currentlyPlayedRowProperty) {
		this.currentlyPlayedRowProperty = currentlyPlayedRowProperty;
	}

	@Override
	public TableRow<SearchResult> call(final TableView<SearchResult> p) {
		TableRow<SearchResult> tableRow = new TableRow<SearchResult>() {
			@Override
			public void updateItem(SearchResult item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(item != null ? new Tooltip(getItem().getName()) : null);

				getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
				if (!isEmpty() && currentlyPlayedRowProperty.get() != null
						&& item.getId().equals(currentlyPlayedRowProperty.get().getId())) {
					getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
				}
			}
		};
		currentlyPlayedRowProperty.addListener((observable, oldValue, newValue) -> tableRow.getTableView().refresh());
		return tableRow;
	}
}
