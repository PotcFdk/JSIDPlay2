package ui.siddump;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

/**
 * Classify SidDump table cells for CSS styling.
 * 
 * @author Ken
 * 
 */
public class SidDumpCellFactory
		implements
		Callback<TableColumn<SidDumpOutput, String>, TableCell<SidDumpOutput, String>> {

	@Override
	public TableCell<SidDumpOutput, String> call(
			final TableColumn<SidDumpOutput, String> column) {
		final TableCell<SidDumpOutput, String> cell = new TableCell<SidDumpOutput, String>() {
			@Override
			public void updateItem(String value, boolean empty) {
				super.updateItem(value, empty);
				int columnIndex = column.getTableView().getColumns()
						.indexOf(column);
				if (!empty) {
					setText(value);
				}

				if (columnIndex == 0 || columnIndex > 15) {
					getStyleClass().add("normalCellValue");
				} else if (value != null && value.trim().startsWith(".")) {
					getStyleClass().add("unchangedCellValue");
				} else {
					switch ((columnIndex - 1) / 5) {
					case 0:
						getStyleClass().add("voice1CellValue");
						break;
					case 1:
						getStyleClass().add("voice2CellValue");
						break;
					case 2:
						getStyleClass().add("voice3CellValue");
						break;

					default:
						break;
					}
				}
			}
		};
		return cell;
	}
}
