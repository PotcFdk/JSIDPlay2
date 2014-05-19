package ui.favorites;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import libsidplay.Player;
import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;

public class FavoritesCellFactory implements
		Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	private static final Image STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	private static final Image NO_STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private Player player;
	private ObjectProperty<HVSCEntry> currentlyPlayedHVSCEntryProperty;

	public void setPlayer(Player player) {
		this.player = player;
	}

	public void setCurrentlyPlayedHVSCEntryProperty(
			ObjectProperty<HVSCEntry> currentlyPlayedHVSCEntryProperty) {
		this.currentlyPlayedHVSCEntryProperty = currentlyPlayedHVSCEntryProperty;
	}

	protected final class TableCellImpl extends TableCell<HVSCEntry, Object> {

		private ChangeListener<HVSCEntry> listener = (observable, oldValue,
				newValue) -> setCellStyle();

		public TableCellImpl() {
			currentlyPlayedHVSCEntryProperty.addListener(listener);
		}

		@Override
		protected void updateItem(Object value, boolean empty) {
			super.updateItem(value, empty);
			if (!empty && value != null) {
				setText(value.toString());
				if (getTableView().getColumns().indexOf(getTableColumn()) == 0) {
					if (player.getStilEntry(getPath()) != null) {
						setGraphic(new ImageView(STIL_ICON));
					} else {
						setGraphic(new ImageView(NO_STIL_ICON));
					}
				}
			} else {
				setText(null);
				setGraphic(null);
			}
			setCellStyle();
		}

		private String getPath() {
			HVSCEntry hvscEntry = getHVSCEntry();
			return hvscEntry != null ? hvscEntry.getPath() : null;
		}

		private HVSCEntry getHVSCEntry() {
			if (getTableRow() != null) {
				return (HVSCEntry) getTableRow().getItem();
			}
			return null;
		}

		private void setCellStyle() {
			getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
			if (!isEmpty()
					&& getHVSCEntry() == currentlyPlayedHVSCEntryProperty.get()) {
				getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
			}
		}

	}

	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		return new TableCellImpl();
	}

}
