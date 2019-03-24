package ui.assembly64;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;

public class ContentEntryRowFactory implements Callback<TableView<ContentEntry>, TableRow<ContentEntry>> {
	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private ObjectProperty<ContentEntry> currentlyPlayedContentEntryProperty;

	public ObjectProperty<ContentEntry> getCurrentlyPlayedContentEntryProperty() {
		return currentlyPlayedContentEntryProperty;
	}

	public void setCurrentlyPlayedContentEntryProperty(ObjectProperty<ContentEntry> currentlyPlayedHVSCEntryProperty) {
		this.currentlyPlayedContentEntryProperty = currentlyPlayedHVSCEntryProperty;
	}

	@Override
	public TableRow<ContentEntry> call(final TableView<ContentEntry> p) {
		TableRow<ContentEntry> tableRow = new TableRow<ContentEntry>() {
			@Override
			public void updateItem(ContentEntry item, boolean empty) {
				super.updateItem(item, empty);
				setTooltip(item != null ? new Tooltip(getItem().getDecodedName()) : null);

				getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
				if (!isEmpty() && currentlyPlayedContentEntryProperty.get() != null
						&& item.getId().equals(currentlyPlayedContentEntryProperty.get().getId())) {
					getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
				}
			}
		};
		currentlyPlayedContentEntryProperty
				.addListener((observable, oldValue, newValue) -> tableRow.getTableView().refresh());
		return tableRow;
	}
}
