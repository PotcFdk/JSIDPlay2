package ui.diskcollection;

import java.io.File;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class DiskCollectionCellFactory implements
		Callback<TreeView<File>, TreeCell<File>> {

	@Override
	public TreeCell<File> call(TreeView<File> param) {
		return new TreeCell<File>() {
			@Override
			protected void updateItem(File item, boolean empty) {
				super.updateItem(item, empty);
				if (!empty) {
					setText(item.getName());
					setGraphic(getTreeItem().getGraphic());
				}
			}
		};
	}
}
