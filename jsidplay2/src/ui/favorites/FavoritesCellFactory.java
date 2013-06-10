package ui.favorites;

import java.io.File;

import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;

public class FavoritesCellFactory implements
		Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	private static final Image stilIcon = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	private static final Image noStilIcon = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private static final String FILE_NOT_FOUND_CELL_VALUE = "fileNotFoundCellValue";

	private Configuration config;

	public void setConfig(Configuration config) {
		this.config = config;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		final TableCell<HVSCEntry, ?> cell = new TableCell() {

			@Override
			public void updateItem(Object value, boolean empty) {
				super.updateItem(value, empty);
				int columnIndex = column.getTableView().getColumns()
						.indexOf(column);
				if (!empty) {
					String text = value.toString();
					setText(text);
					if (config != null) {
						if (columnIndex == 0) {
							File file = FavoritesTab.getFile(config, text);
							if (!file.exists()
									&& !getStyleClass().contains(
											FILE_NOT_FOUND_CELL_VALUE)) {
								getStyleClass().add(FILE_NOT_FOUND_CELL_VALUE);
							}
							if (FavoritesTab.getStilEntry(config, text) != null) {
								setGraphic(new ImageView(stilIcon));
							} else {
								setGraphic(new ImageView(noStilIcon));
							}
						}
					}
				}
			}
		};
		return cell;
	}
}
