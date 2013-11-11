package ui.favorites;

import java.io.File;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import libsidutils.STIL;
import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;

public class FavoritesCellFactory implements
		Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	protected static final Image STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	protected static final Image NO_STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private static final String FILE_NOT_FOUND_ROW = "fileNotFoundRow";

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	@SuppressWarnings("rawtypes")
	protected final class TableCellImpl extends TableCell {
		@SuppressWarnings("unchecked")
		@Override
		public void updateItem(Object value, boolean empty) {
			super.updateItem(value, empty);
			if (!empty) {
				setText(value.toString());
				if (getTableRow() != null) {
					HVSCEntry hvscEntry = (HVSCEntry) getTableRow().getItem();
					if (hvscEntry != null && favoritesTab != null) {
						File file = favoritesTab.getFile(hvscEntry.getPath());
						getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
						getStyleClass().remove(FILE_NOT_FOUND_ROW);
						if (file == null || !file.exists()) {
							getStyleClass().add(FILE_NOT_FOUND_ROW);
						} else if (file.equals(favoritesTab
								.getCurrentlyPlayedFile())) {
							getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
						}
						int columnIndex = getTableView().getColumns().indexOf(
								getTableColumn());
						if (columnIndex == 0) {
							STIL stil = favoritesTab
									.getConsolePlayer().getStil();
							if (stil != null && stil.getSTILEntry(file) != null) {
								setGraphic(new ImageView(STIL_ICON));
							} else {
								setGraphic(new ImageView(NO_STIL_ICON));
							}
						}
					}
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		return new TableCellImpl();
	}

	protected FavoritesTab favoritesTab;

	public void setFavoritesTab(FavoritesTab favoritesTab) {
		this.favoritesTab = favoritesTab;
	}
}
