package ui.assembly64;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;

public class ZeroIgnoringCellFactory
		implements Callback<TableColumn<SearchResult, String>, TableCell<SearchResult, String>> {

	@Override
	public TableCell<SearchResult, String> call(TableColumn<SearchResult, String> arg0) {
		return new ZeroContainingFormatCell();
	}

}
