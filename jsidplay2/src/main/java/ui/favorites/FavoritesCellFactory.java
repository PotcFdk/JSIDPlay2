package ui.favorites;

import java.io.File;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;

public class FavoritesCellFactory implements Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	private static final Image STIL_ICON = new Image(JSIDPlay2Main.class.getResource("icons/stil.png").toString());

	private static final Image NO_STIL_ICON = new Image(JSIDPlay2Main.class.getResource("icons/stil_no.png").toString());

	private static final String FILE_NOT_FOUND_ROW = "fileNotFoundRow";

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private FavoritesTab favoritesTab;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		final TableCell<HVSCEntry, ?> cell = new TableCell() {

			@Override
			public void updateItem(Object value, boolean empty) {
				super.updateItem(value, empty);
				if (!empty) {
					setText(value.toString());
					if (getTableRow() != null) {
						HVSCEntry hvscEntry = (HVSCEntry) getTableRow().getItem();
						if (hvscEntry != null && favoritesTab != null) {
							File file = favoritesTab.getFile(hvscEntry.getPath());
							if (file != null) {
								getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
								getStyleClass().remove(FILE_NOT_FOUND_ROW);
								if (file.equals(favoritesTab.getCurrentlyPlayedFile())) {
									getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
								}
								if (!file.exists()) {
									getStyleClass().add(FILE_NOT_FOUND_ROW);
								}
							}
							int columnIndex = column.getTableView().getColumns().indexOf(column);
							if (columnIndex == 0) {
								if (favoritesTab.getStilEntry(hvscEntry.getPath()) != null) {
									setGraphic(new ImageView(STIL_ICON));
								} else {
									setGraphic(new ImageView(NO_STIL_ICON));
								}
							}
						}
					}
				}
			}
		};
		return cell;
	}

	public void setFavoritesTab(FavoritesTab favoritesTab) {
		this.favoritesTab = favoritesTab;
	}
}
