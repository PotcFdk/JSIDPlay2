package ui.musiccollection;

import java.io.File;
import java.util.List;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class MusicCollectionCellFactory implements
		Callback<TreeView<File>, TreeCell<File>> {

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	public class TextFieldTreeCellImpl extends TreeCell<File> {
		@Override
		protected void updateItem(File file, boolean empty) {
			super.updateItem(file, empty);

			if (empty) {
				setText(null);
				setGraphic(null);
			} else {
				getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
				MusicCollectionTreeItem item = (MusicCollectionTreeItem) getTreeItem();
				MusicCollection musicCollection = item.getMusicCollection();
				if (musicCollection.getCurrentlyPlayedTreeItems() != null
						&& isCurrentlyPlayed(file,
								musicCollection.getCurrentlyPlayedTreeItems())) {
					getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
				}
				setText(file.getName());
				setGraphic(getTreeItem().getGraphic());
			}
		}

		private boolean isCurrentlyPlayed(File file,
				List<TreeItem<File>> currentlyPlayedFiles) {
			for (TreeItem<File> treeItem : currentlyPlayedFiles) {
				if (file.equals(treeItem.getValue())) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	public TreeCell<File> call(TreeView<File> arg0) {
		return new TextFieldTreeCellImpl();
	}
}
