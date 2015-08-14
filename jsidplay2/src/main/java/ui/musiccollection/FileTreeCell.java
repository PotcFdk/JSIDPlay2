package ui.musiccollection;

import java.io.File;

import javafx.scene.control.TreeCell;

public final class FileTreeCell extends TreeCell<File> {
	@Override
	protected void updateItem(File item, boolean empty) {
		super.updateItem(item, empty);
		if (!empty) {
			setText(item.getName());
			setGraphic(getTreeItem().getGraphic());
		} else {
			setText("");
			setGraphic(null);
		}
	}
}