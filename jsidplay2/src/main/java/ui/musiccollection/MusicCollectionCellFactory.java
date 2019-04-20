package ui.musiccollection;

import java.io.File;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.util.Callback;

public class MusicCollectionCellFactory implements Callback<TreeView<File>, TreeCell<File>> {

	private static final String CURRENTLY_PLAYED_FILE_ROW = "currentlyPlayedRow";

	private ObjectProperty<List<TreeItem<File>>> currentlyPlayedTreeItemsProperty;

	public void setCurrentlyPlayedTreeItems(ObjectProperty<List<TreeItem<File>>> currentlyPlayedTreeItems) {
		this.currentlyPlayedTreeItemsProperty = currentlyPlayedTreeItems;
	}

	public class TextFieldTreeCellImpl extends TreeCell<File> {

		private ChangeListener<List<TreeItem<File>>> listChangeListener = (observable, oldValue,
				newValue) -> setCellStyle();

		public TextFieldTreeCellImpl() {
			currentlyPlayedTreeItemsProperty.addListener(listChangeListener);
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
			return currentlyPlayedTreeItemsProperty.get() != null ? currentlyPlayedTreeItemsProperty.get().stream()
					.filter(treeItem -> treeItem.getValue().equals(getItem())).findFirst().isPresent() : false;
		}

	}

	@Override
	public TreeCell<File> call(TreeView<File> treeView) {
		return new TextFieldTreeCellImpl();
	}

}
