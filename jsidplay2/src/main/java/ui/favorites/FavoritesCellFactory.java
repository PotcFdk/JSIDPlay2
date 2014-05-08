package ui.favorites;

import java.io.File;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import libsidplay.Player;
import libsidutils.PathUtils;
import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.SidPlay2Section;

public class FavoritesCellFactory implements
		Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	private static final Image STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	private static final Image NO_STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private static final String FILE_NOT_FOUND_ROW = "fileNotFoundRow";

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private Player player;
	private ObjectProperty<File> currentlyPlayedFileProperty;

	public void setPlayer(Player player) {
		this.player = player;
	}

	public void setCurrentlyPlayedFileProperty(
			ObjectProperty<File> currentlyPlayedFileProperty) {
		this.currentlyPlayedFileProperty = currentlyPlayedFileProperty;
	}

	protected final class TableCellImpl extends TableCell<HVSCEntry, Object> {

		private File file;
		private ChangeListener<File> listener = (observable, oldValue, newValue) -> setCellStyle();

		public TableCellImpl() {
			currentlyPlayedFileProperty.addListener(listener);
		}

		@Override
		protected void updateItem(Object value, boolean empty) {
			super.updateItem(value, empty);
			if (!empty && value != null) {
				file = getFile();
				setText(value.toString());
				if (getTableView().getColumns().indexOf(getTableColumn()) == 0) {
					if (player.getStilEntry(file) != null) {
						setGraphic(new ImageView(STIL_ICON));
					} else {
						setGraphic(new ImageView(NO_STIL_ICON));
					}
				}
			} else {
				file = null;
				setText(null);
			}
			setCellStyle();
		}

		private File getFile() {
			if (getTableRow() != null) {
				HVSCEntry hvscEntry = (HVSCEntry) getTableRow().getItem();
				SidPlay2Section sidPlay2Section = (SidPlay2Section) player
						.getConfig().getSidplay2();
				return PathUtils.getFile(hvscEntry.getPath(),
						sidPlay2Section.getHvscFile(),
						sidPlay2Section.getCgscFile());
			}
			return null;
		}

		private void setCellStyle() {
			getStyleClass().removeAll(FILE_NOT_FOUND_ROW,
					CURRENTLY_PLAYED_FILE_ROW);
			if (!isEmpty() && file != null) {
				if (!file.exists()) {
					getStyleClass().add(FILE_NOT_FOUND_ROW);
				} else if (file.equals(currentlyPlayedFileProperty.get())) {
					getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
				}
			}
		}

	}

	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		return new TableCellImpl();
	}

}
