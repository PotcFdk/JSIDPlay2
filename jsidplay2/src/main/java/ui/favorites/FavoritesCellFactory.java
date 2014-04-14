package ui.favorites;

import java.io.File;

import javafx.beans.property.ObjectProperty;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Callback;
import libsidutils.PathUtils;
import libsidutils.STIL;
import sidplay.ConsolePlayer;
import ui.JSIDPlay2Main;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;

public class FavoritesCellFactory implements
		Callback<TableColumn<HVSCEntry, ?>, TableCell<HVSCEntry, ?>> {

	private static final Image STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil.png").toString());

	private static final Image NO_STIL_ICON = new Image(JSIDPlay2Main.class
			.getResource("icons/stil_no.png").toString());

	private static final String FILE_NOT_FOUND_ROW = "fileNotFoundRow";

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private Configuration config;
	private ConsolePlayer consolePlayer;
	private ObjectProperty<File> currentlyPlayedFileProperty;

	public void setConfig(Configuration config) {
		this.config = config;
	}

	public void setConsolePlayer(ConsolePlayer consolePlayer) {
		this.consolePlayer = consolePlayer;
	}

	public void setCurrentlyPlayedFileProperty(
			ObjectProperty<File> currentlyPlayedFileProperty) {
		this.currentlyPlayedFileProperty = currentlyPlayedFileProperty;
	}

	@SuppressWarnings("rawtypes")
	protected final class TableCellImpl extends TableCell {

		public TableCellImpl() {
			super();
			currentlyPlayedFileProperty.addListener((observable, oldValue,
					newValue) -> {
				setCellStyle();
			});
		}

		@SuppressWarnings("unchecked")
		@Override
		public void updateItem(Object value, boolean empty) {
			super.updateItem(value, empty);
			setCellStyle();

			if (!empty) {
				String path = value != null ? value.toString() : "";
				setText(path);
				int columnIndex = getTableView().getColumns().indexOf(
						getTableColumn());
				if (columnIndex == 0) {
					STIL stil = consolePlayer.getStil();
					SidPlay2Section sidPlay2Section = (SidPlay2Section) config
							.getSidplay2();
					File file = PathUtils.getFile(path,
							sidPlay2Section.getHvscFile(),
							sidPlay2Section.getCgscFile());
					if (stil != null && file != null
							&& stil.getSTILEntry(file) != null) {
						setGraphic(new ImageView(STIL_ICON));
					} else {
						setGraphic(new ImageView(NO_STIL_ICON));
					}
				} else {
					setGraphic(null);
				}
			} else {
				setText(null);
				setGraphic(new ImageView(NO_STIL_ICON));
			}
		}

		private void setCellStyle() {
			getStyleClass().removeAll(CURRENTLY_PLAYED_FILE_ROW,
					FILE_NOT_FOUND_ROW);
			if (getTableRow() == null) {
				return;
			}
			HVSCEntry hvscEntry = (HVSCEntry) getTableRow().getItem();
			if (hvscEntry != null) {
				SidPlay2Section sidPlay2Section = (SidPlay2Section) config
						.getSidplay2();
				File file = PathUtils.getFile(hvscEntry.getPath(),
						sidPlay2Section.getHvscFile(),
						sidPlay2Section.getCgscFile());
				if (file == null || !file.exists()) {
					getStyleClass().add(FILE_NOT_FOUND_ROW);
				} else if (file.equals(currentlyPlayedFileProperty.get())) {
					getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
				}
			}
		}

	}

	@SuppressWarnings({ "unchecked" })
	@Override
	public TableCell<HVSCEntry, ?> call(final TableColumn<HVSCEntry, ?> column) {
		return new TableCellImpl();
	}

}
