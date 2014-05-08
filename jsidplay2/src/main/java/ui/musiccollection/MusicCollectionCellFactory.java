package ui.musiccollection;

import java.io.File;

import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class MusicCollectionCellFactory implements
		Callback<TreeView<File>, TreeCell<File>> {

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private ObservableList<TreeItem<File>> currentlyPlayedTreeItems;

	public void setCurrentlyPlayedTreeItems(
			ObservableList<TreeItem<File>> currentlyPlayedTreeItems) {
		this.currentlyPlayedTreeItems = currentlyPlayedTreeItems;
	}

	public class TextFieldTreeCellImpl extends TreeCell<File> {

		private ListChangeListener<TreeItem<File>> listChangeListener = (
				Change<? extends TreeItem<File>> c) -> setCellStyle();

		public TextFieldTreeCellImpl() {
			currentlyPlayedTreeItems.addListener(listChangeListener);
		}

		@Override
		protected void updateItem(File file, boolean empty) {
			super.updateItem(file, empty);
			if (!empty && file != null) {
				setText(file.getName());
				setGraphic(getTreeItem().getGraphic());
			} else {
				setText(null);
				setGraphic(null);
			}
			setCellStyle();
		}

		private void setCellStyle() {
			getStyleClass().remove(CURRENTLY_PLAYED_FILE_ROW);
			if (!isEmpty() && isCurrentlyPlayed()) {
				getStyleClass().add(CURRENTLY_PLAYED_FILE_ROW);
			}
		}

		private boolean isCurrentlyPlayed() {
			for (TreeItem<File> treeItem : currentlyPlayedTreeItems) {
				if (treeItem.getValue().equals(getItem())) {
					return true;
				}
			}
			return false;
		}

	}

	@Override
	public TreeCell<File> call(TreeView<File> treeView) {
		return new TextFieldTreeCellImpl();
	}

}
