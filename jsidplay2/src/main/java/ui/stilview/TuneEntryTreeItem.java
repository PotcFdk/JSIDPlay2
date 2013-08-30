package ui.stilview;

import java.util.ArrayList;
import java.util.Collection;

import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import libsidutils.STIL.Info;
import libsidutils.STIL.TuneEntry;

public class TuneEntryTreeItem extends TreeItem<Object> {

	private boolean hasLoadedChildren;

	public TuneEntryTreeItem(TuneEntry tuneEntry) {
		super(tuneEntry);
	}

	@Override
	public boolean isLeaf() {
		TuneEntry tuneEntry = (TuneEntry) getValue();
		return tuneEntry.infos.size() == 0;
	}

	@Override
	public ObservableList<TreeItem<Object>> getChildren() {
		if (hasLoadedChildren == false) {
			hasLoadedChildren = true;
			Collection<InfoTreeItem> children = new ArrayList<InfoTreeItem>();
			TuneEntry tuneEntry = (TuneEntry) getValue();
			for (Info info : tuneEntry.infos) {
				children.add(new InfoTreeItem(info));
			}
			super.getChildren().setAll(children);
		}
		return super.getChildren();
	}

}
