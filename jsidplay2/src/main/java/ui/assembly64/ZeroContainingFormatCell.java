package ui.assembly64;

import javafx.scene.control.TableCell;

public class ZeroContainingFormatCell extends TableCell<SearchResult, String> {

	@Override
	protected void updateItem(String item, boolean empty) {
		super.updateItem(item, empty);

        if (!empty && "0".equals(item)) {
            item = "";
        }

        setText(item == null ? "" : item);
	}
}
